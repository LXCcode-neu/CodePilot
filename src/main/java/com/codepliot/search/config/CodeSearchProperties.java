package com.codepliot.search.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for on-demand code search.
 */
@Component
@ConfigurationProperties(prefix = "codepilot.search")
@Data
public class CodeSearchProperties {

    private String mode = "grep";

    private int maxResults = 30;

    private int contextBeforeLines = 40;

    private int contextAfterLines = 40;

    private int commandTimeoutSeconds = 10;

    private boolean useRipgrep = true;

    private List<String> defaultExcludePatterns = new ArrayList<>(List.of(
            ".git/**",
            "target/**",
            "build/**",
            "dist/**",
            "node_modules/**",
            ".idea/**",
            ".vscode/**"
    ));
}
