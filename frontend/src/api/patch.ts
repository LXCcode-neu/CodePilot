import { request } from "@/api/request";
import { buildPullRequestPreview, parseUnifiedDiff } from "@/lib/patch-diff";
import type { PatchRecord, PatchReviewRecord, PullRequestSubmitResult } from "@/types/patch";

/**
 * 从记录对象中安全提取字符串字段
 * @param record - 数据记录对象
 * @param key - 要提取的字段名
 * @returns 返回字段值（字符串），字段不存在或非字符串时返回 null
 */
function pickString(record: Record<string, unknown>, key: string) {
  const value = record[key];
  return typeof value === "string" ? value : null;
}

/**
 * 将原始补丁数据标准化为统一的 PatchRecord 格式
 * 支持字符串、对象等多种后端返回格式的兼容处理
 * @param data - 原始补丁数据（可能是字符串、对象或 null）
 * @returns 返回标准化后的补丁记录对象
 */
function normalizePatch(data: unknown): PatchRecord {
  if (!data) {
    return {};
  }

  if (typeof data === "string") {
    return { patch: data, raw: data };
  }

  if (typeof data === "object") {
    const record = data as Record<string, unknown>;
    const patch = pickString(record, "patch") ?? pickString(record, "content");
    const fileChanges = Array.isArray(record.fileChanges) ? record.fileChanges : parseUnifiedDiff(patch);
    const pullRequest =
      record.pullRequest && typeof record.pullRequest === "object"
        ? (record.pullRequest as PatchRecord["pullRequest"])
        : buildPullRequestPreview(
            patch,
            pickString(record, "analysis"),
            pickString(record, "solution"),
            pickString(record, "risk") ?? pickString(record, "resultSummary"),
            typeof record.taskId === "string" || typeof record.taskId === "number" ? record.taskId : null
          );

    return {
      id: typeof record.id === "string" || typeof record.id === "number" ? record.id : null,
      taskId: typeof record.taskId === "string" || typeof record.taskId === "number" ? record.taskId : null,
      analysis: pickString(record, "analysis"),
      solution: pickString(record, "solution"),
      patch,
      risk: pickString(record, "risk") ?? pickString(record, "resultSummary"),
      safetyCheckResult: pickString(record, "safetyCheckResult"),
      rawOutput: pickString(record, "rawOutput"),
      confirmed: typeof record.confirmed === "boolean" ? record.confirmed : null,
      confirmedAt: pickString(record, "confirmedAt"),
      prSubmitted: typeof record.prSubmitted === "boolean" ? record.prSubmitted : null,
      prSubmittedAt: pickString(record, "prSubmittedAt"),
      prUrl: pickString(record, "prUrl"),
      prNumber: typeof record.prNumber === "number" ? record.prNumber : null,
      prBranch: pickString(record, "prBranch"),
      createdAt: pickString(record, "createdAt"),
      updatedAt: pickString(record, "updatedAt"),
      fileChanges,
      pullRequest,
      raw: data,
    };
  }

  return { raw: data };
}

/**
 * 获取指定任务的补丁（代码变更）
 * @param taskId - 任务 ID
 * @returns 返回标准化后的补丁记录
 */
export async function getTaskPatch(taskId: string) {
  const data = await request.get<unknown>(`/api/tasks/${taskId}/patch`);
  return normalizePatch(data);
}

/**
 * 获取指定任务的补丁评审记录
 * @param taskId - 任务 ID
 * @returns 返回补丁评审记录，无评审时返回 null
 */
export function getTaskPatchReview(taskId: string) {
  return request.get<PatchReviewRecord | null>(`/api/tasks/${taskId}/patch/review`);
}

/**
 * 确认任务的补丁（用户审批通过）
 * @param taskId - 任务 ID
 * @returns 返回确认结果
 */
export function confirmTaskPatch(taskId: string) {
  return request.post(`/api/tasks/${taskId}/confirm`);
}

/**
 * 提交任务的补丁为 Pull Request
 * @param taskId - 任务 ID
 * @returns 返回 PR 提交结果，包含 PR URL 等信息
 */
export function submitTaskPullRequest(taskId: string) {
  return request.post<PullRequestSubmitResult>(`/api/tasks/${taskId}/patch/pull-request`);
}
