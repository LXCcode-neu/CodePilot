export interface ProjectRepo {
  id: string;
  userId: string;
  repoUrl: string;
  repoName: string;
  localPath?: string | null;
  defaultBranch?: string | null;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateProjectRequest {
  repoUrl: string;
}
