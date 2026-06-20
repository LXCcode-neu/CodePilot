import type { TaskEventMessage } from "@/types/common";

/**
 * 创建任务的 SSE（Server-Sent Events）连接
 * 用于实时接收任务执行状态的推送消息
 * @param taskId - 任务 ID
 * @param token - 可选的认证 token，用于身份验证
 * @returns 返回 EventSource 实例，可用于监听服务端推送事件
 */
export function createTaskEventSource(taskId: string, token?: string | null) {
  const query = new URLSearchParams();
  if (token) {
    query.set("token", token);
  }

  const suffix = query.toString() ? `?${query}` : "";
  return new EventSource(`/api/tasks/${taskId}/events${suffix}`);
}

/**
 * 解析 SSE 原始消息为结构化的任务事件消息
 * 兼容多种后端消息格式，自动提取时间、状态、阶段、消息内容等字段
 * @param raw - SSE 原始 MessageEvent 对象
 * @returns 返回结构化的任务事件消息对象
 */
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
