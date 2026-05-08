package com.codepliot.search.grep;

import com.codepliot.search.dto.GrepMatch;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Parses ripgrep output lines in file:line:column:text format.
 */
@Component
public class RipgrepResultParser {

    public List<GrepMatch> parse(String output, String query, int maxResults) {
        if (output == null || output.isBlank()) {
            return List.of();
        }

        List<GrepMatch> matches = new ArrayList<>();
        for (String line : output.split("\\R")) {
            if (line.isBlank()) {
                continue;
            }
            GrepMatch match = parseLine(line, query);
            if (match != null) {
                matches.add(match);
            }
            if (maxResults > 0 && matches.size() >= maxResults) {
                break;
            }
        }
        return matches;
    }

    public GrepMatch parseLine(String outputLine, String query) {
        if (outputLine == null || outputLine.isBlank()) {
            return null;
        }

        List<Integer> colons = colonIndexes(outputLine);
        for (int i = 0; i + 2 < colons.size(); i++) {
            int first = colons.get(i);
            int second = colons.get(i + 1);
            int third = colons.get(i + 2);
            String lineNumber = outputLine.substring(first + 1, second);
            String column = outputLine.substring(second + 1, third);
            if (!isInteger(lineNumber) || !isInteger(column)) {
                continue;
            }

            GrepMatch match = new GrepMatch();
            match.setFilePath(outputLine.substring(0, first).replace('\\', '/'));
            match.setLineNumber(Integer.valueOf(lineNumber));
            match.setColumn(Integer.valueOf(column));
            match.setLineText(outputLine.substring(third + 1));
            match.setQuery(query);
            return match;
        }
        return null;
    }

    private List<Integer> colonIndexes(String value) {
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == ':') {
                indexes.add(i);
            }
        }
        return indexes;
    }

    private boolean isInteger(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
