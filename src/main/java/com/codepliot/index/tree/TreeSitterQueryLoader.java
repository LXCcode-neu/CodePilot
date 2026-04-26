package com.codepliot.index.tree;

import com.codepliot.index.detect.LanguageType;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Tree-sitter Query 加载器。
 * 当前阶段只提供按语言和名称读取 classpath query 文件的统一入口。
 */
@Component
public class TreeSitterQueryLoader {

    /**
     * 按语言和 query 名称加载查询脚本。
     * 如果文件不存在或读取失败，则返回空。
     */
    public Optional<String> load(LanguageType languageType, String queryName) {
        if (languageType == null || queryName == null || queryName.isBlank()) {
            return Optional.empty();
        }

        String resourcePath = "tree-sitter/queries/" + languageType.name().toLowerCase() + "/" + queryName + ".scm";
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
