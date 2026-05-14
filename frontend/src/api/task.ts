import { request } from "@/api/request";
import type { AgentTask, CreateTaskRequest } from "@/types/task";

export function createTask(data: CreateTaskRequest) {
  return request.post<AgentTask>("/api/tasks", data);
}

export function getTasks() {
  return request.get<AgentTask[]>("/api/tasks");
}

export function getTask(id: string) {
  return request.get<AgentTask>(`/api/tasks/${id}`);
}

export function deleteTask(id: string) {
  return request.delete<void>(`/api/tasks/${id}`);
}

export function runTask(id: string) {
  return request.post<AgentTask>(`/api/tasks/${id}/run`);
}

export function cancelTask(id: string) {
  return request.post<AgentTask>(`/api/tasks/${id}/cancel`);
}
