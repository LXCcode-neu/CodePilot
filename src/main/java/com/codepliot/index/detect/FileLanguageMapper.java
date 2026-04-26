package com.codepliot.index.detect;

import java.nio.file.Path;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * 文件语言映射器。
 * 仅根据文件后缀判断语言类型，不负责读取文件内容。
 */
@Component
public class FileLanguageMapper {

    /**
     * 根据文件路径推断语言。
     */
    public LanguageType map(Path filePath) {
        if (filePath == null || filePath.getFileName() == null) {
            return LanguageType.UNKNOWN;
        }
        return mapFileName(filePath.getFileName().toString());
    }

    /**
     * 根据文件名后缀映射语言。
     */
    public LanguageType mapFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return LanguageType.UNKNOWN;
        }

        String normalized = fileName.toLowerCase(Locale.ROOT);
        if (normalized.endsWith(".java")) {
            return LanguageType.JAVA;
        }
        if (normalized.endsWith(".py")) {
            return LanguageType.PYTHON;
        }
        if (normalized.endsWith(".js") || normalized.endsWith(".jsx")) {
            return LanguageType.JAVASCRIPT;
        }
        if (normalized.endsWith(".ts") || normalized.endsWith(".tsx")) {
            return LanguageType.TYPESCRIPT;
        }
        if (normalized.endsWith(".go")) {
            return LanguageType.GO;
        }
        return LanguageType.UNKNOWN;
    }
}
