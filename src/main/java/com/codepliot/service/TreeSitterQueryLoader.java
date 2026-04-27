package com.codepliot.service;

import com.codepliot.service.LanguageType;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
/**
 * TreeSitterQueryLoader 服务类，负责封装业务流程和领域能力。
 */
@Component
public class TreeSitterQueryLoader {
/**
 * 执行 load 相关逻辑。
 */
public Optional<String> load(LanguageType languageType, String queryName) {
        if (languageType == null || queryName == null || queryName.isBlank()) {
            return Optional.empty();
        }

        String resourcePath = "tree-sitter/queries/" + languageType.name().toLowerCase(Locale.ROOT) + "/" + queryName + ".scm";
        ClassPathResource resource = new ClassPathResource(resourcePath);
        if (!resource.exists()) {
            return Optional.empty();
        }

        try (InputStream inputStream = resource.getInputStream()) {
            byte[] bytes = inputStream.readAllBytes();
            return Optional.of(new String(bytes, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            return Optional.empty();
        }
    }
}

