package com.codepliot.search.read;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codepliot.search.dto.CodeSnippet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CodeReadServiceTest {

    @TempDir
    Path tempDir;

    private final CodeReadService service = new CodeReadService();

    @Test
    void shouldReadContextWithLineNumbers() throws IOException {
        write("src/App.java", numberedLines(8));

        CodeSnippet snippet = service.readSnippet(tempDir.toString(), "src/App.java", 4, 1, 2);

        assertEquals(3, snippet.getStartLine());
        assertEquals(6, snippet.getEndLine());
        assertTrue(snippet.getContentWithLineNumbers().contains("     3 | line 3"));
        assertTrue(snippet.getContentWithLineNumbers().contains("     6 | line 6"));
    }

    @Test
    void shouldHandleBeginningAndEndOfFile() throws IOException {
        write("src/App.java", numberedLines(3));

        CodeSnippet beginning = service.readSnippet(tempDir.toString(), "src/App.java", 1, 5, 1);
        CodeSnippet end = service.readSnippet(tempDir.toString(), "src/App.java", 3, 1, 5);

        assertEquals(1, beginning.getStartLine());
        assertEquals(2, beginning.getEndLine());
        assertEquals(2, end.getStartLine());
        assertEquals(3, end.getEndLine());
    }

    @Test
    void shouldRejectPathTraversal() throws IOException {
        write("src/App.java", numberedLines(3));

        assertThrows(IllegalArgumentException.class,
                () -> service.readSnippet(tempDir.toString(), "../outside.txt", 1, 1, 1));
    }

    private void write(String relativePath, String content) throws IOException {
        Path file = tempDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    private String numberedLines(int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i <= count; i++) {
            builder.append("line ").append(i).append('\n');
        }
        return builder.toString();
    }
}
