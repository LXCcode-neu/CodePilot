package com.codepliot.index.detect;

import java.nio.file.Path;
import org.springframework.stereotype.Component;

@Component
public class LanguageDetector {

    private final FileLanguageMapper fileLanguageMapper;

    public LanguageDetector(FileLanguageMapper fileLanguageMapper) {
        this.fileLanguageMapper = fileLanguageMapper;
    }

    public LanguageType detect(Path filePath) {
        return fileLanguageMapper.map(filePath);
    }
}
