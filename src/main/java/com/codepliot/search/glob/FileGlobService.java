package com.codepliot.search.glob;

import com.codepliot.search.config.CodeSearchProperties;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Finds repository files by glob patterns while keeping all paths inside the repository root.
 */
@Service
public class FileGlobService {

    private final CodeSearchProperties properties;

    public FileGlobService(CodeSearchProperties properties) {
        this.properties = properties;
    }

    public List<String> findFiles(String repoPath, String globPattern) {
        return findFiles(repoPath, globPattern == null || globPattern.isBlank() ? List.of() : List.of(globPattern));
    }

    public List<String> findFiles(String repoPath, List<String> globPatterns) {
        Path repoRoot = resolveRepositoryRoot(repoPath);
        List<PathMatcher> includeMatchers = buildIncludeMatchers(globPatterns);
        List<PathMatcher> excludeMatchers = buildExcludeMatchers();
        Set<String> matches = new LinkedHashSet<>();

        try {
            Files.walkFileTree(repoRoot, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (!isInsideRepository(repoRoot, dir)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    if (!repoRoot.equals(dir) && isExcluded(repoRoot, dir, excludeMatchers)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (!attrs.isRegularFile() || !isInsideRepository(repoRoot, file)) {
                        return FileVisitResult.CONTINUE;
                    }
                    if (isExcluded(repoRoot, file, excludeMatchers) || !isIncluded(repoRoot, file, includeMatchers)) {
                        return FileVisitResult.CONTINUE;
                    }
                    matches.add(toRepositoryRelativePath(repoRoot, file));
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException exception) {
            return List.of();
        }

        return List.copyOf(matches);
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

    private List<PathMatcher> buildIncludeMatchers(List<String> globPatterns) {
        List<String> patterns = globPatterns == null || globPatterns.isEmpty() ? List.of("**") : globPatterns;
        return patterns.stream()
                .filter(pattern -> pattern != null && !pattern.isBlank())
                .map(this::toMatcher)
                .toList();
    }

    private List<PathMatcher> buildExcludeMatchers() {
        return properties.getDefaultExcludePatterns().stream()
                .filter(pattern -> pattern != null && !pattern.isBlank())
                .map(this::toMatcher)
                .toList();
    }

    private PathMatcher toMatcher(String pattern) {
        return FileSystems.getDefault().getPathMatcher("glob:" + normalizeGlob(pattern));
    }

    private boolean isIncluded(Path repoRoot, Path file, List<PathMatcher> includeMatchers) {
        Path relativePath = toMatcherPath(repoRoot, file);
        for (PathMatcher matcher : includeMatchers) {
            if (matcher.matches(relativePath)) {
                return true;
            }
        }
        return false;
    }

    private boolean isExcluded(Path repoRoot, Path path, List<PathMatcher> excludeMatchers) {
        Path relativePath = toMatcherPath(repoRoot, path);
        for (PathMatcher matcher : excludeMatchers) {
            if (matcher.matches(relativePath)) {
                return true;
            }
        }
        return isExcludedDirectoryName(path);
    }

    private boolean isExcludedDirectoryName(Path path) {
        Path fileName = path.getFileName();
        if (fileName == null) {
            return false;
        }
        String normalized = fileName.toString().toLowerCase(Locale.ROOT);
        return Set.of(".git", "target", "build", "dist", "node_modules", ".idea", ".vscode", "logs", "tmp")
                .contains(normalized);
    }

    private Path toMatcherPath(Path repoRoot, Path path) {
        return Path.of(toRepositoryRelativePath(repoRoot, path));
    }

    private String toRepositoryRelativePath(Path repoRoot, Path path) {
        Path normalizedPath = path.toAbsolutePath().normalize();
        if (!isInsideRepository(repoRoot, normalizedPath)) {
            throw new IllegalArgumentException("Path is outside repository: " + normalizedPath);
        }
        return repoRoot.relativize(normalizedPath).toString().replace('\\', '/');
    }

    private boolean isInsideRepository(Path repoRoot, Path path) {
        return path.toAbsolutePath().normalize().startsWith(repoRoot);
    }

    private String normalizeGlob(String pattern) {
        String normalized = pattern.replace('\\', '/');
        if (normalized.startsWith("/")) {
            return normalized.substring(1);
        }
        return normalized;
    }
}
