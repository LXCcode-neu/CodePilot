import { request } from "@/api/request";
import type {
  GitHubAccount,
  GitHubAuthorizedRepository,
  GitHubAuthUrl,
  GitHubConnectRequest,
} from "@/types/github-auth";

export function getGitHubAuthUrl() {
  return request.get<GitHubAuthUrl>("/api/github/auth-url");
}

export function connectGitHubAccount(data: GitHubConnectRequest) {
  return request.post<GitHubAccount>("/api/github/callback", data);
}

export function getGitHubAccount() {
  return request.get<GitHubAccount>("/api/github/account");
}

export function disconnectGitHubAccount() {
  return request.delete<void>("/api/github/account");
}

export function getGitHubAuthorizedRepositories() {
  return request.get<GitHubAuthorizedRepository[]>("/api/github/repositories");
}
