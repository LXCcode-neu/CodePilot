import { request } from "@/api/request";
import type { AgentStep } from "@/types/step";

export function getTaskSteps(taskId: string) {
  return request.get<AgentStep[]>(`/api/tasks/${taskId}/steps`);
}
