import { request } from "@/api/request";
import type {
  GitHubAccount,
  GitHubAuthorizedRepository,
  GitHubAuthUrl,
  GitHubConnectRequest,
} from "@/types/github-auth";

/**
 * 获取 GitHub OAuth 授权地址
 * @returns 返回包含授权 URL 的对象，用于跳转 GitHub 授权页面
 */
export function getGitHubAuthUrl() {
  return request.get<GitHubAuthUrl>("/api/github/auth-url");
}

/**
 * 关联 GitHub 账号（OAuth 回调）
 * @param data - 包含授权码等回调参数的请求数据
 * @returns 返回关联成功的 GitHub 账号信息
 */
export function connectGitHubAccount(data: GitHubConnectRequest) {
  return request.post<GitHubAccount>("/api/github/callback", data);
}

/**
 * 获取当前已关联的 GitHub 账号信息
 * @returns 返回 GitHub 账号详细信息
 */
export function getGitHubAccount() {
  return request.get<GitHubAccount>("/api/github/account");
}

/**
 * 取消关联 GitHub 账号
 * @returns 无返回值
 */
export function disconnectGitHubAccount() {
  return request.delete<void>("/api/github/account");
}

/**
 * 获取 GitHub 授权仓库列表
 * @returns 返回用户已授权的 GitHub 仓库列表
 */
export function getGitHubAuthorizedRepositories() {
  return request.get<GitHubAuthorizedRepository[]>("/api/github/repositories");
}
