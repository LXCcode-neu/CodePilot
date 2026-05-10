package com.codepliot.search.grep;

import com.codepliot.search.config.CodeSearchProperties;
import com.codepliot.search.dto.GrepMatch;
import com.codepliot.search.dto.SearchRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 使用 ripgrep 检索仓库内容，并在配置允许时回退到 Java 扫描。
 */
@Service
public class GrepSearchService {

    private static final Logger log = LoggerFactory.getLogger(GrepSearchService.class);

    private final CodeSearchProperties properties;
    private final RipgrepCommandBuilder commandBuilder;
    private final RipgrepResultParser resultParser;

    public GrepSearchService(CodeSearchProperties properties,
                             RipgrepCommandBuilder commandBuilder,
                             RipgrepResultParser resultParser) {
        this.properties = properties;
        this.commandBuilder = commandBuilder;
        this.resultParser = resultParser;
    }

    public List<GrepMatch> search(SearchRequest request) {
        return searchWithStatus(request).matches();
    }

    public GrepSearchResponse searchWithStatus(SearchRequest request) {
        try {
            SearchInput input = normalizeRequest(request);
            if (properties.isUseRipgrep()) {
                GrepSearchResponse rgResponse = searchWithRipgrep(input);
                if (rgResponse.success() || !properties.isFallbackEnabled()) {
                    return rgResponse;
                }
                GrepSearchResponse fallbackResponse = searchWithJavaFallback(input);
                return fallbackResponse.withMessage(rgResponse.message() + "; " + fallbackResponse.message());
            }
            return searchWithJavaFallback(input);
        } catch (IllegalArgumentException exception) {
            log.warn("Invalid grep search request: {}", exception.getMessage());
            return GrepSearchResponse.failure(exception.getMessage());
        } catch (Exception exception) {
            log.warn("Grep search failed unexpectedly", exception);
            return GrepSearchResponse.failure("Grep search failed: " + buildErrorMessage(exception));
        }
    }

    private GrepSearchResponse searchWithRipgrep(SearchInput input) {
        List<String> command = commandBuilder.build(new RipgrepCommandBuilder.RipgrepCommand(
                properties.getRgPath(),
                input.query(),
                input.regexEnabled(),
                input.globPatterns(),
                properties.getDefaultExcludePatterns(),
                input.maxResults()
        ));

        Process process = null;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(input.repoRoot().toFile());
            processBuilder.redirectErrorStream(false);
            process = processBuilder.start();

            boolean completed = process.waitFor(input.timeout().toMillis(), TimeUnit.MILLISECONDS);
            if (!completed) {
                process.destroyForcibly();
                return GrepSearchResponse.failure("ripgrep timed out after " + input.timeout().toSeconds() + " seconds");
            }

            String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            int exitCode = process.exitValue();
            if (exitCode == 0 || exitCode == 1) {
                List<GrepMatch> matches = resultParser.parse(stdout, input.query(), input.maxResults());
                return GrepSearchResponse.success(exitCode == 0 ? "ripgrep search completed" : "ripgrep found no matches", matches);
            }
            return GrepSearchResponse.failure("ripgrep failed with exit code " + exitCode + buildStderrSuffix(stderr));
        } catch (IOException exception) {
            return GrepSearchResponse.failure("ripgrep is not available or cannot be executed: " + buildErrorMessage(exception));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return GrepSearchResponse.failure("ripgrep search was interrupted");
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private GrepSearchResponse searchWithJavaFallback(SearchInput input) {
        List<GrepMatch> matches = new ArrayList<>();
        Pattern regexPattern;
        try {
            regexPattern = input.regexEnabled() ? Pattern.compile(input.query()) : null;
        } catch (PatternSyntaxException exception) {
            return GrepSearchResponse.failure("Invalid regular expression: " + exception.getMessage());
        }

        try {
            Files.walkFileTree(input.repoRoot(), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (!isInsideRepository(input.repoRoot(), dir) || isExcludedDirectoryName(dir)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (!attrs.isRegularFile() || !isInsideRepository(input.repoRoot(), file) || shouldSkip(file, input)) {
                        return FileVisitResult.CONTINUE;
                    }
                    scanFile(file, input, regexPattern, matches);
                    return matches.size() >= input.maxResults() ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException exception) {
            return GrepSearchResponse.failure("Java fallback grep failed: " + buildErrorMessage(exception));
        }

        return GrepSearchResponse.success("Java fallback grep completed", List.copyOf(matches));
    }

    private void scanFile(Path file,
                          SearchInput input,
                          Pattern regexPattern,
                          List<GrepMatch> matches) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null && matches.size() < input.maxResults()) {
                lineNumber++;
                int column = findColumn(line, input.query(), regexPattern);
                if (column <= 0) {
                    continue;
                }
                GrepMatch match = new GrepMatch();
                match.setFilePath(toRepositoryRelativePath(input.repoRoot(), file));
                match.setLineNumber(lineNumber);
                match.setColumn(column);
                match.setLineText(line);
                match.setQuery(input.query());
                matches.add(match);
            }
        } catch (IOException exception) {
            log.debug("Skipping unreadable file during Java grep fallback: {}", file, exception);
        }
    }

    private int findColumn(String line, String query, Pattern regexPattern) {
        if (regexPattern != null) {
            java.util.regex.Matcher matcher = regexPattern.matcher(line);
            return matcher.find() ? matcher.start() + 1 : -1;
        }
        int index = line.toLowerCase(Locale.ROOT).indexOf(query.toLowerCase(Locale.ROOT));
        return index >= 0 ? index + 1 : -1;
    }

    private boolean shouldSkip(Path file, SearchInput input) {
        String relativePath = toRepositoryRelativePath(input.repoRoot(), file);
        if (!input.globPatterns().isEmpty() && input.globMatchers().stream().noneMatch(matcher -> matcher.matches(Path.of(relativePath)))) {
            return true;
        }
        return input.excludeMatchers().stream().anyMatch(matcher -> matcher.matches(Path.of(relativePath)));
    }

    private SearchInput normalizeRequest(SearchRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("SearchRequest must not be null");
        }
        Path repoRoot = resolveRepositoryRoot(request.getRepoPath());
        String query = request.getQuery();
        if (query == null || query.isBlank()) {
            query = request.getIssueText();
        }
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("query or issueText must not be blank");
        }
        int maxResults = request.getMaxResults() == null || request.getMaxResults() <= 0
                ? properties.getMaxResults()
                : request.getMaxResults();
        Duration timeout = Duration.ofSeconds(Math.max(1, properties.getCommandTimeoutSeconds()));
        List<String> globPatterns = request.getGlobPatterns() == null ? List.of() : request.getGlobPatterns();
        return new SearchInput(
                repoRoot,
                query,
                request.isRegexEnabled(),
                maxResults,
                timeout,
                globPatterns,
                globPatterns.stream().map(this::toMatcher).toList(),
                properties.getDefaultExcludePatterns().stream().map(this::toMatcher).toList()
        );
    }

    private Path resolveRepositoryRoot(String repoPath) {
        if (repoPath == null || repoPath.isBlank()) {
            throw new IllegalArgumentException("repoPath must not be blank");
        }
        Path repoRoot = Path.of(repoPath).toAbsolutePath().normalize();
        if (!Files.isDirectory(repoRoot)) {
            throw new IllegalArgumentException("repoPath must be an existing directory: " + repoRoot);
        }
        return repoRoot;
    }

    private PathMatcher toMatcher(String glob) {
        return java.nio.file.FileSystems.getDefault().getPathMatcher("glob:" + normalizeGlob(glob));
    }

    private String normalizeGlob(String glob) {
        String normalized = glob == null ? "" : glob.replace('\\', '/');
        return normalized.startsWith("/") ? normalized.substring(1) : normalized;
    }

    private boolean isExcludedDirectoryName(Path dir) {
        Path fileName = dir.getFileName();
        if (fileName == null) {
            return false;
        }
        String normalized = fileName.toString().toLowerCase(Locale.ROOT);
        return Set.of(".git", "target", "build", "dist", "node_modules", ".idea", ".vscode", "logs", "tmp")
                .contains(normalized);
    }

    private boolean isInsideRepository(Path repoRoot, Path path) {
        return path.toAbsolutePath().normalize().startsWith(repoRoot);
    }

    private String toRepositoryRelativePath(Path repoRoot, Path path) {
        Path normalizedPath = path.toAbsolutePath().normalize();
        if (!isInsideRepository(repoRoot, normalizedPath)) {
            throw new IllegalArgumentException("Path is outside repository: " + normalizedPath);
        }
        return repoRoot.relativize(normalizedPath).toString().replace('\\', '/');
    }

    private String buildStderrSuffix(String stderr) {
        return stderr == null || stderr.isBlank() ? "" : ": " + stderr;
    }

    private String buildErrorMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private record SearchInput(
            Path repoRoot,
            String query,
            boolean regexEnabled,
            int maxResults,
            Duration timeout,
            List<String> globPatterns,
            List<PathMatcher> globMatchers,
            List<PathMatcher> excludeMatchers
    ) {
    }

    public record GrepSearchResponse(boolean success, String message, List<GrepMatch> matches) {

        public static GrepSearchResponse success(String message, List<GrepMatch> matches) {
            return new GrepSearchResponse(true, message, matches == null ? List.of() : List.copyOf(matches));
        }

        public static GrepSearchResponse failure(String message) {
            return new GrepSearchResponse(false, message, List.of());
        }

        public GrepSearchResponse withMessage(String newMessage) {
            return new GrepSearchResponse(success, newMessage, matches);
        }
    }
}
