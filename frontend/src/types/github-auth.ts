export interface GitHubAuthUrl {
  url: string;
}

export interface GitHubAccount {
  connected: boolean;
  githubLogin?: string | null;
  githubName?: string | null;
  githubAvatarUrl?: string | null;
  scope?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface GitHubConnectRequest {
  code: string;
  state: string;
}

export interface GitHubAuthorizedRepository {
  id: string;
  owner: string;
  name: string;
  fullName: string;
  privateRepo: boolean;
  defaultBranch?: string | null;
  htmlUrl: string;
  cloneUrl: string;
}
