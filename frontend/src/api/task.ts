import { request } from "@/api/request";
import type { AgentTask, CreateTaskRequest } from "@/types/task";

/**
 * 创建新的 Agent 任务
 * @param data - 创建任务的请求参数
 * @returns 返回新创建的任务信息
 */
export function createTask(data: CreateTaskRequest) {
  return request.post<AgentTask>("/api/tasks", data);
}

/**
 * 获取所有任务列表
 * @returns 返回任务列表
 */
export function getTasks() {
  return request.get<AgentTask[]>("/api/tasks");
}

/**
 * 获取指定任务的详细信息
 * @param id - 任务 ID
 * @returns 返回任务详细信息
 */
export function getTask(id: string) {
  return request.get<AgentTask>(`/api/tasks/${id}`);
}

/**
 * 删除指定任务
 * @param id - 任务 ID
 * @returns 无返回值
 */
export function deleteTask(id: string) {
  return request.delete<void>(`/api/tasks/${id}`);
}

/**
 * 执行指定任务（启动 Agent 运行）
 * @param id - 任务 ID
 * @returns 返回任务执行后的状态信息
 */
export function runTask(id: string) {
  return request.post<AgentTask>(`/api/tasks/${id}/run`);
}

/**
 * 取消正在执行的任务
 * @param id - 任务 ID
 * @returns 返回任务取消后的状态信息
 */
export function cancelTask(id: string) {
  return request.post<AgentTask>(`/api/tasks/${id}/cancel`);
}
