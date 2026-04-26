import { request } from "@/api/request";
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
    return {
      analysis: pickString(record, "analysis"),
      solution: pickString(record, "solution"),
      patch: pickString(record, "patch") ?? pickString(record, "content"),
      risk: pickString(record, "risk") ?? pickString(record, "resultSummary"),
      raw: data,
    };
  }

  return { raw: data };
}

export async function getTaskPatch(taskId: string) {
  const data = await request.get<unknown>(`/api/tasks/${taskId}/patch`);
  return normalizePatch(data);
}
