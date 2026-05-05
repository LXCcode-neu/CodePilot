package com.codepliot.search;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Builds prioritized grep query terms from issue descriptions.
 */
@Component
public class SearchQueryPlanner {

    private static final int DEFAULT_MAX_TERMS = 12;

    private static final Pattern API_PATH_PATTERN = Pattern.compile("(?<![\\w.-])/[A-Za-z0-9_{}:$.-]+(?:/[A-Za-z0-9_{}:$.-]+)+");
    private static final Pattern EXCEPTION_PATTERN = Pattern.compile("\\b[A-Z][A-Za-z0-9_]*Exception\\b");
    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile("\\b[A-Z][A-Za-z0-9_]*(?:Controller|Service|Impl|Mapper|Repository|Executor|Client|Config|Policy|Handler|Filter|Tool|Builder|Record|Util|Utils|DTO|VO)\\b");
    private static final Pattern METHOD_NAME_PATTERN = Pattern.compile("\\b[a-z][A-Za-z0-9_]*(?:Task|Patch|Run|Confirm|Create|Update|Delete|Search|Index|Login|Execute|Generate|Apply|Build|Code|Verify|Captcha|Random|Order|Voucher|Stock|Save|Get|Set)[A-Za-z0-9_]*\\b");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(?<![\\w.])\\d{1,6}(?![\\w.])");
    private static final Pattern QUOTED_TOKEN_PATTERN = Pattern.compile("[`'\"]([A-Za-z_][A-Za-z0-9_]{2,80})[`'\"]");
    private static final Pattern ASCII_TOKEN_PATTERN = Pattern.compile("\\b[A-Za-z][A-Za-z0-9_]{2,50}\\b");
    private static final Pattern CHINESE_TOKEN_PATTERN = Pattern.compile("[\\p{IsHan}]{2,12}");

    private static final Set<String> TECH_KEYWORDS = Set.of(
            "token", "login", "patch", "diff", "task", "agent", "redis", "jwt", "security",
            "controller", "service", "mapper", "repository", "code", "verify", "verification",
            "captcha", "otp", "sms", "phone", "random", "length", "digit", "digits", "generate",
            "generator", "util", "utils", "password", "auth", "cache", "user", "order", "voucher",
            "coupon", "stock", "seckill", "shop", "blog", "follow", "comment", "upload", "file"
    );

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "and", "for", "with", "from", "this", "that", "when", "then", "there",
            "should", "could", "would", "issue", "bug", "fix", "failed", "error", "problem"
    );

    private static final List<ChineseMapping> CHINESE_MAPPINGS = List.of(
            new ChineseMapping("登录", List.of("login")),
            new ChineseMapping("用户", List.of("user")),
            new ChineseMapping("任务", List.of("task")),
            new ChineseMapping("补丁", List.of("patch")),
            new ChineseMapping("确认", List.of("confirm")),
            new ChineseMapping("安全检查", List.of("safety", "check")),
            new ChineseMapping("仓库", List.of("repo", "repository")),
            new ChineseMapping("检索", List.of("search", "grep")),
            new ChineseMapping("索引", List.of("index", "search")),
            new ChineseMapping("报错", List.of("error", "exception")),
            new ChineseMapping("异常", List.of("error", "exception")),
            new ChineseMapping("权限", List.of("auth", "security")),
            new ChineseMapping("接口", List.of("api", "controller")),
            new ChineseMapping("异步", List.of("async")),
            new ChineseMapping("执行", List.of("execute", "run")),
            new ChineseMapping("状态", List.of("status", "state")),
            new ChineseMapping("验证码", List.of("code", "verify", "verification", "captcha", "random")),
            new ChineseMapping("校验码", List.of("code", "verify", "verification", "captcha", "random")),
            new ChineseMapping("验证", List.of("verify", "verification")),
            new ChineseMapping("短信", List.of("sms", "phone", "code")),
            new ChineseMapping("手机", List.of("phone", "mobile")),
            new ChineseMapping("随机", List.of("random")),
            new ChineseMapping("生成", List.of("generate", "random")),
            new ChineseMapping("长度", List.of("length")),
            new ChineseMapping("位数", List.of("length", "digit", "digits")),
            new ChineseMapping("订单", List.of("order")),
            new ChineseMapping("优惠券", List.of("voucher", "coupon")),
            new ChineseMapping("券", List.of("voucher", "coupon")),
            new ChineseMapping("秒杀", List.of("seckill", "voucher", "order")),
            new ChineseMapping("库存", List.of("stock")),
            new ChineseMapping("店铺", List.of("shop")),
            new ChineseMapping("博客", List.of("blog")),
            new ChineseMapping("关注", List.of("follow")),
            new ChineseMapping("评论", List.of("comment")),
            new ChineseMapping("上传", List.of("upload", "file")),
            new ChineseMapping("缓存", List.of("cache", "redis")),
            new ChineseMapping("令牌", List.of("token", "jwt"))
    );

    public List<String> plan(String issueText) {
        return plan(issueText, DEFAULT_MAX_TERMS);
    }

    public List<String> plan(String issueText, int maxTerms) {
        if (issueText == null || issueText.isBlank() || maxTerms <= 0) {
            return List.of();
        }

        LinkedHashMap<String, Boolean> terms = new LinkedHashMap<>();
        addPatternMatches(terms, issueText, API_PATH_PATTERN);
        addPatternMatches(terms, issueText, EXCEPTION_PATTERN);
        addPatternMatches(terms, issueText, CLASS_NAME_PATTERN);
        addPatternMatches(terms, issueText, METHOD_NAME_PATTERN);
        addPatternGroupMatches(terms, issueText, QUOTED_TOKEN_PATTERN, 1);
        addEnglishKeywords(terms, issueText);
        addPatternMatches(terms, issueText, NUMBER_PATTERN);
        addChineseMappings(terms, issueText);
        addReadableAsciiTokens(terms, issueText);
        addChineseTokens(terms, issueText);

        return terms.keySet().stream()
                .limit(maxTerms)
                .toList();
    }

    public String buildQuery(String issueText) {
        return String.join(" ", plan(issueText));
    }

    private void addPatternMatches(LinkedHashMap<String, Boolean> terms, String issueText, Pattern pattern) {
        Matcher matcher = pattern.matcher(issueText);
        while (matcher.find()) {
            addTerm(terms, matcher.group());
        }
    }

    private void addPatternGroupMatches(LinkedHashMap<String, Boolean> terms, String issueText, Pattern pattern, int group) {
        Matcher matcher = pattern.matcher(issueText);
        while (matcher.find()) {
            addTerm(terms, matcher.group(group));
        }
    }

    private void addEnglishKeywords(LinkedHashMap<String, Boolean> terms, String issueText) {
        String normalized = issueText.toLowerCase(Locale.ROOT);
        for (String keyword : TECH_KEYWORDS) {
            if (containsWord(normalized, keyword)) {
                addTerm(terms, keyword);
            }
        }
    }

    private void addChineseMappings(LinkedHashMap<String, Boolean> terms, String issueText) {
        for (ChineseMapping mapping : CHINESE_MAPPINGS) {
            if (!issueText.contains(mapping.keyword())) {
                continue;
            }
            for (String term : mapping.terms()) {
                addTerm(terms, term);
            }
        }
    }

    private void addReadableAsciiTokens(LinkedHashMap<String, Boolean> terms, String issueText) {
        Matcher matcher = ASCII_TOKEN_PATTERN.matcher(issueText);
        while (matcher.find()) {
            String token = matcher.group();
            String normalized = token.toLowerCase(Locale.ROOT);
            if (!STOP_WORDS.contains(normalized)) {
                addTerm(terms, token);
            }
        }
    }

    private void addChineseTokens(LinkedHashMap<String, Boolean> terms, String issueText) {
        Matcher matcher = CHINESE_TOKEN_PATTERN.matcher(issueText);
        while (matcher.find()) {
            String token = matcher.group();
            if (token.length() <= 6) {
                addTerm(terms, token);
            } else {
                // Long Chinese issue phrases rarely appear verbatim in code; short windows are safer grep probes.
                for (int index = 0; index + 2 <= token.length(); index += 2) {
                    addTerm(terms, token.substring(index, Math.min(index + 4, token.length())));
                }
            }
        }
    }

    private boolean containsWord(String text, String word) {
        return Pattern.compile("\\b" + Pattern.quote(word) + "\\b").matcher(text).find();
    }

    private void addTerm(LinkedHashMap<String, Boolean> terms, String term) {
        if (term == null || term.isBlank()) {
            return;
        }
        String normalized = term.trim();
        if (normalized.length() > 120) {
            return;
        }
        terms.putIfAbsent(normalized, Boolean.TRUE);
    }

    private record ChineseMapping(String keyword, List<String> terms) {
    }
}
