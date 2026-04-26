package com.codepliot.index.detect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FileLanguageMapperTest {

    private final FileLanguageMapper fileLanguageMapper = new FileLanguageMapper();
    private final LanguageDetector languageDetector = new LanguageDetector(fileLanguageMapper);

    @Test
    void shouldMapKnownSuffixesToLanguageTypes() {
        assertEquals(LanguageType.JAVA, fileLanguageMapper.mapFileName("Demo.java"));
        assertEquals(LanguageType.PYTHON, fileLanguageMapper.mapFileName("demo.py"));
        assertEquals(LanguageType.JAVASCRIPT, fileLanguageMapper.mapFileName("index.jsx"));
        assertEquals(LanguageType.TYPESCRIPT, fileLanguageMapper.mapFileName("app.tsx"));
        assertEquals(LanguageType.GO, fileLanguageMapper.mapFileName("main.go"));
    }

    @Test
    void shouldReturnUnknownForUnsupportedFileNames() {
        assertEquals(LanguageType.UNKNOWN, fileLanguageMapper.mapFileName("README.md"));
        assertEquals(LanguageType.UNKNOWN, fileLanguageMapper.mapFileName(""));
        assertEquals(LanguageType.UNKNOWN, fileLanguageMapper.map(null));
    }

    @Test
    void shouldDetectLanguageFromPath() {
        assertEquals(LanguageType.JAVA, languageDetector.detect(Path.of("src/main/java/App.java")));
        assertEquals(LanguageType.UNKNOWN, languageDetector.detect(Path.of("docs/guide.txt")));
    }
}
