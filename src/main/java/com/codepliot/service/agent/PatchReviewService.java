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
            PatchReviewResult result = PatchReviewResult.skipped("AI patch review is disabled.");
            patchReviewRecordService.saveReviewResult(context.taskId(), context.patchRecordId(), context.llmRuntimeConfig(), result);
            return result;
        }
        if (context.patchVerificationResult() == null || !context.patchVerificationResult().passed()) {
            PatchReviewResult result = PatchReviewResult.skipped("AI patch review skipped because automatic verification did not pass.");
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
            parsed = PatchReviewResult.failed("AI patch review response could not be parsed. PR confirmation is blocked.", rawOutput);
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
            summary = "AI patch review blocked this patch.";
        }
        if (scoreBlocked) {
            summary = summary + " Score is below the configured threshold.";
        }
        if (highRiskBlocked) {
            summary = summary + " High risk patches are blocked by configuration.";
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
