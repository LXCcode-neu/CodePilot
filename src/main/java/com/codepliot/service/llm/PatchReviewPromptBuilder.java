package com.codepliot.service.llm;

import com.codepliot.model.PatchGenerateResult;
import com.codepliot.model.PatchVerificationCommandResult;
import com.codepliot.model.PatchVerificationResult;
import com.codepliot.model.RetrievedCodeChunk;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PatchReviewPromptBuilder {

    private static final int TEXT_LIMIT = 20000;

    public String buildSystemPrompt() {
        return """
                你是 CodePilot 内部的一名资深代码审查员。
                你需要在确定性验证通过后审查生成的 Patch。
                重点关注正确性、最小改动、可维护性、安全性和仓库约束。
                只能返回一个合法 JSON 对象，不要使用 Markdown 包裹。
                除 riskLevel 和 severity 这类枚举值必须使用英文枚举外，summary、findings.message、recommendations 必须使用简体中文。
                """;
    }

    public String buildUserPrompt(String issueTitle,
                                  String issueDescription,
                                  String analysis,
                                  List<RetrievedCodeChunk> retrievedChunks,
                                  PatchGenerateResult patchGenerateResult,
                                  PatchVerificationResult verificationResult) {
        StringBuilder builder = new StringBuilder();
        builder.append("请把这个 Patch 当作 AI 代码审查门槛进行审查。\n\n")
                .append("输出语言要求：\n")
                .append("- summary 必须使用简体中文。\n")
                .append("- findings 数组中每个 message 必须使用简体中文。\n")
                .append("- recommendations 数组中每一项必须使用简体中文。\n")
                .append("- riskLevel 只能使用 LOW、MEDIUM、HIGH。\n")
                .append("- findings.severity 只能使用 INFO、LOW、MEDIUM、HIGH、CRITICAL。\n\n")
                .append("决策规则：\n")
                .append("- 如果 Patch 很可能没有修复问题、引入高风险行为、泄露密钥、违反仓库架构或包含无关改动，passed 必须为 false。\n")
                .append("- score 必须是 0-100。\n")
                .append("- 需要人工注意但不阻塞确认的问题，riskLevel 使用 MEDIUM。\n")
                .append("- 应阻塞 PR 确认的问题，riskLevel 使用 HIGH。\n\n")
                .append("审查标准：\n")
                .append("1. 是否修复原始 Issue 或 Sentry 告警根因。\n")
                .append("2. Patch 是否最小、聚焦。\n")
                .append("3. 是否避免无关重构或大范围行为变化。\n")
                .append("4. 是否保留包结构、分层边界和现有风格。\n")
                .append("5. 是否没有引入不支持的依赖、框架、中间件或前端技术栈。\n")
                .append("6. 是否没有暴露 token、API key、webhook secret、OAuth token 或明文凭据。\n")
                .append("7. 是否没有削弱认证、加密、通知审批、验证或 PR 确认门槛。\n")
                .append("8. 是否处理明显的空值、并发、事务和边界风险。\n")
                .append("9. 命名和代码结构是否足够清晰。\n\n")
                .append("必须返回以下 JSON 结构：\n")
                .append("{\n")
                .append("  \"passed\": true,\n")
                .append("  \"score\": 86,\n")
                .append("  \"riskLevel\": \"LOW\",\n")
                .append("  \"summary\": \"Patch 改动聚焦，能够修复问题，未发现阻塞风险。\",\n")
                .append("  \"findings\": [{\"severity\":\"LOW\",\"filePath\":\"path/or/null\",\"message\":\"这里写中文问题描述\"}],\n")
                .append("  \"recommendations\": [\"这里写中文后续建议\"]\n")
                .append("}\n\n")
                .append("Issue title:\n").append(nullToEmpty(issueTitle)).append("\n\n")
                .append("Issue description:\n").append(truncate(issueDescription)).append("\n\n")
                .append("Prior analysis:\n").append(truncate(analysis)).append("\n\n")
                .append("Patch generation analysis:\n")
                .append(truncate(patchGenerateResult == null ? null : patchGenerateResult.analysis())).append("\n\n")
                .append("Patch solution:\n")
                .append(truncate(patchGenerateResult == null ? null : patchGenerateResult.solution())).append("\n\n")
                .append("Patch risk from generator:\n")
                .append(truncate(patchGenerateResult == null ? null : patchGenerateResult.risk())).append("\n\n")
                .append("Patch diff:\n```diff\n")
                .append(truncate(patchGenerateResult == null ? null : patchGenerateResult.patch()))
                .append("\n```\n\n")
                .append("Verification summary:\n")
                .append(verificationResult == null ? "" : nullToEmpty(verificationResult.summary())).append("\n\n")
                .append("Verification commands:\n");
        if (verificationResult != null) {
            for (PatchVerificationCommandResult command : verificationResult.commands()) {
                builder.append("- ").append(command.name())
                        .append(" passed=").append(command.passed())
                        .append(" exitCode=").append(command.exitCode())
                        .append(" timedOut=").append(command.timedOut())
                        .append("\n");
            }
        }
        builder.append("\nRelevant code snippets:\n");
        for (RetrievedCodeChunk chunk : safeChunks(retrievedChunks)) {
            builder.append("File: ").append(chunk.filePath()).append("\n")
                    .append("Lines: ").append(chunk.startLine()).append("-").append(chunk.endLine()).append("\n")
                    .append("```").append(chunk.language() == null ? "" : chunk.language().toLowerCase()).append("\n")
                    .append(truncate(chunk.content(), 5000))
                    .append("\n```\n\n");
        }
        return truncate(builder.toString(), 60000);
    }

    private List<RetrievedCodeChunk> safeChunks(List<RetrievedCodeChunk> chunks) {
        return chunks == null ? List.of() : chunks.stream().limit(12).toList();
    }

    private String truncate(String value) {
        return truncate(value, TEXT_LIMIT);
    }

    private String truncate(String value, int limit) {
        if (value == null) {
            return "";
        }
        return value.length() <= limit ? value : value.substring(0, limit);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
