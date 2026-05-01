package com.codepliot.service.index;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 统一的关键词提取工具，支持驼峰命名和下划线拆分。
 *
 * <p>被 SearchRelevantCodeTool、LuceneCodeSearchService、CodeRanker 共同使用，
 * 保证关键词提取逻辑的一致性。
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

    private static final int MIN_TOKEN_LENGTH = 2;

    private KeywordExtractor() {
    }

    /**
     * 从文本中提取关键词，支持驼峰和下划线拆分，过滤停用词和短词。
     *
     * @param text 输入文本（issue 标题/描述等）
     * @return 去重后的关键词列表，保持插入顺序
     */
    public static List<String> extractKeywords(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        String[] rawTokens = text.toLowerCase(Locale.ROOT).split("[^a-z0-9_./-]+");
        LinkedHashSet<String> result = new LinkedHashSet<>();

        for (String token : rawTokens) {
            if (token == null || token.isBlank()) {
                continue;
            }
            List<String> subTokens = splitCodeToken(token);
            for (String sub : subTokens) {
                if (sub.length() >= MIN_TOKEN_LENGTH && !STOPWORDS.contains(sub)) {
                    result.add(sub);
                }
            }
        }

        return new ArrayList<>(result);
    }

    /**
     * 提取关键词并返回 Set，供 CodeRanker 使用。
     */
    public static Set<String> extractKeywordSet(String text) {
        return new LinkedHashSet<>(extractKeywords(text));
    }

    /**
     * 拆分代码风格的 token，处理驼峰命名和下划线。
     *
     * <p>示例：
     * <ul>
     *   <li>"getUserById" → ["get", "user", "by", "id"]</li>
     *   <li>"user_id" → ["user", "id"]</li>
     *   <li>"HTMLParser" → ["html", "parser"]</li>
     *   <li>"login" → ["login"]</li>
     * </ul>
     */
    private static List<String> splitCodeToken(String token) {
        List<String> parts = new ArrayList<>();

        // 先按下划线和点拆分
        String[] segments = token.split("[_.]+");
        for (String segment : segments) {
            if (segment.isEmpty()) {
                continue;
            }
            // 再对每段做驼峰拆分
            List<String> camelParts = splitCamelCase(segment);
            parts.addAll(camelParts);
        }

        return parts;
    }

    /**
     * 驼峰命名拆分。连续大写字母视为一个整体（如 "HTML" → "html"），
     * 大写字母后跟小写字母时拆分（如 "getUser" → "get", "user"）。
     */
    private static List<String> splitCamelCase(String input) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (Character.isUpperCase(c)) {
                // 如果当前有积累的小写内容，先输出
                if (current.length() > 0 && containsLowerCase(current)) {
                    result.add(current.toString().toLowerCase(Locale.ROOT));
                    current.setLength(0);
                }
                current.append(c);
            } else if (Character.isDigit(c)) {
                // 数字边界：如果之前是字母，先输出
                if (current.length() > 0 && !containsDigit(current)) {
                    result.add(current.toString().toLowerCase(Locale.ROOT));
                    current.setLength(0);
                }
                current.append(c);
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            result.add(current.toString().toLowerCase(Locale.ROOT));
        }

        return result;
    }

    private static boolean containsLowerCase(StringBuilder sb) {
        for (int i = 0; i < sb.length(); i++) {
            if (Character.isLowerCase(sb.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsDigit(StringBuilder sb) {
        for (int i = 0; i < sb.length(); i++) {
            if (Character.isDigit(sb.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}
