import { request } from "@/api/request";
import type { CreateProjectRequest, ImportGitHubRepoRequest, ProjectRepo } from "@/types/project";

export function createProject(data: CreateProjectRequest) {
  return request.post<ProjectRepo>("/api/projects", data);
}

export function getProjects() {
  return request.get<ProjectRepo[]>("/api/projects");
}

export function getProject(id: string) {
  return request.get<ProjectRepo>(`/api/projects/${id}`);
}

export function deleteProject(id: string) {
  return request.delete<void>(`/api/projects/${id}`);
}

export function importGitHubProject(data: ImportGitHubRepoRequest) {
  return request.post<ProjectRepo>("/api/projects/import-github-repo", data);
}
