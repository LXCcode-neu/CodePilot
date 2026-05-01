package com.codepliot.service.index;

import com.codepliot.service.llm.LlmService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 使用 LLM 对 issue 进行查询改写，提取更精确的检索关键词。
 *
 * <p>当 LLM 调用失败时自动回退到基于规则的关键词提取。
 */
@Component
public class QueryRewriter {

    private static final Logger log = LoggerFactory.getLogger(QueryRewriter.class);

    private static final String SYSTEM_PROMPT = """
            You are a code search query generator. Given a GitHub issue title and description,
            extract the most relevant keywords for searching a code repository.

            Output ONLY a space-separated list of keywords, nothing else. Rules:
            - Include technical terms: class names, method names, file patterns, error types
            - Include domain concepts: authentication, payment, database, etc.
            - Include technology names if mentioned: react, spring, redis, etc.
            - Do NOT include common English words (the, is, have, etc.)
            - Do NOT include action words (fix, add, update, etc.)
            - Maximum 15 keywords
            - Lowercase all output

            Example:
            Issue: "Login button doesn't work after clicking submit - returns 401 unauthorized"
            Output: login authentication button submit 401 unauthorized error response api
            """;

    private final LlmService llmService;

    public QueryRewriter(LlmService llmService) {
        this.llmService = llmService;
    }

    /**
     * 对 issue 进行查询改写，返回优化后的检索关键词。
     *
     * @param issueTitle       issue 标题
     * @param issueDescription issue 描述
     * @return 改写后的查询字符串（空格分隔的关键词）
     */
    public String rewrite(String issueTitle, String issueDescription) {
        String userPrompt = buildUserPrompt(issueTitle, issueDescription);

        try {
            String result = llmService.generate(SYSTEM_PROMPT, userPrompt);
            if (result != null && !result.isBlank()) {
                String cleaned = result.strip();
                log.debug("LLM query rewrite result: {}", cleaned);
                return cleaned;
            }
        } catch (Exception e) {
            log.warn("LLM query rewrite failed, falling back to keyword extraction: {}", e.getMessage());
        }

        return fallbackRewrite(issueTitle, issueDescription);
    }

    private String buildUserPrompt(String issueTitle, String issueDescription) {
        StringBuilder sb = new StringBuilder();
        sb.append("Issue Title: ").append(issueTitle == null ? "" : issueTitle.strip());
        sb.append("\n");
        sb.append("Issue Description: ").append(issueDescription == null ? "" : issueDescription.strip());
        return sb.toString();
    }

    /**
     * 当 LLM 不可用时的兜底策略：使用 KeywordExtractor 提取关键词。
     */
    private String fallbackRewrite(String issueTitle, String issueDescription) {
        String combined = ((issueTitle == null ? "" : issueTitle) + " " + (issueDescription == null ? "" : issueDescription)).trim();
        List<String> keywords = KeywordExtractor.extractKeywords(combined);
        if (keywords.isEmpty()) {
            return combined;
        }
        return String.join(" ", keywords.stream().limit(15).toList());
    }
}
