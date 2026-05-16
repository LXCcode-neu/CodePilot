package com.codepliot.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CliConfigStore {

    private final ObjectMapper objectMapper;
    private final Path configPath;

    public CliConfigStore(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.configPath = Path.of(System.getProperty("user.home"), ".codepilot", "config.json");
    }

    public CliConfig load() {
        if (!Files.isRegularFile(configPath)) {
            return CliConfig.empty();
        }
        try {
            return objectMapper.readValue(configPath.toFile(), CliConfig.class);
        } catch (IOException exception) {
            throw new CliException(1, "读取 CLI 配置失败: " + exception.getMessage());
        }
    }

    public void save(CliConfig config) {
        try {
            Files.createDirectories(configPath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(configPath.toFile(), config);
        } catch (IOException exception) {
            throw new CliException(1, "写入 CLI 配置失败: " + exception.getMessage());
        }
    }

    public Path configPath() {
        return configPath;
    }
}
