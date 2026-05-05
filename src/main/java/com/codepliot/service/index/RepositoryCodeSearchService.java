package com.codepliot.service.index;

import com.codepliot.model.RetrievedCodeChunk;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * 仓库文本搜索兜底服务，用于在 Lucene 命中不足时直接从源码文件补充上下文。
 */
@Service
public class RepositoryCodeSearchService {

    private static final int MAX_FILE_SIZE = 1024 * 1024;
    private static final int CONTEXT_LINE_RADIUS = 8;

    private static final Set<String> SKIPPED_DIRECTORIES = Set.of(
            ".git", "target", "node_modules", "dist", "build", ".idea", ".vscode", "out", "coverage",
            ".m2repo", ".gradle", ".yarn", ".pnpm-store", ".cache", ".next", ".nuxt", "__pycache__",
            ".pytest_cache", "storybook-static", "tmp", "temp", "logs"
    );

    private static final Set<String> SKIPPED_SUFFIXES = Set.of(
            ".jar", ".war", ".ear", ".zip", ".gz", ".tgz", ".7z", ".class", ".dll", ".exe", ".so", ".dylib",
            ".sha1", ".md5", ".png", ".jpg", ".jpeg", ".gif", ".ico", ".pdf", ".woff", ".woff2", ".ttf", ".eot",
            ".map", ".min.js", ".min.css"
    );

    private final FileLanguageMapper fileLanguageMapper;

    public RepositoryCodeSearchService(FileLanguageMapper fileLanguageMapper) {
        this.fileLanguageMapper = fileLanguageMapper;
    }

    public List<RetrievedCodeChunk> search(String localPath, String query, int topK) {
        Path repoRoot = resolveRepositoryRoot(localPath);
        if (repoRoot == null || topK <= 0) {
            return List.of();
        }

        List<String> keywords = KeywordExtractor.extractKeywords(query);
        if (keywords.isEmpty()) {
            return List.of();
        }

        List<RetrievedCodeChunk> results = new ArrayList<>();
        try {
            Files.walkFileTree(repoRoot, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    Path fileName = dir.getFileName();
                    if (fileName != null && SKIPPED_DIRECTORIES.contains(fileName.toString())) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (!attrs.isRegularFile() || attrs.size() > MAX_FILE_SIZE || shouldSkip(file)) {
                        return FileVisitResult.CONTINUE;
                    }
                    RetrievedCodeChunk chunk = buildChunk(repoRoot, file, keywords);
                    if (chunk != null) {
                        results.add(chunk);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ignored) {
            return List.of();
        }

        return results.stream()
                .sorted(Comparator.comparingDouble(RetrievedCodeChunk::score).reversed()
                        .thenComparing(RetrievedCodeChunk::filePath, Comparator.nullsLast(String::compareTo)))
                .limit(topK)
                .toList();
    }

    public List<RetrievedCodeChunk> hydrate(String localPath, String query, List<RetrievedCodeChunk> chunks) {
        Path repoRoot = resolveRepositoryRoot(localPath);
        if (repoRoot == null || chunks == null || chunks.isEmpty()) {
            return chunks == null ? List.of() : List.copyOf(chunks);
        }

        List<String> keywords = KeywordExtractor.extractKeywords(query);
        List<RetrievedCodeChunk> hydrated = new ArrayList<>(chunks.size());
        for (RetrievedCodeChunk chunk : chunks) {
            hydrated.add(hydrateChunk(repoRoot, chunk, keywords));
        }
        return hydrated;
    }

    private RetrievedCodeChunk buildChunk(Path repoRoot, Path file, List<String> keywords) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            return null;
        }

        String normalizedPath = normalizePath(repoRoot.relativize(file).toString());
        MatchWindow matchWindow = findBestWindow(lines, normalizedPath, keywords);
        if (matchWindow.score() <= 0) {
            return null;
        }

        String excerpt = buildExcerpt(lines, matchWindow.startLine(), matchWindow.endLine());
        String language = fileLanguageMapper.map(file).name();
        return new RetrievedCodeChunk(
                normalizedPath,
                language,
                "FILE",
                file.getFileName() == null ? normalizedPath : file.getFileName().toString(),
                null,
                null,
                matchWindow.startLine(),
                matchWindow.endLine(),
                excerpt,
                matchWindow.score(),
                matchWindow.score()
        );
    }

    private RetrievedCodeChunk hydrateChunk(Path repoRoot, RetrievedCodeChunk chunk, List<String> keywords) {
        if (chunk == null || chunk.filePath() == null || chunk.filePath().isBlank()) {
            return chunk;
        }

        Path file = repoRoot.resolve(chunk.filePath()).normalize();
        if (!Files.isRegularFile(file)) {
            return chunk;
        }

        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            if (lines.isEmpty()) {
                return chunk;
            }

            int startLine = chunk.startLine() == null ? -1 : chunk.startLine();
            int endLine = chunk.endLine() == null ? -1 : chunk.endLine();
            MatchWindow matchWindow = resolveWindow(lines, startLine, endLine, keywords);
            String excerpt = buildExcerpt(lines, matchWindow.startLine(), matchWindow.endLine());
            return new RetrievedCodeChunk(
                    chunk.filePath(),
                    chunk.language(),
                    chunk.symbolType(),
                    chunk.symbolName(),
                    chunk.parentSymbol(),
                    chunk.routePath(),
                    matchWindow.startLine(),
                    matchWindow.endLine(),
                    excerpt,
                    chunk.score(),
                    chunk.finalScore()
            );
        } catch (IOException ignored) {
            return chunk;
        }
    }

    private MatchWindow findBestWindow(List<String> lines, String normalizedPath, List<String> keywords) {
        double pathScore = scoreText(normalizedPath, keywords) * 1.5d;
        int bestCenterLine = -1;
        double bestLineScore = 0.0d;
        double totalLineScore = 0.0d;

        for (int i = 0; i < lines.size(); i++) {
            double lineScore = scoreText(lines.get(i), keywords);
            if (lineScore > 0) {
                totalLineScore += lineScore;
                if (lineScore > bestLineScore) {
                    bestLineScore = lineScore;
                    bestCenterLine = i + 1;
                }
            }
        }

        double finalScore = pathScore + totalLineScore;
        if (finalScore <= 0) {
            return new MatchWindow(1, Math.min(lines.size(), CONTEXT_LINE_RADIUS * 2 + 1), 0.0d);
        }

        int centerLine = bestCenterLine > 0 ? bestCenterLine : 1;
        return buildWindow(lines.size(), centerLine, finalScore);
    }

    private MatchWindow resolveWindow(List<String> lines, int startLine, int endLine, List<String> keywords) {
        if (startLine > 0 && endLine >= startLine) {
            int centerLine = startLine + Math.max(0, endLine - startLine) / 2;
            return buildWindow(lines.size(), centerLine, 0.0d);
        }

        MatchWindow matchWindow = findBestWindow(lines, "", keywords);
        if (matchWindow.score() > 0 || matchWindow.startLine() > 0) {
            return buildWindow(lines.size(), matchWindow.startLine(), 0.0d);
        }
        return buildWindow(lines.size(), 1, 0.0d);
    }

    private MatchWindow buildWindow(int totalLines, int centerLine, double score) {
        int safeCenterLine = Math.max(1, Math.min(totalLines, centerLine));
        int startLine = Math.max(1, safeCenterLine - CONTEXT_LINE_RADIUS);
        int endLine = Math.min(totalLines, safeCenterLine + CONTEXT_LINE_RADIUS);
        return new MatchWindow(startLine, endLine, score);
    }

    private double scoreText(String text, List<String> keywords) {
        if (text == null || text.isBlank() || keywords.isEmpty()) {
            return 0.0d;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        double score = 0.0d;
        Set<String> uniqueHits = new LinkedHashSet<>();
        for (String keyword : keywords) {
            if (keyword.length() == 1 && Character.isDigit(keyword.charAt(0))) {
                if (containsStandaloneDigit(normalized, keyword.charAt(0))) {
                    uniqueHits.add(keyword);
                }
                continue;
            }
            if (normalized.contains(keyword)) {
                uniqueHits.add(keyword);
            }
        }
        for (String ignored : uniqueHits) {
            score += 1.0d;
        }
        return score;
    }

    private boolean containsStandaloneDigit(String text, char digit) {
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) != digit) {
                continue;
            }
            boolean leftDigit = i > 0 && Character.isDigit(text.charAt(i - 1));
            boolean rightDigit = i + 1 < text.length() && Character.isDigit(text.charAt(i + 1));
            if (!leftDigit && !rightDigit) {
                return true;
            }
        }
        return false;
    }

    private String buildExcerpt(List<String> lines, int startLine, int endLine) {
        StringBuilder builder = new StringBuilder();
        for (int line = startLine; line <= endLine && line <= lines.size(); line++) {
            builder.append(lines.get(line - 1));
            if (line < endLine) {
                builder.append('\n');
            }
        }
        return builder.toString();
    }

    private boolean shouldSkip(Path file) {
        String fileName = file.getFileName() == null ? "" : file.getFileName().toString().toLowerCase(Locale.ROOT);
        for (String suffix : SKIPPED_SUFFIXES) {
            if (fileName.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    private Path resolveRepositoryRoot(String localPath) {
        if (localPath == null || localPath.isBlank()) {
            return null;
        }
        Path path = Path.of(localPath).toAbsolutePath().normalize();
        return Files.isDirectory(path) ? path : null;
    }

    private String normalizePath(String path) {
        return path.replace('\\', '/');
    }

    private record MatchWindow(int startLine, int endLine, double score) {
    }
}
