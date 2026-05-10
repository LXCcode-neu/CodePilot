import { request } from "@/api/request";
import type { AgentTask } from "@/types/task";
import type { GitHubIssuePage, GitHubIssueState } from "@/types/github-issue";

export function getGitHubIssues(
  projectId: string,
  params: { state?: GitHubIssueState; page?: number; pageSize?: number }
) {
  return request.get<GitHubIssuePage>(`/api/projects/${projectId}/github/issues`, { params });
}

export function importGitHubIssueAsTask(projectId: string, issueNumber: number) {
  return request.post<AgentTask>(`/api/projects/${projectId}/github/issues/${issueNumber}/import-task`);
}
