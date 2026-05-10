package com.codepliot.service.agent.tool;

import com.codepliot.model.AgentContext;
import com.codepliot.service.agent.AgentTool;
import com.codepliot.service.agent.ToolResult;
import com.codepliot.service.llm.IssueAnalysisPromptBuilder;
import com.codepliot.service.llm.LlmService;
import com.codepliot.entity.AgentTaskStatus;
import com.codepliot.entity.AgentStepType;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
/**
 * AnalyzeIssueTool 服务类，负责封装业务流程和领域能力。
 */
@Component
@Order(30)
public class AnalyzeIssueTool implements AgentTool {

    private final LlmService llmService;
    private final IssueAnalysisPromptBuilder promptBuilder;
/**
 * 创建 AnalyzeIssueTool 实例。
 */
public AnalyzeIssueTool(LlmService llmService, IssueAnalysisPromptBuilder promptBuilder) {
        this.llmService = llmService;
        this.promptBuilder = promptBuilder;
    }
    /**
     * 执行 taskStatus 相关逻辑。
     */
@Override
    public AgentTaskStatus taskStatus() {
        return AgentTaskStatus.ANALYZING;
    }
    /**
     * 执行 stepType 相关逻辑。
     */
@Override
    public AgentStepType stepType() {
        return AgentStepType.ANALYZE_ISSUE;
    }
    /**
     * 执行 stepName 相关逻辑。
     */
@Override
    public String stepName() {
        return "分析 Issue";
    }
    /**
     * 执行 execute 相关逻辑。
     */
@Override
    public ToolResult execute(AgentContext context) {
        String analysis = llmService.generate(
                promptBuilder.buildSystemPrompt(),
                promptBuilder.buildUserPrompt(
                        context.issueTitle(),
                        context.issueDescription(),
                        context.retrievedChunks()
                )
        );
        context.updateAnalysis(analysis);
        return ToolResult.success("issue analysis completed", Map.of(
                "issueTitle", context.issueTitle(),
                "retrievedChunkCount", context.retrievedChunks().size(),
                "analysis", analysis
        ));
    }
}
