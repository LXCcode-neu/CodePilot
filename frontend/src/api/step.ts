import { request } from "@/api/request";
import type { AgentStep } from "@/types/step";

/**
 * 获取指定任务的所有执行步骤
 * @param taskId - 任务 ID
 * @returns 返回任务的步骤列表
 */
export function getTaskSteps(taskId: string) {
  return request.get<AgentStep[]>(`/api/tasks/${taskId}/steps`);
}
