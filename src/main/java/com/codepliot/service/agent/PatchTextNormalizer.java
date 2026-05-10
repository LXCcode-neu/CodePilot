package com.codepliot.service.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Normalizes unified diff text before it is persisted and verified.
 */
@Component
public class PatchTextNormalizer {

    private static final Pattern HUNK_HEADER = Pattern.compile(
            "^@@ -(\\d+)(?:,(\\d+))? \\+(\\d+)(?:,(\\d+))? @@(.*)$"
    );

    public String normalize(String patchText) {
        if (patchText == null || patchText.isBlank()) {
            return patchText;
        }

        String normalizedNewlines = patchText.replace("\r\n", "\n").replace('\r', '\n');
        boolean endedWithNewline = normalizedNewlines.endsWith("\n");
        String[] lines = normalizedNewlines.split("\n", -1);
        int lineCount = endedWithNewline ? lines.length - 1 : lines.length;
        List<String> output = new ArrayList<>(lineCount);

        int index = 0;
        while (index < lineCount) {
            String line = lines[index];
            Matcher matcher = HUNK_HEADER.matcher(line);
            if (!matcher.matches()) {
                output.add(line);
                index++;
                continue;
            }

            int bodyStart = index + 1;
            int bodyEnd = findHunkBodyEnd(lines, bodyStart, lineCount);
            HunkCounts counts = countHunkLines(lines, bodyStart, bodyEnd);
            output.add(buildHunkHeader(matcher, counts));
            for (int bodyIndex = bodyStart; bodyIndex < bodyEnd; bodyIndex++) {
                output.add(lines[bodyIndex]);
            }
            index = bodyEnd;
        }

        String result = String.join("\n", output);
        return endedWithNewline ? result + "\n" : result;
    }

    private int findHunkBodyEnd(String[] lines, int start, int lineCount) {
        int index = start;
        while (index < lineCount) {
            String line = lines[index];
            if (HUNK_HEADER.matcher(line).matches()
                    || line.startsWith("diff --git ")
                    || isNextFileHeader(lines, index, lineCount)) {
                break;
            }
            index++;
        }
        return index;
    }

    private boolean isNextFileHeader(String[] lines, int index, int lineCount) {
        return lines[index].startsWith("--- ")
                && index + 1 < lineCount
                && lines[index + 1].startsWith("+++ ");
    }

    private HunkCounts countHunkLines(String[] lines, int start, int end) {
        int oldCount = 0;
        int newCount = 0;
        for (int index = start; index < end; index++) {
            String line = lines[index];
            if (line.startsWith("\\")) {
                continue;
            }
            if (line.startsWith("-")) {
                oldCount++;
            } else if (line.startsWith("+")) {
                newCount++;
            } else {
                oldCount++;
                newCount++;
            }
        }
        return new HunkCounts(oldCount, newCount);
    }

    private String buildHunkHeader(Matcher matcher, HunkCounts counts) {
        return "@@ -"
                + matcher.group(1)
                + formatCount(counts.oldCount())
                + " +"
                + matcher.group(3)
                + formatCount(counts.newCount())
                + " @@"
                + matcher.group(5);
    }

    private String formatCount(int count) {
        return count == 1 ? "" : "," + count;
    }

    private record HunkCounts(int oldCount, int newCount) {
    }
}
