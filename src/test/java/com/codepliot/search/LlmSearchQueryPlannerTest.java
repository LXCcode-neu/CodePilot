package com.codepliot.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codepliot.search.dto.SearchRequest;
import com.codepliot.service.llm.LlmService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class LlmSearchQueryPlannerTest {

    @Test
    void shouldParseLlmPlannedGrepAndGlobQueries() {
        LlmService llmService = new LlmService((systemPrompt, userPrompt) -> """
                {
                  "queries": [
                    {
                      "pattern": "RandomUtil.randomNumbers",
                      "regexEnabled": false,
                      "globPatterns": ["**/*.java"],
                      "reason": "verification code generator"
                    }
                  ],
                  "fileGlobs": ["**/*Service*.java"]
                }
                """);
        LlmSearchQueryPlanner planner = new LlmSearchQueryPlanner(llmService, new ObjectMapper());

        SearchPlan plan = planner.plan(request("修复验证码不为6位的问题"), List.of("captcha", "random"), 8);

        assertEquals("llm", plan.source());
        assertEquals("RandomUtil.randomNumbers", plan.queries().get(0).pattern());
        assertTrue(plan.queries().get(0).globPatterns().contains("**/*.java"));
        assertTrue(plan.fileGlobs().contains("**/*Service*.java"));
    }

    @Test
    void shouldFallbackWhenLlmReturnsPlainText() {
        LlmService llmService = new LlmService((systemPrompt, userPrompt) -> "not json");
        LlmSearchQueryPlanner planner = new LlmSearchQueryPlanner(llmService, new ObjectMapper());

        SearchPlan plan = planner.plan(request("修复验证码不为6位的问题"), List.of("captcha", "random"), 8);

        assertFalse(plan.queries().isEmpty());
        assertEquals("captcha", plan.queries().get(0).pattern());
    }

    private SearchRequest request(String issueText) {
        SearchRequest request = new SearchRequest();
        request.setRepoPath(".");
        request.setIssueText(issueText);
        request.setQuery(issueText);
        request.setMaxResults(10);
        return request;
    }
}
