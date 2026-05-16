package com.codepliot.service.agent;

import com.codepliot.config.PatchReviewProperties;
import com.codepliot.model.AgentContext;
import com.codepliot.model.PatchReviewResult;
import com.codepliot.service.patch.PatchReviewRecordService;
import com.codepliot.service.llm.LlmService;
import com.codepliot.service.llm.PatchReviewPromptBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class PatchReviewService {

    private final PatchReviewProperties properties;
    private final LlmService llmService;
    private final PatchReviewPromptBuilder promptBuilder;
    private final PatchReviewRecordService patchReviewRecordService;
    private final ObjectMapper objectMapper;

    public PatchReviewService(PatchReviewProperties properties,
                              LlmService llmService,
                              PatchReviewPromptBuilder promptBuilder,
                              PatchReviewRecordService patchReviewRecordService,
                              ObjectMapper objectMapper) {
        this.properties = properties;
        this.llmService = llmService;
        this.promptBuilder = promptBuilder;
        this.patchReviewRecordService = patchReviewRecordService;
        this.objectMapper = objectMapper;
    }

    public PatchReviewResult review(AgentContext context) {
        if (!properties.isEnabled()) {
            PatchReviewResult result = PatchReviewResult.skipped("AI 代码审查已关闭。");
            patchReviewRecordService.saveReviewResult(context.taskId(), context.patchRecordId(), context.llmRuntimeConfig(), result);
            return result;
        }
        if (context.patchVerificationResult() == null || !context.patchVerificationResult().passed()) {
            PatchReviewResult result = PatchReviewResult.skipped("自动验证未通过，已跳过 AI 代码审查。");
            patchReviewRecordService.saveReviewResult(context.taskId(), context.patchRecordId(), context.llmRuntimeConfig(), result);
            return result;
        }

        String rawOutput = llmService.generate(
                context.llmRuntimeConfig(),
                promptBuilder.buildSystemPrompt(),
                promptBuilder.buildUserPrompt(
                        context.issueTitle(),
                        context.issueDescription(),
                        context.analysis(),
                        context.retrievedChunks(),
                        context.patchGenerateResult(),
                        context.patchVerificationResult()
                )
        );

        PatchReviewResult parsed;
        try {
            parsed = PatchReviewResult.fromRawOutput(objectMapper, rawOutput);
        } catch (IllegalArgumentException exception) {
            parsed = PatchReviewResult.failed("AI 代码审查结果解析失败，已阻止 PR 确认。", rawOutput);
        }
        PatchReviewResult gated = applyGate(parsed);
        patchReviewRecordService.saveReviewResult(context.taskId(), context.patchRecordId(), context.llmRuntimeConfig(), gated);
        return gated;
    }

    private PatchReviewResult applyGate(PatchReviewResult result) {
        boolean highRiskBlocked = properties.isFailOnHighRisk() && "HIGH".equalsIgnoreCase(result.riskLevel());
        boolean scoreBlocked = result.score() < Math.max(0, Math.min(100, properties.getMinScore()));
        boolean passed = result.passed() && !highRiskBlocked && !scoreBlocked;
        if (passed == result.passed()) {
            return result;
        }
        String summary = result.summary();
        if (summary == null || summary.isBlank()) {
            summary = "AI 代码审查阻止了这个 Patch。";
        }
        if (scoreBlocked) {
            summary = summary + " 质量评分低于配置阈值。";
        }
        if (highRiskBlocked) {
            summary = summary + " 当前配置会阻止高风险 Patch。";
        }
        return new PatchReviewResult(
                result.skipped(),
                false,
                result.score(),
                result.riskLevel(),
                summary,
                result.findings(),
                result.recommendations(),
                result.rawResponse()
        );
    }
}
