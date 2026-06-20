import { request } from "@/api/request";
import type { AgentTask } from "@/types/task";
import type { GitHubIssuePage, GitHubIssueState } from "@/types/github-issue";

/**
 * 获取项目的 GitHub Issue 列表（分页）
 * @param projectId - 项目 ID
 * @param params - 查询参数
 * @param params.state - Issue 状态筛选（open/closed 等）
 * @param params.page - 页码
 * @param params.pageSize - 每页数量
 * @returns 返回分页的 GitHub Issue 列表
 */
export function getGitHubIssues(
  projectId: string,
  params: { state?: GitHubIssueState; page?: number; pageSize?: number }
) {
  return request.get<GitHubIssuePage>(`/api/projects/${projectId}/github/issues`, { params });
}

/**
 * 将 GitHub Issue 导入为 Agent 任务
 * @param projectId - 项目 ID
 * @param issueNumber - GitHub Issue 编号
 * @returns 返回导入后创建的 Agent 任务
 */
export function importGitHubIssueAsTask(projectId: string, issueNumber: number) {
  return request.post<AgentTask>(`/api/projects/${projectId}/github/issues/${issueNumber}/import-task`);
}
