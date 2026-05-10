package com.codepliot.model;

import com.codepliot.entity.PatchRecord;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Patch 记录视图对象。
 */
public record PatchRecordVO(
        Long id,
        Long taskId,
        String analysis,
        String solution,
        String patch,
        String risk,
        String safetyCheckResult,
        String rawOutput,
        Boolean confirmed,
        LocalDateTime confirmedAt,
        Boolean prSubmitted,
        LocalDateTime prSubmittedAt,
        String prUrl,
        Integer prNumber,
        String prBranch,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<PatchFileChange> fileChanges,
        PullRequestPreview pullRequest
) {

    private static final Pattern HUNK_HEADER_PATTERN = Pattern.compile(
            "^@@\\s+-(\\d+)(?:,(\\d+))?\\s+\\+(\\d+)(?:,(\\d+))?\\s+@@.*$"
    );

    public static PatchRecordVO from(PatchRecord patchRecord) {
        List<PatchFileChange> fileChanges = parseFileChanges(patchRecord.getPatch());
        PullRequestPreview pullRequest = buildPullRequestPreview(patchRecord, fileChanges);
        return new PatchRecordVO(
                patchRecord.getId(),
                patchRecord.getTaskId(),
                patchRecord.getAnalysis(),
                patchRecord.getSolution(),
                patchRecord.getPatch(),
                patchRecord.getRisk(),
                patchRecord.getSafetyCheckResult(),
                patchRecord.getRawOutput(),
                patchRecord.getConfirmed(),
                patchRecord.getConfirmedAt(),
                patchRecord.getPrSubmitted(),
                patchRecord.getPrSubmittedAt(),
                patchRecord.getPrUrl(),
                patchRecord.getPrNumber(),
                patchRecord.getPrBranch(),
                patchRecord.getCreatedAt(),
                patchRecord.getUpdatedAt(),
                fileChanges,
                pullRequest
        );
    }

    private static List<PatchFileChange> parseFileChanges(String patch) {
        if (patch == null || patch.isBlank()) {
            return List.of();
        }

        List<PatchFileChangeBuilder> files = new ArrayList<>();
        PatchFileChangeBuilder currentFile = null;
        PatchDiffHunkBuilder currentHunk = null;
        int oldLine = 0;
        int newLine = 0;

        for (String line : patch.split("\\R", -1)) {
            if (line.startsWith("--- ")) {
                currentFile = new PatchFileChangeBuilder();
                currentFile.oldPath = normalizeDiffPath(line.substring(4).trim());
                files.add(currentFile);
                currentHunk = null;
                continue;
            }
            if (line.startsWith("+++ ")) {
                if (currentFile == null) {
                    currentFile = new PatchFileChangeBuilder();
                    files.add(currentFile);
                }
                currentFile.newPath = normalizeDiffPath(line.substring(4).trim());
                continue;
            }
            Matcher hunkMatcher = HUNK_HEADER_PATTERN.matcher(line);
            if (hunkMatcher.matches()) {
                if (currentFile == null) {
                    currentFile = new PatchFileChangeBuilder();
                    files.add(currentFile);
                }
                oldLine = parseInt(hunkMatcher.group(1), 0);
                newLine = parseInt(hunkMatcher.group(3), 0);
                currentHunk = new PatchDiffHunkBuilder(
                        line,
                        oldLine,
                        parseInt(hunkMatcher.group(2), 1),
                        newLine,
                        parseInt(hunkMatcher.group(4), 1)
                );
                currentFile.hunks.add(currentHunk);
                continue;
            }
            if (currentFile == null || currentHunk == null || line.startsWith("\\ No newline")) {
                continue;
            }

            if (line.startsWith("+")) {
                currentFile.addedLines++;
                currentHunk.lines.add(new PatchDiffLine("added", null, newLine, line.substring(1)));
                newLine++;
            } else if (line.startsWith("-")) {
                currentFile.removedLines++;
                currentHunk.lines.add(new PatchDiffLine("removed", oldLine, null, line.substring(1)));
                oldLine++;
            } else {
                String content = line.startsWith(" ") ? line.substring(1) : line;
                currentHunk.lines.add(new PatchDiffLine("context", oldLine, newLine, content));
                oldLine++;
                newLine++;
            }
        }

        return files.stream()
                .map(PatchFileChangeBuilder::build)
                .filter(file -> file.filePath() != null && !file.filePath().isBlank())
                .toList();
    }

    private static PullRequestPreview buildPullRequestPreview(PatchRecord patchRecord,
                                                              List<PatchFileChange> fileChanges) {
        int addedLines = fileChanges.stream().mapToInt(file -> valueOrZero(file.addedLines())).sum();
        int removedLines = fileChanges.stream().mapToInt(file -> valueOrZero(file.removedLines())).sum();
        List<String> touchedFiles = fileChanges.stream()
                .map(PatchFileChange::filePath)
                .filter(path -> path != null && !path.isBlank())
                .toList();
        boolean ready = patchRecord.getPatch() != null && !patchRecord.getPatch().isBlank() && !fileChanges.isEmpty();
        String taskId = patchRecord.getTaskId() == null ? "unknown" : String.valueOf(patchRecord.getTaskId());
        String title = firstSentence(patchRecord.getSolution(), "CodePilot patch for task " + taskId);
        String branchName = "codepilot/task-" + taskId;
        String commitMessage = firstSentence(patchRecord.getSolution(), "Apply CodePilot patch for task " + taskId);
        String status = ready
                ? "Patch is ready to submit as a GitHub pull request."
                : "Patch is empty or not a valid unified diff; PR preview is not ready.";

        StringBuilder body = new StringBuilder();
        body.append("## Summary\n")
                .append(nullToFallback(patchRecord.getSolution(), "No solution summary provided."))
                .append("\n\n## Analysis\n")
                .append(nullToFallback(patchRecord.getAnalysis(), "No analysis provided."))
                .append("\n\n## Risk\n")
                .append(nullToFallback(patchRecord.getRisk(), "No risk notes provided."))
                .append("\n\n## Changes\n")
                .append("- Files changed: ").append(fileChanges.size()).append('\n')
                .append("- Added lines: ").append(addedLines).append('\n')
                .append("- Removed lines: ").append(removedLines).append('\n');
        for (String file : touchedFiles) {
            body.append("- `").append(file).append("`\n");
        }

        return new PullRequestPreview(
                title,
                branchName,
                commitMessage,
                body.toString().trim(),
                fileChanges.size(),
                addedLines,
                removedLines,
                touchedFiles,
                ready,
                status
        );
    }

    private static String normalizeDiffPath(String value) {
        if (value == null || value.isBlank() || "/dev/null".equals(value)) {
            return "";
        }
        String normalized = value.replace('\\', '/');
        if (normalized.startsWith("a/") || normalized.startsWith("b/")) {
            return normalized.substring(2);
        }
        return normalized;
    }

    private static int parseInt(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private static String firstSentence(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        int end = normalized.indexOf('。');
        if (end < 0) {
            end = normalized.indexOf('.');
        }
        String sentence = end > 0 ? normalized.substring(0, end + 1) : normalized;
        return sentence.length() > 90 ? sentence.substring(0, 90).trim() + "..." : sentence;
    }

    private static String nullToFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static final class PatchFileChangeBuilder {
        private String oldPath = "";
        private String newPath = "";
        private int addedLines;
        private int removedLines;
        private final List<PatchDiffHunkBuilder> hunks = new ArrayList<>();

        private PatchFileChange build() {
            String filePath = newPath == null || newPath.isBlank() ? oldPath : newPath;
            return new PatchFileChange(
                    oldPath,
                    newPath,
                    filePath,
                    addedLines,
                    removedLines,
                    hunks.stream().map(PatchDiffHunkBuilder::build).toList()
            );
        }
    }

    private static final class PatchDiffHunkBuilder {
        private final String header;
        private final Integer oldStart;
        private final Integer oldLineCount;
        private final Integer newStart;
        private final Integer newLineCount;
        private final List<PatchDiffLine> lines = new ArrayList<>();

        private PatchDiffHunkBuilder(String header,
                                     Integer oldStart,
                                     Integer oldLineCount,
                                     Integer newStart,
                                     Integer newLineCount) {
            this.header = header;
            this.oldStart = oldStart;
            this.oldLineCount = oldLineCount;
            this.newStart = newStart;
            this.newLineCount = newLineCount;
        }

        private PatchDiffHunk build() {
            return new PatchDiffHunk(header, oldStart, oldLineCount, newStart, newLineCount, List.copyOf(lines));
        }
    }
}
