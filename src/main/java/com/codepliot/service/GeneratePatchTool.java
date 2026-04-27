package com.codepliot.service;

import com.codepliot.model.AgentContext;
import com.codepliot.service.AgentTool;
import com.codepliot.service.ToolResult;
import com.codepliot.exception.BusinessException;
import com.codepliot.model.ErrorCode;
import com.codepliot.service.PatchPromptBuilder;
import com.codepliot.service.LlmService;
import com.codepliot.model.PatchGenerateResult;
import com.codepliot.entity.PatchRecord;
import com.codepliot.service.PatchService;
import com.codepliot.model.PatchRecordVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.codepliot.entity.AgentTaskStatus;
import com.codepliot.entity.AgentStepType;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
/**
 * GeneratePatchTool 服务类，负责封装业务流程和领域能力。
 */
@Component
@Order(50)
public class GeneratePatchTool implements AgentTool {

    private final LlmService llmService;
    private final PatchPromptBuilder promptBuilder;
    private final PatchService patchService;
    private final ObjectMapper objectMapper;
/**
 * 创建 GeneratePatchTool 实例。
 */
public GeneratePatchTool(LlmService llmService,
                             PatchPromptBuilder promptBuilder,
                             PatchService patchService,
                             ObjectMapper objectMapper) {
        this.llmService = llmService;
        this.promptBuilder = promptBuilder;
        this.patchService = patchService;
        this.objectMapper = objectMapper;
    }
    /**
     * 执行 taskStatus 相关逻辑。
     */
@Override
    public AgentTaskStatus taskStatus() {
        return AgentTaskStatus.GENERATING_PATCH;
    }
    /**
     * 执行 stepType 相关逻辑。
     */
@Override
    public AgentStepType stepType() {
        return AgentStepType.GENERATE_PATCH;
    }
    /**
     * 执行 stepName 相关逻辑。
     */
@Override
    public String stepName() {
        return "生成 Patch";
    }
    /**
     * 执行 execute 相关逻辑。
     */
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

