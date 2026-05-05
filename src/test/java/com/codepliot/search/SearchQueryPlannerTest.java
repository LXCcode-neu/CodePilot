package com.codepliot.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SearchQueryPlannerTest {

    private final SearchQueryPlanner planner = new SearchQueryPlanner();

    @Test
    void shouldExtractPrioritizedIssueTerms() {
        List<String> terms = planner.plan("""
                POST /api/tasks/{taskId}/run throws NullPointerException in AgentExecutor.
                login token patch task controller service
                """);

        assertEquals("/api/tasks/{taskId}/run", terms.get(0));
        assertTrue(terms.contains("NullPointerException"));
        assertTrue(terms.contains("AgentExecutor"));
        assertTrue(terms.contains("login"));
        assertTrue(terms.contains("token"));
    }

    @Test
    void shouldMapChineseKeywordsAndLimitTermCount() {
        List<String> mappedTerms = planner.plan("登录用户任务补丁确认安全检查仓库检索索引报错权限接口异步执行状态", 30);
        List<String> limitedTerms = planner.plan("登录用户任务补丁确认安全检查仓库检索索引报错权限接口异步执行状态", 5);

        assertTrue(mappedTerms.contains("login"));
        assertTrue(mappedTerms.contains("user"));
        assertEquals(5, limitedTerms.size());
    }
}
