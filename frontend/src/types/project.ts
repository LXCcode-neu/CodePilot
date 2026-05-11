export interface ProjectRepo {
  id: string;
  userId: string;
  repoUrl: string;
  repoName: string;
  githubOwner?: string | null;
  githubRepoName?: string | null;
  githubRepoId?: string | null;
  githubPrivate?: boolean | null;
  localPath?: string | null;
  defaultBranch?: string | null;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateProjectRequest {
  repoUrl: string;
}

export interface ImportGitHubRepoRequest {
  githubRepoId: string;
  owner: string;
  repoName: string;
}
