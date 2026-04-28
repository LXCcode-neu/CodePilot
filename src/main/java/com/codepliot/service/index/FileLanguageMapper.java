package com.codepliot.service.index;

import java.nio.file.Path;
import java.util.Locale;
import org.springframework.stereotype.Component;
/**
 * FileLanguageMapper 服务类，负责封装业务流程和领域能力。
 */
@Component
public class FileLanguageMapper {
/**
 * 执行 map 相关逻辑。
 */
public LanguageType map(Path filePath) {
        if (filePath == null || filePath.getFileName() == null) {
            return LanguageType.UNKNOWN;
        }
        return mapFileName(filePath.getFileName().toString());
    }
/**
 * 执行 mapFileName 相关逻辑。
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
