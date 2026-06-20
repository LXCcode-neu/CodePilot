import { request } from "@/api/request";
import type { CreateProjectRequest, ImportGitHubRepoRequest, ProjectRepo } from "@/types/project";

/**
 * 创建新项目
 * @param data - 创建项目的请求参数
 * @returns 返回新创建的项目信息
 */
export function createProject(data: CreateProjectRequest) {
  return request.post<ProjectRepo>("/api/projects", data);
}

/**
 * 获取所有项目列表
 * @returns 返回项目列表
 */
export function getProjects() {
  return request.get<ProjectRepo[]>("/api/projects");
}

/**
 * 获取指定项目的详细信息
 * @param id - 项目 ID
 * @returns 返回项目详细信息
 */
export function getProject(id: string) {
  return request.get<ProjectRepo>(`/api/projects/${id}`);
}

/**
 * 删除指定项目
 * @param id - 项目 ID
 * @returns 无返回值
 */
export function deleteProject(id: string) {
  return request.delete<void>(`/api/projects/${id}`);
}

/**
 * 从 GitHub 仓库导入项目
 * @param data - 导入 GitHub 仓库的请求参数
 * @returns 返回导入后的项目信息
 */
export function importGitHubProject(data: ImportGitHubRepoRequest) {
  return request.post<ProjectRepo>("/api/projects/import-github-repo", data);
}
