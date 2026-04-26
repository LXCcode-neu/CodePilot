package com.codepliot.index.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codepliot.index.detect.FileLanguageMapper;
import com.codepliot.index.detect.LanguageDetector;
import com.codepliot.index.detect.LanguageType;
import com.codepliot.index.dto.TreeSitterParseResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TreeSitterParserServiceTest {

    @TempDir
    Path tempDir;

    private final TreeSitterParserService parserService = new TreeSitterParserService(
            new LanguageDetector(new FileLanguageMapper()),
            new TreeSitterLanguageRegistry()
    );

    @Test
    void shouldReturnFailureWhenSourceFileCannotBeRead() {
        Path missingFile = tempDir.resolve("Missing.java");

        TreeSitterParseResult result = parserService.parse(missingFile, LanguageType.JAVA);

        assertFalse(result.success());
        assertTrue(result.errorMessage().startsWith("Failed to read source file"));
        assertNull(result.astObject());
    }

    @Test
    void shouldReturnFailureWhenLanguageIsUnsupported() throws IOException {
        Path file = tempDir.resolve("README.md");
        Files.writeString(file, "plain text fallback", StandardCharsets.UTF_8);

        TreeSitterParseResult result = parserService.parse(file, LanguageType.UNKNOWN);

        assertEquals(LanguageType.UNKNOWN, result.language());
        assertFalse(result.success());
        assertEquals("plain text fallback", result.sourceCode());
        assertNotNull(result.errorMessage());
        assertNull(result.astObject());
    }

    @Test
    void shouldAutoDetectLanguageWhenParsingByPath() throws IOException {
        Path file = tempDir.resolve("script.py");
        Files.writeString(file, "print('hello')", StandardCharsets.UTF_8);

        TreeSitterParseResult result = parserService.parse(file);

        assertEquals(LanguageType.PYTHON, result.language());
        assertTrue(result.filePath().endsWith("script.py"));
    }
}
