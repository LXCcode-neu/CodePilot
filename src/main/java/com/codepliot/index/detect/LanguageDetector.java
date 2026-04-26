package com.codepliot.index.detect;

import java.nio.file.Path;
import org.springframework.stereotype.Component;

/**
 * 语言检测入口。
 * 当前阶段仅委托 FileLanguageMapper 基于后缀做检测。
 */
@Component
public class LanguageDetector {

    private final FileLanguageMapper fileLanguageMapper;

    public LanguageDetector(FileLanguageMapper fileLanguageMapper) {
        this.fileLanguageMapper = fileLanguageMapper;
    }

    /**
     * 根据路径推断语言类型。
     */
    public LanguageType detect(Path filePath) {
        return fileLanguageMapper.map(filePath);
    }
}
