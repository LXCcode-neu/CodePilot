package com.codepliot.service.index;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 统一的关键词提取工具，兼顾英文代码词、数字和常见中文 Issue 表达。
 */
public final class KeywordExtractor {

    private static final Set<String> STOPWORDS = Set.of(
            "the", "and", "for", "with", "from", "into", "that", "this", "when", "then", "than",
            "does", "doesnt", "cant", "cannot", "will", "should", "need", "after", "before",
            "have", "has", "had", "there", "their", "them", "they", "user", "users", "please",
            "check", "ensure", "keep", "show", "move", "issue", "also", "been", "being",
            "would", "could", "might", "just", "very", "some", "each", "more", "most",
            "other", "about", "which", "these", "those", "only", "well", "what", "your"
    );

    private static final Map<String, List<String>> CHINESE_HINTS = Map.ofEntries(
            Map.entry("验证码", List.of("code", "verify", "captcha", "login")),
            Map.entry("校验码", List.of("code", "verify", "captcha")),
            Map.entry("短信", List.of("sms", "message", "code")),
            Map.entry("手机", List.of("phone", "mobile")),
            Map.entry("登录", List.of("login", "auth", "code")),
            Map.entry("随机", List.of("random", "generate")),
            Map.entry("生成", List.of("generate", "random", "create")),
            Map.entry("长度", List.of("length", "digits")),
            Map.entry("位", List.of("digits", "length")),
            Map.entry("用户", List.of("user")),
            Map.entry("注册", List.of("register", "signup")),
            Map.entry("密码", List.of("password", "credential"))
    );

    private KeywordExtractor() {
    }

    public static List<String> extractKeywords(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        LinkedHashSet<String> keywords = new LinkedHashSet<>();

        for (String token : text.split("[^A-Za-z0-9_./-]+")) {
            if (token == null || token.isBlank()) {
                continue;
            }
            for (String part : splitCodeToken(token)) {
                if (shouldKeep(part)) {
                    keywords.add(part);
                }
            }
        }

        for (Map.Entry<String, List<String>> entry : CHINESE_HINTS.entrySet()) {
            if (!text.contains(entry.getKey())) {
                continue;
            }
            keywords.addAll(entry.getValue());
        }

        return new ArrayList<>(keywords);
    }

    public static Set<String> extractKeywordSet(String text) {
        return new LinkedHashSet<>(extractKeywords(text));
    }

    public static String buildQuery(String... parts) {
        LinkedHashMap<String, Boolean> ordered = new LinkedHashMap<>();
        if (parts != null) {
            for (String part : parts) {
                for (String keyword : extractKeywords(part)) {
                    ordered.put(keyword, Boolean.TRUE);
                }
            }
        }
        return String.join(" ", ordered.keySet());
    }

    private static boolean shouldKeep(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        if (token.length() == 1) {
            return Character.isDigit(token.charAt(0));
        }
        return !STOPWORDS.contains(token);
    }

    private static List<String> splitCodeToken(String token) {
        List<String> parts = new ArrayList<>();
        for (String segment : token.split("[_.]+")) {
            if (segment.isEmpty()) {
                continue;
            }
            parts.addAll(splitCamelCase(segment));
        }
        return parts;
    }

    private static List<String> splitCamelCase(String input) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (Character.isUpperCase(ch) && current.length() > 0 && hasLowerCase(current)) {
                parts.add(current.toString().toLowerCase(Locale.ROOT));
                current.setLength(0);
            } else if (Character.isDigit(ch) && current.length() > 0 && !hasDigit(current)) {
                parts.add(current.toString().toLowerCase(Locale.ROOT));
                current.setLength(0);
            }
            current.append(ch);
        }

        if (current.length() > 0) {
            parts.add(current.toString().toLowerCase(Locale.ROOT));
        }
        return parts;
    }

    private static boolean hasLowerCase(StringBuilder value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isLowerCase(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasDigit(StringBuilder value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isDigit(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}
