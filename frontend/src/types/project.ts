/** 项目仓库信息 */
export interface ProjectRepo {
  /** 项目唯一标识 */
  id: string;
  /** 所属用户 ID */
  userId: string;
  /** 仓库地址 */
  repoUrl: string;
  /** 仓库名称 */
  repoName: string;
  /** GitHub 仓库所有者 */
  githubOwner?: string | null;
  /** GitHub 仓库名称 */
  githubRepoName?: string | null;
  /** GitHub 仓库 ID */
  githubRepoId?: string | null;
  /** 是否为 GitHub 私有仓库 */
  githubPrivate?: boolean | null;
  /** 本地克隆路径 */
  localPath?: string | null;
  /** 默认分支名称 */
  defaultBranch?: string | null;
  /** 项目状态 */
  status: string;
  /** 创建时间 */
  createdAt: string;
  /** 更新时间 */
  updatedAt: string;
}

/** 通过仓库地址创建项目请求参数 */
export interface CreateProjectRequest {
  /** Git 仓库地址 */
  repoUrl: string;
}

/** 从 GitHub 导入仓库请求参数 */
export interface ImportGitHubRepoRequest {
  /** GitHub 仓库 ID */
  githubRepoId: string;
  /** 仓库所有者 */
  owner: string;
  /** 仓库名称 */
  repoName: string;
}
