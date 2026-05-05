package com.codepliot.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codepliot.entity.ProjectRepo;
import com.codepliot.model.AgentContext;
import com.codepliot.search.config.CodeSearchProperties;
import com.codepliot.search.dto.CodeSearchResult;
import com.codepliot.search.dto.SearchRequest;
import com.codepliot.service.agent.AgentExecutor;
import com.codepliot.service.agent.ToolResult;
import com.codepliot.service.agent.tool.SearchRelevantCodeTool;
import com.codepliot.search.grep.GrepSearchService;
import com.codepliot.search.glob.FileGlobService;
import com.codepliot.search.read.CodeReadService;
import com.codepliot.service.index.lucene.LuceneCodeSearchService;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.Test;

class AgentSearchIntegrationTest {

    @Test
    void searchRelevantCodeToolShouldUseCodeSearchFacadeAndPopulateAgentContext() {
        CodeSearchFacade facade = request -> {
            CodeSearchResult result = new CodeSearchResult();
            result.setFilePath("src/main/java/com/example/UserController.java");
            result.setStartLine(10);
            result.setEndLine(12);
            result.setScore(9.0d);
            result.setReason("test");
            result.setContentWithLineNumbers("    10 | public class UserController {}");
            return List.of(result);
        };
        SearchRelevantCodeTool tool = new SearchRelevantCodeTool(facade, new CodeSearchProperties());
        AgentContext context = new AgentContext(
                1L,
                2L,
                3L,
                "https://example.com/repo.git",
                "repo",
                ".",
                "UserController bug",
                "Fix controller"
        );

        ToolResult result = tool.execute(context);

        assertTrue(result.success());
        assertEquals(1, context.retrievedChunks().size());
        assertEquals("src/main/java/com/example/UserController.java", context.retrievedChunks().get(0).filePath());
    }

    @Test
    void agentExecutorShouldNotDirectlyDependOnSearchImplementations() {
        List<Class<?>> forbiddenTypes = List.of(
                GrepSearchService.class,
                FileGlobService.class,
                CodeReadService.class,
                LuceneCodeSearchService.class
        );

        for (Field field : AgentExecutor.class.getDeclaredFields()) {
            assertFalse(forbiddenTypes.contains(field.getType()), "Forbidden dependency: " + field.getType());
            assertFalse(field.getType().getName().contains("TreeSitter"), "Forbidden TreeSitter dependency: " + field.getType());
        }
    }
}
