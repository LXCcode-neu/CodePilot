import { request } from "@/api/request";
import { buildPullRequestPreview, parseUnifiedDiff } from "@/lib/patch-diff";
import type { PatchRecord } from "@/types/patch";

function pickString(record: Record<string, unknown>, key: string) {
  const value = record[key];
  return typeof value === "string" ? value : null;
}

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
      createdAt: pickString(record, "createdAt"),
      updatedAt: pickString(record, "updatedAt"),
      fileChanges,
      pullRequest,
      raw: data,
    };
  }

  return { raw: data };
}

export async function getTaskPatch(taskId: string) {
  const data = await request.get<unknown>(`/api/tasks/${taskId}/patch`);
  return normalizePatch(data);
}

export function confirmTaskPatch(taskId: string) {
  return request.post(`/api/tasks/${taskId}/confirm`);
}
