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

/**
 * 补丁修复服务。
 * <p>
 * 当补丁验证失败时，通过 LLM 对补丁进行自动修复。
 * 支持多轮修复尝试（最多 {@code maxRepairAttempts} 次），每轮修复后重新验证补丁，
 * 直到验证通过或达到最大尝试次数。修复过程中会对补丁文本进行规范化和安全检查。
 * </p>
 */
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

    /**
     * 反复修复补丁直到验证通过或达到最大尝试次数。
     * <p>
     * 如果原始验证已通过或未配置修复尝试次数，则跳过修复。
     * 每次修复尝试包括：调用 LLM 生成修复补丁 -> 规范化补丁文本 ->
     * 安全检查 -> 保存补丁记录 -> 验证补丁。
     * </p>
     *
     * @param context Agent 执行上下文
     * @return 包含修复尝试详情的结果 Map，包括是否尝试过、尝试次数、各次尝试详情和最终验证结果
     */
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
