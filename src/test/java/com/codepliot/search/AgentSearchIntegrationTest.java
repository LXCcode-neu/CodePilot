package com.codepliot.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codepliot.entity.PatchRecord;
import com.codepliot.model.AgentContext;
import com.codepliot.model.AgentTaskVO;
import com.codepliot.model.PatchGenerateResult;
import com.codepliot.model.PatchRecordVO;
import com.codepliot.model.PatchSafetyCheckResult;
import com.codepliot.policy.PatchSafetyPolicy;
import com.codepliot.search.config.CodeSearchProperties;
import com.codepliot.search.dto.CodeSearchResult;
import com.codepliot.search.grep.RipgrepCommandBuilder;
import com.codepliot.search.grep.RipgrepResultParser;
import com.codepliot.service.agent.AgentExecutor;
import com.codepliot.service.agent.ToolResult;
import com.codepliot.search.grep.GrepSearchService;
import com.codepliot.search.glob.FileGlobService;
import com.codepliot.search.read.CodeReadService;
import com.codepliot.service.agent.tool.AnalyzeIssueTool;
import com.codepliot.service.agent.tool.GeneratePatchTool;
import com.codepliot.service.agent.tool.SearchRelevantCodeTool;
import com.codepliot.service.llm.IssueAnalysisPromptBuilder;
import com.codepliot.service.llm.LlmService;
import com.codepliot.service.llm.PatchPromptBuilder;
import com.codepliot.service.patch.PatchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AgentSearchIntegrationTest {

    @TempDir
    Path tempDir;

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
                CodeReadService.class
        );

        for (Field field : AgentExecutor.class.getDeclaredFields()) {
            assertFalse(forbiddenTypes.contains(field.getType()), "Forbidden dependency: " + field.getType());
            assertFalse(field.getType().getName().contains("Lucene"), "Forbidden Lucene dependency: " + field.getType());
            assertFalse(field.getType().getName().contains("TreeSitter"), "Forbidden TreeSitter dependency: " + field.getType());
        }
    }

    @Test
    void grepSearchContextShouldContinueThroughAnalysisAndPatchGeneration() throws IOException {
        Path controller = tempDir.resolve("src/main/java/com/example/UserController.java");
        Files.createDirectories(controller.getParent());
        Files.writeString(controller, """
                package com.example;

                import org.springframework.web.bind.annotation.PostMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class UserController {
                    @PostMapping("/api/user/login")
                    public String login() {
                        return "ok";
                    }
                }
                """);

        CodeSearchProperties properties = new CodeSearchProperties();
        properties.setUseRipgrep(false);
        properties.setMaxResults(5);
        properties.setContextBeforeLines(2);
        properties.setContextAfterLines(2);
        GrepSearchService grepSearchService = new GrepSearchService(
                properties,
                new RipgrepCommandBuilder(),
                new RipgrepResultParser()
        );
        GrepCodeSearchFacade facade = new GrepCodeSearchFacade(
                new SearchQueryPlanner(),
                grepSearchService,
                new CodeReadService(),
                properties
        );
        SearchRelevantCodeTool searchTool = new SearchRelevantCodeTool(facade, properties);
        LlmService llmService = new LlmService((systemPrompt, userPrompt) -> {
            if (userPrompt.contains("\"patch\"") && userPrompt.contains("\"risk\"")) {
                return """
                        {
                          "analysis": "found login controller",
                          "solution": "keep existing behavior",
                          "patch": "",
                          "risk": "no code change generated in test"
                        }
                        """;
            }
            assertTrue(userPrompt.contains("UserController"));
            return "Analysis saw UserController login endpoint.";
        });
        AnalyzeIssueTool analyzeTool = new AnalyzeIssueTool(llmService, new IssueAnalysisPromptBuilder());
        RecordingPatchService patchService = new RecordingPatchService();
        GeneratePatchTool patchTool = new GeneratePatchTool(
                llmService,
                new PatchPromptBuilder(),
                patchService,
                new PatchSafetyPolicy(),
                new ObjectMapper()
        );
        AgentContext context = new AgentContext(
                10L,
                20L,
                30L,
                "https://example.com/repo.git",
                "repo",
                tempDir.toString(),
                "Login endpoint fails",
                "POST /api/user/login should inspect UserController"
        );

        ToolResult searchResult = searchTool.execute(context);
        ToolResult analysisResult = analyzeTool.execute(context);
        ToolResult patchResult = patchTool.execute(context);

        assertTrue(searchResult.success());
        assertFalse(context.retrievedChunks().isEmpty());
        assertTrue(context.retrievedChunks().get(0).content().contains("UserController"));
        assertTrue(analysisResult.success());
        assertNotNull(context.analysis());
        assertTrue(patchResult.success());
        assertNotNull(context.patchSafetyCheckResult());
        assertNotNull(patchService.savedPatch.get());
        assertEquals(10L, patchService.savedPatch.get().getTaskId());
    }

    private static class RecordingPatchService implements PatchService {

        private final AtomicReference<PatchRecord> savedPatch = new AtomicReference<>();

        @Override
        public PatchRecord saveGeneratedPatch(Long taskId,
                                              PatchGenerateResult result,
                                              String rawOutput,
                                              PatchSafetyCheckResult safetyCheckResult) {
            PatchRecord patchRecord = new PatchRecord();
            patchRecord.setId(1L);
            patchRecord.setTaskId(taskId);
            patchRecord.setAnalysis(result.analysis());
            patchRecord.setSolution(result.solution());
            patchRecord.setPatch(result.patch());
            patchRecord.setRisk(result.risk());
            patchRecord.setRawOutput(rawOutput);
            patchRecord.setConfirmed(Boolean.FALSE);
            savedPatch.set(patchRecord);
            return patchRecord;
        }

        @Override
        public PatchRecord saveFailedPatch(Long taskId, String rawOutput, String risk) {
            PatchRecord patchRecord = new PatchRecord();
            patchRecord.setTaskId(taskId);
            patchRecord.setRawOutput(rawOutput);
            patchRecord.setRisk(risk);
            savedPatch.set(patchRecord);
            return patchRecord;
        }

        @Override
        public PatchRecordVO getTaskPatch(Long taskId) {
            return PatchRecordVO.from(savedPatch.get());
        }

        @Override
        public AgentTaskVO confirmTaskPatch(Long taskId) {
            return null;
        }
    }
}
