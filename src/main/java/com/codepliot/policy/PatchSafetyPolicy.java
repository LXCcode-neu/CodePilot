package com.codepliot.policy;

import com.codepliot.model.PatchSafetyCheckResult;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Patch 安全策略。
 *
 * <p>负责在 patch 生成完成后进行静态规则检查，识别需要人工重点确认的风险信号。
 */
@Component
public class PatchSafetyPolicy {

    private static final Pattern DIFF_FILE_PATTERN = Pattern.compile("(?m)^\\+\\+\\+\\s+b/(.+)$");
    private static final Set<String> CONFIG_FILE_NAMES = Set.of(
            "application.yml", "application.yaml", "application.properties", ".env", "pom.xml",
            "package.json", "package-lock.json", "dockerfile", "docker-compose.yml", "docker-compose.yaml"
    );
    private static final Set<String> CONFIG_EXTENSIONS = Set.of(
            "yml", "yaml", "properties", "json", "xml", "toml", "ini", "conf", "cfg"
    );
    private static final List<String> SENSITIVE_PATH_KEYWORDS = List.of(
            "security", "auth", "permission", "role", "jwt", "secret", "token", "credential", "key"
    );
    private static final List<String> PERMISSION_BYPASS_KEYWORDS = List.of(
            "permitall", "allowall", "skipauth", "bypass", "disablecsrf", "ignoreauth",
            "isadmin=true", "isadmin = true", "role_admin", "super_admin", "anonymous()"
    );

    /**
     * 对 patch 进行安全评估。
     */
    public PatchSafetyCheckResult evaluate(String patch) {
        String normalizedPatch = patch == null ? "" : patch.trim();
        List<String> touchedFiles = extractTouchedFiles(normalizedPatch);
        Set<String> touchedLanguages = new LinkedHashSet<>();
        List<String> riskItems = new ArrayList<>();

        int addedLineCount = countAddedLines(normalizedPatch);
        int removedLineCount = countRemovedLines(normalizedPatch);

        boolean emptyPatch = normalizedPatch.isBlank();
        boolean sensitiveFileModified = false;
        boolean configFileModified = false;

        for (String file : touchedFiles) {
            String normalizedPath = file.toLowerCase(Locale.ROOT);
            touchedLanguages.add(detectLanguage(normalizedPath));
            if (isSensitiveFile(normalizedPath)) {
                sensitiveFileModified = true;
            }
            if (isConfigFile(normalizedPath)) {
                configFileModified = true;
            }
        }

        boolean largeDeletionSuspected = removedLineCount >= 80
                || (removedLineCount >= 40 && removedLineCount > addedLineCount * 2);
        boolean permissionBypassRisk = containsPermissionBypassPattern(normalizedPatch);
        boolean crossLanguageWideModification = touchedFiles.size() >= 4 && touchedLanguages.size() >= 2;

        if (emptyPatch) {
            riskItems.add("patch 为空，当前结果仅包含建议说明。");
        }
        if (sensitiveFileModified) {
            riskItems.add("修改涉及安全或权限相关敏感文件，需要人工复核。");
        }
        if (largeDeletionSuspected) {
            riskItems.add("疑似删除大量代码，需要确认是否存在误删。");
        }
        if (permissionBypassRisk) {
            riskItems.add("检测到疑似权限绕过或安全放宽关键词，需要重点确认。");
        }
        if (configFileModified) {
            riskItems.add("修改涉及配置文件，可能影响运行环境或部署行为。");
        }
        if (crossLanguageWideModification) {
            riskItems.add("修改跨越多种语言且涉及文件较多，建议人工整体审查。");
        }
        if (riskItems.isEmpty()) {
            riskItems.add("未发现明显高风险信号，仍建议人工确认后再进入后续流程。");
        }

        return new PatchSafetyCheckResult(
                emptyPatch,
                sensitiveFileModified,
                largeDeletionSuspected,
                permissionBypassRisk,
                configFileModified,
                crossLanguageWideModification,
                touchedFiles.size(),
                touchedLanguages.size(),
                addedLineCount,
                removedLineCount,
                List.copyOf(touchedFiles),
                List.copyOf(touchedLanguages),
                List.copyOf(riskItems),
                String.join("；", riskItems)
        );
    }

    /**
     * 从 unified diff 中提取被修改的文件路径。
     */
    private List<String> extractTouchedFiles(String patch) {
        if (patch.isBlank()) {
            return List.of();
        }
        Set<String> files = new LinkedHashSet<>();
        Matcher matcher = DIFF_FILE_PATTERN.matcher(patch);
        while (matcher.find()) {
            files.add(matcher.group(1).trim());
        }
        return List.copyOf(files);
    }

    /**
     * 统计新增行数，忽略 diff 头部元数据。
     */
    private int countAddedLines(String patch) {
        int count = 0;
        for (String line : patch.split("\\R")) {
            if (line.startsWith("+") && !line.startsWith("+++")) {
                count++;
            }
        }
        return count;
    }

    /**
     * 统计删除行数，忽略 diff 头部元数据。
     */
    private int countRemovedLines(String patch) {
        int count = 0;
        for (String line : patch.split("\\R")) {
            if (line.startsWith("-") && !line.startsWith("---")) {
                count++;
            }
        }
        return count;
    }

    /**
     * 判断文件是否属于敏感路径。
     */
    private boolean isSensitiveFile(String normalizedPath) {
        for (String keyword : SENSITIVE_PATH_KEYWORDS) {
            if (normalizedPath.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断文件是否为配置文件。
     */
    private boolean isConfigFile(String normalizedPath) {
        String fileName = normalizedPath.substring(normalizedPath.lastIndexOf('/') + 1);
        if (CONFIG_FILE_NAMES.contains(fileName)) {
            return true;
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return false;
        }
        String extension = fileName.substring(dotIndex + 1);
        return CONFIG_EXTENSIONS.contains(extension);
    }

    /**
     * 判断 patch 内容中是否包含疑似权限绕过信号。
     */
    private boolean containsPermissionBypassPattern(String patch) {
        String lowered = patch.toLowerCase(Locale.ROOT);
        for (String keyword : PERMISSION_BYPASS_KEYWORDS) {
            if (lowered.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据文件后缀推断语言类型，用于识别是否存在跨语言大范围修改。
     */
    private String detectLanguage(String normalizedPath) {
        int dotIndex = normalizedPath.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == normalizedPath.length() - 1) {
            return "unknown";
        }
        String extension = normalizedPath.substring(dotIndex + 1);
        return switch (extension) {
            case "java" -> "java";
            case "py" -> "python";
            case "js", "jsx" -> "javascript";
            case "ts", "tsx" -> "typescript";
            case "go" -> "go";
            case "yml", "yaml", "properties", "xml", "json", "toml" -> "config";
            default -> extension;
        };
    }
}
