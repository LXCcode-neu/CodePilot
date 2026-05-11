package com.codepliot.service.agent.tool;

import com.codepliot.entity.AgentStepType;
import com.codepliot.entity.AgentTaskStatus;
import com.codepliot.entity.PatchRecord;
import com.codepliot.exception.BusinessException;
import com.codepliot.model.AgentContext;
import com.codepliot.model.ErrorCode;
import com.codepliot.model.PatchGenerateResult;
import com.codepliot.model.PatchRecordVO;
import com.codepliot.model.PatchSafetyCheckResult;
import com.codepliot.policy.PatchSafetyPolicy;
import com.codepliot.service.agent.AgentTool;
import com.codepliot.service.agent.PatchTextNormalizer;
import com.codepliot.service.agent.ToolResult;
import com.codepliot.service.llm.LlmService;
import com.codepliot.service.llm.PatchPromptBuilder;
import com.codepliot.service.patch.PatchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Patch 生成工具。
 *
 * <p>负责调用 LLM 生成结构化 patch 结果，并在落库前执行安全检查。
 */
@Component
@Order(40)
public class GeneratePatchTool implements AgentTool {

    private final LlmService llmService;
    private final PatchPromptBuilder promptBuilder;
    private final PatchService patchService;
    private final PatchSafetyPolicy patchSafetyPolicy;
    private final PatchTextNormalizer patchTextNormalizer;
    private final ObjectMapper objectMapper;

    /**
     * 创建 Patch 生成工具。
     */
    public GeneratePatchTool(LlmService llmService,
                             PatchPromptBuilder promptBuilder,
                             PatchService patchService,
                             PatchSafetyPolicy patchSafetyPolicy,
                             PatchTextNormalizer patchTextNormalizer,
                             ObjectMapper objectMapper) {
        this.llmService = llmService;
        this.promptBuilder = promptBuilder;
        this.patchService = patchService;
        this.patchSafetyPolicy = patchSafetyPolicy;
        this.patchTextNormalizer = patchTextNormalizer;
        this.objectMapper = objectMapper;
    }

    /**
     * 返回当前步骤对应的任务状态。
     */
    @Override
    public AgentTaskStatus taskStatus() {
        return AgentTaskStatus.GENERATING_PATCH;
    }

    /**
     * 返回当前步骤类型。
     */
    @Override
    public AgentStepType stepType() {
        return AgentStepType.GENERATE_PATCH;
    }

    /**
     * 返回当前步骤名称。
     */
    @Override
    public String stepName() {
        return "生成 Patch";
    }

    /**
     * 执行 patch 生成、安全检查和落库。
     */
    @Override
    public ToolResult execute(AgentContext context) {
        String rawOutput = llmService.generate(
                context.llmRuntimeConfig(),
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
            result = new PatchGenerateResult(
                    result.analysis(),
                    result.solution(),
                    patchTextNormalizer.normalize(result.patch()),
                    result.risk()
            );
            PatchSafetyCheckResult safetyCheckResult = patchSafetyPolicy.evaluate(result.patch());
            PatchRecord patchRecord = patchService.saveGeneratedPatch(
                    context.taskId(),
                    result,
                    rawOutput,
                    safetyCheckResult
            );
            context.updatePatchGenerateResult(result);
            context.updatePatchSafetyCheckResult(safetyCheckResult);
            return ToolResult.success("patch generation completed", PatchRecordVO.from(patchRecord));
        } catch (IllegalArgumentException exception) {
            patchService.saveFailedPatch(context.taskId(), rawOutput, "Failed to parse LLM JSON output");
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to parse patch generation JSON output");
        }
    }
}
