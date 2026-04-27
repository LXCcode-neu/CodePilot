import type { TaskEventMessage } from "@/types/common";

export function createTaskEventSource(taskId: string, token?: string | null) {
  const query = new URLSearchParams();
  if (token) {
    query.set("token", token);
  }

  const suffix = query.toString() ? `?${query}` : "";
  return new EventSource(`/api/tasks/${taskId}/events${suffix}`);
}

export function parseTaskEventMessage(raw: MessageEvent<string>): TaskEventMessage {
  try {
    const payload = JSON.parse(raw.data) as Record<string, unknown>;
    return {
      id: crypto.randomUUID(),
      time:
        typeof payload.timestamp === "string"
          ? payload.timestamp
          : typeof payload.time === "string"
            ? payload.time
            : new Date().toISOString(),
      status:
        typeof payload.taskStatus === "string"
          ? payload.taskStatus
          : typeof payload.status === "string"
            ? payload.status
            : undefined,
      phase:
        typeof payload.phase === "string"
          ? payload.phase
          : undefined,
      stepType:
        typeof payload.stepType === "string"
          ? payload.stepType
          : undefined,
      message:
        typeof payload.message === "string"
          ? payload.message
          : typeof payload.event === "string"
            ? payload.event
            : raw.data,
      payload,
    };
  } catch {
    return {
      id: crypto.randomUUID(),
      time: new Date().toISOString(),
      message: raw.data,
    };
  }
}
