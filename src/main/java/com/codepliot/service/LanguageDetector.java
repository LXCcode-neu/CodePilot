package com.codepliot.service;

import java.nio.file.Path;
import org.springframework.stereotype.Component;
/**
 * LanguageDetector 服务类，负责封装业务流程和领域能力。
 */
@Component
public class LanguageDetector {

    private final FileLanguageMapper fileLanguageMapper;
/**
 * 创建 LanguageDetector 实例。
 */
public LanguageDetector(FileLanguageMapper fileLanguageMapper) {
        this.fileLanguageMapper = fileLanguageMapper;
    }
/**
 * 执行 detect 相关逻辑。
 */
public LanguageType detect(Path filePath) {
        return fileLanguageMapper.map(filePath);
    }
}

