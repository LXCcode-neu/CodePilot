/** GitHub OAuth 授权地址 */
export interface GitHubAuthUrl {
  /** GitHub OAuth 授权跳转 URL */
  url: string;
}

/** 已连接的 GitHub 账户信息 */
export interface GitHubAccount {
  /** 是否已连接 GitHub 账户 */
  connected: boolean;
  /** GitHub 用户登录名 */
  githubLogin?: string | null;
  /** GitHub 用户显示名称 */
  githubName?: string | null;
  /** GitHub 用户头像 URL */
  githubAvatarUrl?: string | null;
  /** OAuth 授权范围 */
  scope?: string | null;
  /** 连接时间 */
  createdAt?: string | null;
  /** 更新时间 */
  updatedAt?: string | null;
}

/** GitHub OAuth 回调连接请求参数 */
export interface GitHubConnectRequest {
  /** OAuth 授权码 */
  code: string;
  /** OAuth 状态参数，用于防 CSRF */
  state: string;
}

/** GitHub 已授权的仓库信息 */
export interface GitHubAuthorizedRepository {
  /** 仓库唯一标识 */
  id: string;
  /** 仓库所有者 */
  owner: string;
  /** 仓库名称 */
  name: string;
  /** 仓库完整名称（owner/name） */
  fullName: string;
  /** 是否为私有仓库 */
  privateRepo: boolean;
  /** 默认分支名称 */
  defaultBranch?: string | null;
  /** 仓库网页地址 */
  htmlUrl: string;
  /** 仓库克隆地址 */
  cloneUrl: string;
}
