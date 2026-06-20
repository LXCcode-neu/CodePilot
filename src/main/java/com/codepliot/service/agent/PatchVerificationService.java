package com.codepliot.service.agent;

import com.codepliot.config.PatchVerificationProperties;
import com.codepliot.exception.AgentTaskCancelledException;
import com.codepliot.model.PatchVerificationCommandResult;
import com.codepliot.model.PatchVerificationResult;
import com.codepliot.service.patch.PatchVerificationRecordService;
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
 * 补丁验证服务。
 * <p>
 * 对生成的补丁进行自动化验证，包括以下步骤：
 * <ol>
 *   <li>在临时目录中复制仓库副本</li>
 *   <li>执行 {@code git apply --check} 检查补丁是否可应用</li>
 *   <li>执行 {@code git apply} 应用补丁</li>
 *   <li>根据项目类型自动检测并运行验证命令（如 Maven test、Go test、npm build 等）</li>
 * </ol>
 * 支持配置自定义验证命令，支持任务取消中断验证过程。
 * </p>
 */
@Service
public class PatchVerificationService {

    private static final int OUTPUT_SUMMARY_LIMIT = 4000;
    private static final Set<String> EXCLUDED_DIRECTORIES = Set.of(
            ".git",
            "target",
            "node_modules",
            "dist",
            "build"
    );

    private final ObjectMapper objectMapper;
    private final PatchVerificationProperties properties;
    private final PatchVerificationRecordService patchVerificationRecordService;
    private final AgentTaskCancellationService agentTaskCancellationService;

    public PatchVerificationService(ObjectMapper objectMapper,
                                    PatchVerificationProperties properties,
                                    PatchVerificationRecordService patchVerificationRecordService,
                                    AgentTaskCancellationService agentTaskCancellationService) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.patchVerificationRecordService = patchVerificationRecordService;
        this.agentTaskCancellationService = agentTaskCancellationService;
    }

    /**
     * 验证补丁（不关联补丁记录）。
     *
     * @param repoPath  仓库本地路径
     * @param taskId    任务 ID
     * @param patchText 补丁文本
     * @return 验证结果
     */
    public PatchVerificationResult verify(String repoPath, Long taskId, String patchText) {
        return verify(repoPath, taskId, null, patchText);
    }

    /**
     * 验证补丁并保存验证记录。
     * <p>
     * 完整验证流程：复制仓库到临时目录 -> 检查补丁可应用性 -> 应用补丁 ->
     * 检测并运行验证命令 -> 保存验证结果。验证过程中会检查任务取消状态。
     * </p>
     *
     * @param repoPath      仓库本地路径
     * @param taskId        任务 ID
     * @param patchRecordId 补丁记录 ID（可为 null）
     * @param patchText     补丁文本
     * @return 验证结果
     */
    public PatchVerificationResult verify(String repoPath, Long taskId, Long patchRecordId, String patchText) {
        if (patchText == null || patchText.isBlank()) {
            return saveAndReturn(taskId, patchRecordId, new PatchVerificationResult(
                    true,
                    false,
                    true,
                    "Patch is empty. Automatic verification was skipped.",
                    List.of(),
                    List.of()
            ));
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
                    verificationRoot,
                    taskId
            );
            results.add(checkResult);
            if (!checkResult.passed()) {
                return saveAndReturn(taskId, patchRecordId, buildResult(
                        false,
                        false,
                        verificationRoot,
                        results,
                        "Patch cannot be applied. Verification stopped."
                ));
            }

            PatchVerificationCommandResult applyResult = runCommand(
                    "patch apply",
                    List.of(executable("git"), "apply", patchFile.toAbsolutePath().toString()),
                    verificationRoot,
                    taskId
            );
            results.add(applyResult);
            if (!applyResult.passed()) {
                return saveAndReturn(taskId, patchRecordId, buildResult(
                        true,
                        false,
                        verificationRoot,
                        results,
                        "Patch apply failed. Verification stopped."
                ));
            }

            for (VerificationCommand command : detectVerificationCommands(verificationRoot)) {
                results.add(runCommand(command.name(), command.command(), command.workingDirectory(), taskId));
            }
            boolean passed = results.stream().allMatch(PatchVerificationCommandResult::passed);
            String summary = passed
                    ? "Patch passed automatic verification."
                    : "Patch automatic verification failed. Manual review is required.";
            return saveAndReturn(taskId, patchRecordId, buildResult(true, passed, verificationRoot, results, summary));
        } catch (AgentTaskCancelledException exception) {
            throw exception;
        } catch (RuntimeException | IOException exception) {
            return saveAndReturn(taskId, patchRecordId, new PatchVerificationResult(
                    false,
                    false,
                    false,
                    "Patch automatic verification failed with exception: " + buildErrorMessage(exception),
                    List.of(),
                    List.of()
            ));
        }
    }

    /**
     * 检测仓库中需要运行的验证命令。
     * <p>
     * 根据项目类型自动检测验证命令：
     * <ul>
     *   <li>Maven 项目（pom.xml）-> mvn test</li>
     *   <li>Go 项目（go.mod）-> go test ./...</li>
     *   <li>Python 项目（pyproject.toml/requirements.txt/setup.py）-> python -m compileall</li>
     *   <li>Node.js 项目（package.json 含 build 脚本）-> npm ci/install + npm run build</li>
     * </ul>
     * 同时包含配置文件中定义的自定义验证命令。
     * </p>
     *
     * @param root 仓库根目录
     * @return 验证命令列表
     * @throws IOException 如果读取文件时发生 I/O 错误
     */
    List<VerificationCommand> detectVerificationCommands(Path root) throws IOException {
        List<VerificationCommand> commands = new ArrayList<>();
        commands.addAll(configuredVerificationCommands(root));
        if (properties == null || !properties.isAutoDetectCommandsEnabled()) {
            return commands;
        }
        if (Files.isRegularFile(root.resolve("pom.xml"))) {
            commands.add(new VerificationCommand(
                    "maven test",
                    List.of(mavenExecutable(root), "test"),
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
                boolean hasLockfile = hasNpmLockfile(packageJson.getParent());
                commands.add(new VerificationCommand(
                        hasLockfile ? "npm ci" : "npm install",
                        List.of(executable("npm"), hasLockfile ? "ci" : "install"),
                        packageJson.getParent()
                ));
                commands.add(new VerificationCommand(
                        "npm build",
                        List.of(executable("npm"), "run", "build"),
                        packageJson.getParent()
                ));
            }
        }
        return commands;
    }

    private List<VerificationCommand> configuredVerificationCommands(Path root) {
        if (properties == null || properties.getCommands() == null || properties.getCommands().isEmpty()) {
            return List.of();
        }
        List<VerificationCommand> commands = new ArrayList<>();
        for (PatchVerificationProperties.Command configuredCommand : properties.getCommands()) {
            if (configuredCommand == null || configuredCommand.getCommand() == null || configuredCommand.getCommand().isBlank()) {
                continue;
            }
            Path workingDirectory = resolveCommandWorkingDirectory(root, configuredCommand.getWorkingDirectory());
            String name = configuredCommand.getName() == null || configuredCommand.getName().isBlank()
                    ? configuredCommand.getCommand()
                    : configuredCommand.getName();
            commands.add(new VerificationCommand(
                    name,
                    shellCommand(configuredCommand.getCommand()),
                    workingDirectory
            ));
        }
        return commands;
    }

    private Path resolveCommandWorkingDirectory(Path root, String workingDirectory) {
        String value = workingDirectory == null || workingDirectory.isBlank() ? "." : workingDirectory;
        Path resolved = root.resolve(value).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("verification command workingDirectory must stay inside repository: " + value);
        }
        if (!Files.isDirectory(resolved)) {
            throw new IllegalArgumentException("verification command workingDirectory does not exist: " + value);
        }
        return resolved;
    }

    private List<String> shellCommand(String command) {
        if (isWindows()) {
            return List.of("cmd.exe", "/c", command);
        }
        return List.of("sh", "-c", command);
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

    private PatchVerificationResult saveAndReturn(Long taskId,
                                                  Long patchRecordId,
                                                  PatchVerificationResult result) {
        if (patchVerificationRecordService != null) {
            patchVerificationRecordService.saveVerificationResult(taskId, patchRecordId, result);
        }
        return result;
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

    private PatchVerificationCommandResult runCommand(String name, List<String> command, Path workingDirectory, Long taskId) {
        Path outputFile = null;
        try {
            outputFile = Files.createTempFile("codepilot-verify-", ".log");
            Process process = new ProcessBuilder(command)
                    .directory(workingDirectory.toFile())
                    .redirectErrorStream(true)
                    .redirectOutput(outputFile.toFile())
                    .start();
            long deadline = System.nanoTime() + commandTimeout().toNanos();
            boolean finished = false;
            while (System.nanoTime() < deadline) {
                if (agentTaskCancellationService != null) {
                    try {
                        agentTaskCancellationService.throwIfCancelRequested(taskId);
                    } catch (RuntimeException exception) {
                        process.destroyForcibly();
                        throw exception;
                    }
                }
                if (process.waitFor(1, TimeUnit.SECONDS)) {
                    finished = true;
                    break;
                }
            }
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
            if (agentTaskCancellationService != null) {
                agentTaskCancellationService.throwIfCancelRequested(taskId);
            }
            return failedCommand(name, command, workingDirectory, "Command interrupted");
        } finally {
            if (outputFile != null) {
                try {
                    Files.deleteIfExists(outputFile);
                } catch (IOException ignored) {
                    // Temporary log cleanup failure should not affect verification.
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

    private String mavenExecutable(Path root) {
        Path wrapper = root.resolve(isWindows() ? "mvnw.cmd" : "mvnw");
        if (Files.isRegularFile(wrapper)) {
            return wrapper.toAbsolutePath().toString();
        }
        return executable("mvn");
    }

    private boolean hasNpmLockfile(Path directory) {
        return Files.isRegularFile(directory.resolve("package-lock.json"))
                || Files.isRegularFile(directory.resolve("npm-shrinkwrap.json"));
    }

    private Duration commandTimeout() {
        int seconds = properties == null ? 300 : Math.max(properties.getCommandTimeoutSeconds(), 1);
        return Duration.ofSeconds(seconds);
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
