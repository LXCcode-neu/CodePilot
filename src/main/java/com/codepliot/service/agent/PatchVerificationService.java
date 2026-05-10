package com.codepliot.service.agent;

import com.codepliot.model.PatchVerificationCommandResult;
import com.codepliot.model.PatchVerificationResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

/**
 * Patch 自动验证服务。
 */
@Service
public class PatchVerificationService {

    private static final Duration COMMAND_TIMEOUT = Duration.ofMinutes(2);
    private static final int OUTPUT_SUMMARY_LIMIT = 4000;
    private static final Set<String> EXCLUDED_DIRECTORIES = Set.of(
            ".git",
            "target",
            "node_modules",
            "dist",
            "build"
    );

    private final ObjectMapper objectMapper;

    public PatchVerificationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public PatchVerificationResult verify(String repoPath, Long taskId, String patchText) {
        if (patchText == null || patchText.isBlank()) {
            return new PatchVerificationResult(
                    true,
                    false,
                    true,
                    "Patch 为空，跳过自动验证。",
                    List.of(),
                    List.of()
            );
        }

        Path sourceRoot = resolveRepositoryRoot(repoPath);
        Path verificationRoot = resolveVerificationRoot(sourceRoot, taskId);
        try {
            recreateDirectory(verificationRoot);
            copyRepository(sourceRoot, verificationRoot);
            Path patchFile = verificationRoot.resolve("generated.patch");
            Files.writeString(patchFile, patchText, StandardCharsets.UTF_8);

            List<PatchVerificationCommandResult> results = new ArrayList<>();
            PatchVerificationCommandResult checkResult = runCommand(
                    "patch apply check",
                    List.of(executable("git"), "apply", "--check", patchFile.toAbsolutePath().toString()),
                    verificationRoot
            );
            results.add(checkResult);
            if (!checkResult.passed()) {
                return buildResult(false, false, verificationRoot, results, "Patch 无法应用，已停止后续验证。");
            }

            PatchVerificationCommandResult applyResult = runCommand(
                    "patch apply",
                    List.of(executable("git"), "apply", patchFile.toAbsolutePath().toString()),
                    verificationRoot
            );
            results.add(applyResult);
            if (!applyResult.passed()) {
                return buildResult(true, false, verificationRoot, results, "Patch 应用失败，已停止后续验证。");
            }

            for (VerificationCommand command : detectVerificationCommands(verificationRoot)) {
                results.add(runCommand(command.name(), command.command(), command.workingDirectory()));
            }
            boolean passed = results.stream().allMatch(PatchVerificationCommandResult::passed);
            String summary = passed ? "Patch 已通过自动验证。" : "Patch 自动验证存在失败项，请人工重点确认。";
            return buildResult(true, passed, verificationRoot, results, summary);
        } catch (RuntimeException | IOException exception) {
            return new PatchVerificationResult(
                    false,
                    false,
                    false,
                    "Patch 自动验证执行异常：" + buildErrorMessage(exception),
                    List.of(),
                    List.of()
            );
        }
    }

    List<VerificationCommand> detectVerificationCommands(Path root) throws IOException {
        List<VerificationCommand> commands = new ArrayList<>();
        if (Files.isRegularFile(root.resolve("pom.xml"))) {
            commands.add(new VerificationCommand(
                    "maven compile",
                    List.of(executable("mvn"), "-q", "-DskipTests", "compile"),
                    root
            ));
        }
        if (Files.isRegularFile(root.resolve("go.mod"))) {
            commands.add(new VerificationCommand(
                    "go test",
                    List.of(executable("go"), "test", "./..."),
                    root
            ));
        }
        if (Files.isRegularFile(root.resolve("pyproject.toml"))
                || Files.isRegularFile(root.resolve("requirements.txt"))
                || Files.isRegularFile(root.resolve("setup.py"))) {
            commands.add(new VerificationCommand(
                    "python compileall",
                    List.of(executable("python"), "-m", "compileall", "."),
                    root
            ));
        }
        for (Path packageJson : findPackageJsonFiles(root)) {
            if (hasNpmBuildScript(packageJson)) {
                commands.add(new VerificationCommand(
                        "npm build",
                        List.of(executable("npm"), "run", "build"),
                        packageJson.getParent()
                ));
            }
        }
        return commands;
    }

    private PatchVerificationResult buildResult(boolean patchApplicable,
                                                boolean passed,
                                                Path verificationRoot,
                                                List<PatchVerificationCommandResult> results,
                                                String summary) throws IOException {
        return new PatchVerificationResult(
                false,
                patchApplicable,
                passed,
                summary,
                detectRepositoryTypes(verificationRoot),
                results
        );
    }

    private List<String> detectRepositoryTypes(Path root) throws IOException {
        Set<String> types = new LinkedHashSet<>();
        if (Files.isRegularFile(root.resolve("pom.xml"))) {
            types.add("MAVEN");
        }
        if (Files.isRegularFile(root.resolve("go.mod"))) {
            types.add("GO");
        }
        if (Files.isRegularFile(root.resolve("pyproject.toml"))
                || Files.isRegularFile(root.resolve("requirements.txt"))
                || Files.isRegularFile(root.resolve("setup.py"))) {
            types.add("PYTHON");
        }
        if (!findPackageJsonFiles(root).isEmpty()) {
            types.add("NODE");
        }
        if (types.isEmpty()) {
            types.add("PATCH_ONLY");
        }
        return List.copyOf(types);
    }

    private List<Path> findPackageJsonFiles(Path root) throws IOException {
        try (var stream = Files.walk(root, 4)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> "package.json".equals(path.getFileName().toString()))
                    .filter(path -> !isExcludedPath(root, path))
                    .toList();
        }
    }

    private boolean hasNpmBuildScript(Path packageJson) {
        try {
            JsonNode root = objectMapper.readTree(packageJson.toFile());
            JsonNode buildScript = root.path("scripts").path("build");
            return buildScript.isTextual() && !buildScript.asText("").isBlank();
        } catch (IOException exception) {
            return false;
        }
    }

    private PatchVerificationCommandResult runCommand(String name, List<String> command, Path workingDirectory) {
        Path outputFile = null;
        try {
            outputFile = Files.createTempFile("codepilot-verify-", ".log");
            Process process = new ProcessBuilder(command)
                    .directory(workingDirectory.toFile())
                    .redirectErrorStream(true)
                    .redirectOutput(outputFile.toFile())
                    .start();
            boolean finished = process.waitFor(COMMAND_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
            }
            int exitCode = finished ? process.exitValue() : -1;
            String output = Files.readString(outputFile, StandardCharsets.UTF_8);
            return new PatchVerificationCommandResult(
                    name,
                    String.join(" ", command),
                    workingDirectory.toString(),
                    exitCode,
                    finished && exitCode == 0,
                    !finished,
                    summarize(output)
            );
        } catch (IOException exception) {
            return failedCommand(name, command, workingDirectory, buildErrorMessage(exception));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return failedCommand(name, command, workingDirectory, "Command interrupted");
        } finally {
            if (outputFile != null) {
                try {
                    Files.deleteIfExists(outputFile);
                } catch (IOException ignored) {
                    // 临时日志删除失败不影响验证结果。
                }
            }
        }
    }

    private PatchVerificationCommandResult failedCommand(String name, List<String> command, Path workingDirectory, String message) {
        return new PatchVerificationCommandResult(
                name,
                String.join(" ", command),
                workingDirectory.toString(),
                -1,
                false,
                false,
                message
        );
    }

    private Path resolveRepositoryRoot(String repoPath) {
        if (repoPath == null || repoPath.isBlank()) {
            throw new IllegalArgumentException("repoPath must not be blank");
        }
        Path root = Path.of(repoPath).toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException("repoPath must be an existing directory: " + root);
        }
        return root;
    }

    private Path resolveVerificationRoot(Path sourceRoot, Long taskId) {
        Path parent = sourceRoot.getParent();
        if (parent == null) {
            throw new IllegalArgumentException("repoPath must have a parent directory");
        }
        return parent.resolve("verify").resolve(String.valueOf(taskId == null ? "unknown" : taskId)).normalize();
    }

    private void recreateDirectory(Path directory) throws IOException {
        deleteRecursively(directory);
        Files.createDirectories(directory);
    }

    private void copyRepository(Path sourceRoot, Path targetRoot) throws IOException {
        Files.walkFileTree(sourceRoot, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (isExcludedPath(sourceRoot, dir)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                Files.createDirectories(targetRoot.resolve(sourceRoot.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!attrs.isRegularFile() || isExcludedPath(sourceRoot, file)) {
                    return FileVisitResult.CONTINUE;
                }
                Path target = targetRoot.resolve(sourceRoot.relativize(file));
                Files.createDirectories(target.getParent());
                Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exception) throws IOException {
                if (exception != null) {
                    throw exception;
                }
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private boolean isExcludedPath(Path root, Path path) {
        if (root.equals(path)) {
            return false;
        }
        Path relative = root.relativize(path);
        for (Path part : relative) {
            String name = part.toString();
            if (EXCLUDED_DIRECTORIES.contains(name)) {
                return true;
            }
        }
        return false;
    }

    private String executable(String name) {
        if (!isWindows()) {
            return name;
        }
        return switch (name) {
            case "mvn" -> "mvn.cmd";
            case "npm" -> "npm.cmd";
            default -> name;
        };
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private String summarize(String output) {
        if (output == null || output.isBlank()) {
            return "";
        }
        String normalized = output.trim();
        if (normalized.length() <= OUTPUT_SUMMARY_LIMIT) {
            return normalized;
        }
        return normalized.substring(normalized.length() - OUTPUT_SUMMARY_LIMIT);
    }

    private String buildErrorMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    record VerificationCommand(String name, List<String> command, Path workingDirectory) {
    }
}
