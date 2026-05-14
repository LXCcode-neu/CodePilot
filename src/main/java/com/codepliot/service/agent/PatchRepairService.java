package com.codepliot.service.agent;

import com.codepliot.config.PatchVerificationProperties;
import com.codepliot.entity.PatchRecord;
import com.codepliot.model.AgentContext;
import com.codepliot.model.PatchGenerateResult;
import com.codepliot.model.PatchRecordVO;
import com.codepliot.model.PatchSafetyCheckResult;
import com.codepliot.model.PatchVerificationResult;
import com.codepliot.policy.PatchSafetyPolicy;
import com.codepliot.service.llm.LlmService;
import com.codepliot.service.llm.PatchPromptBuilder;
import com.codepliot.service.patch.PatchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PatchRepairService {

    private final LlmService llmService;
    private final PatchPromptBuilder promptBuilder;
    private final PatchService patchService;
    private final PatchSafetyPolicy patchSafetyPolicy;
    private final PatchTextNormalizer patchTextNormalizer;
    private final PatchVerificationService patchVerificationService;
    private final PatchVerificationProperties properties;
    private final ObjectMapper objectMapper;

    public PatchRepairService(LlmService llmService,
                              PatchPromptBuilder promptBuilder,
                              PatchService patchService,
                              PatchSafetyPolicy patchSafetyPolicy,
                              PatchTextNormalizer patchTextNormalizer,
                              PatchVerificationService patchVerificationService,
                              PatchVerificationProperties properties,
                              ObjectMapper objectMapper) {
        this.llmService = llmService;
        this.promptBuilder = promptBuilder;
        this.patchService = patchService;
        this.patchSafetyPolicy = patchSafetyPolicy;
        this.patchTextNormalizer = patchTextNormalizer;
        this.patchVerificationService = patchVerificationService;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> repairUntilVerified(AgentContext context) {
        PatchVerificationResult verification = context.patchVerificationResult();
        int maxAttempts = Math.max(properties.getMaxRepairAttempts(), 0);
        List<Map<String, Object>> attempts = new ArrayList<>();
        if (verification == null || verification.passed() || maxAttempts == 0) {
            return resultMap(false, attempts, verification);
        }

        for (int attemptNo = 1; attemptNo <= maxAttempts; attemptNo++) {
            Map<String, Object> attempt = new LinkedHashMap<>();
            attempt.put("attemptNo", attemptNo);
            attempts.add(attempt);

            String rawOutput = llmService.generate(
                    context.llmRuntimeConfig(),
                    promptBuilder.buildSystemPrompt(),
                    promptBuilder.buildRepairUserPrompt(
                            context.issueTitle(),
                            context.issueDescription(),
                            context.analysis(),
                            context.retrievedChunks(),
                            context.patchGenerateResult() == null ? "" : context.patchGenerateResult().patch(),
                            verification,
                            attemptNo,
                            maxAttempts
                    )
            );

            try {
                PatchGenerateResult repaired = PatchGenerateResult.fromRawOutput(objectMapper, rawOutput);
                repaired = new PatchGenerateResult(
                        repaired.analysis(),
                        repaired.solution(),
                        patchTextNormalizer.normalize(repaired.patch()),
                        repaired.risk()
                );
                PatchSafetyCheckResult safetyCheckResult = patchSafetyPolicy.evaluate(repaired.patch());
                PatchRecord patchRecord = patchService.saveGeneratedPatch(
                        context.taskId(),
                        repaired,
                        rawOutput,
                        safetyCheckResult
                );
                context.updatePatchGenerateResult(repaired);
                context.updatePatchSafetyCheckResult(safetyCheckResult);
                context.updatePatchRecordId(patchRecord.getId());

                verification = patchVerificationService.verify(
                        context.localPath(),
                        context.taskId(),
                        patchRecord.getId(),
                        repaired.patch()
                );
                context.updatePatchVerificationResult(verification);

                attempt.put("parsed", true);
                attempt.put("patchRecord", PatchRecordVO.from(patchRecord));
                attempt.put("verification", verification);
                if (verification.passed()) {
                    return resultMap(true, attempts, verification);
                }
            } catch (IllegalArgumentException exception) {
                attempt.put("parsed", false);
                attempt.put("error", "Failed to parse repair JSON output");
            }
        }
        return resultMap(true, attempts, context.patchVerificationResult());
    }

    private Map<String, Object> resultMap(boolean attempted,
                                          List<Map<String, Object>> attempts,
                                          PatchVerificationResult finalVerification) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("attempted", attempted);
        result.put("attemptCount", attempts.size());
        result.put("attempts", attempts);
        result.put("finalVerification", finalVerification);
        return result;
    }
}
