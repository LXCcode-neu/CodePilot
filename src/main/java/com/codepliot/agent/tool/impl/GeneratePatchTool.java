package com.codepliot.agent.tool.impl;

import com.codepliot.agent.context.AgentContext;
import com.codepliot.agent.tool.AgentTool;
import com.codepliot.agent.tool.ToolResult;
import com.codepliot.common.exception.BusinessException;
import com.codepliot.common.result.ErrorCode;
import com.codepliot.llm.prompt.PatchPromptBuilder;
import com.codepliot.llm.service.LlmService;
import com.codepliot.patch.dto.PatchGenerateResult;
import com.codepliot.patch.entity.PatchRecord;
import com.codepliot.patch.service.PatchService;
import com.codepliot.patch.vo.PatchRecordVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.codepliot.task.entity.AgentTaskStatus;
import com.codepliot.trace.entity.AgentStepType;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Mock Patch 生成工具。
 */
@Component
@Order(50)
public class GeneratePatchTool implements AgentTool {

    private final LlmService llmService;
    private final PatchPromptBuilder promptBuilder;
    private final PatchService patchService;
    private final ObjectMapper objectMapper;

    public GeneratePatchTool(LlmService llmService,
                             PatchPromptBuilder promptBuilder,
                             PatchService patchService,
                             ObjectMapper objectMapper) {
        this.llmService = llmService;
        this.promptBuilder = promptBuilder;
        this.patchService = patchService;
        this.objectMapper = objectMapper;
    }

    @Override
    public AgentTaskStatus taskStatus() {
        return AgentTaskStatus.GENERATING_PATCH;
    }

    @Override
    public AgentStepType stepType() {
        return AgentStepType.GENERATE_PATCH;
    }

    @Override
    public String stepName() {
        return "生成 Patch";
    }

    @Override
    public ToolResult execute(AgentContext context) {
        String rawOutput = llmService.generate(
                promptBuilder.buildSystemPrompt(),
                promptBuilder.buildUserPrompt(
                        context.issueTitle(),
                        context.issueDescription(),
                        context.analysis(),
                        context.retrievedChunks()
                )
        );

        try {
            PatchGenerateResult result = PatchGenerateResult.fromRawOutput(objectMapper, rawOutput);
            PatchRecord patchRecord = patchService.saveGeneratedPatch(context.taskId(), result, rawOutput);
            return ToolResult.success("patch generation completed", PatchRecordVO.from(patchRecord));
        } catch (IllegalArgumentException exception) {
            patchService.saveFailedPatch(context.taskId(), rawOutput, "Failed to parse LLM JSON output");
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to parse patch generation JSON output");
        }
    }
}
