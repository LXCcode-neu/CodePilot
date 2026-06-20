/** 通知渠道类型：飞书或企业微信 */
export type NotificationChannelType = "FEISHU" | "WE_COM";

/** 用户仓库监控配置 */
export interface UserRepoWatch {
  /** 监控记录唯一标识 */
  id: string;
  /** 关联的项目仓库 ID */
  projectRepoId?: string | null;
  /** 仓库所有者 */
  owner: string;
  /** 仓库名称 */
  repoName: string;
  /** 仓库地址 */
  repoUrl: string;
  /** 默认监控的分支 */
  defaultBranch?: string | null;
  /** 是否启用监控 */
  watchEnabled: boolean;
  /** 监控模式 */
  watchMode?: string | null;
  /** 最近一次检查时间 */
  lastCheckedAt?: string | null;
  /** 创建时间 */
  createdAt: string;
  /** 更新时间 */
  updatedAt: string;
}

/** 创建仓库监控请求参数 */
export interface CreateRepoWatchRequest {
  /** 仓库所有者 */
  owner: string;
  /** 仓库名称 */
  repoName: string;
  /** 仓库地址 */
  repoUrl: string;
  /** 默认监控的分支 */
  defaultBranch?: string;
}

/** 通知渠道配置信息 */
export interface NotificationChannel {
  /** 渠道唯一标识 */
  id: string;
  /** 渠道类型（飞书/企业微信） */
  channelType: NotificationChannelType;
  /** 渠道自定义名称 */
  channelName?: string | null;
  /** Webhook 地址（脱敏显示） */
  webhookMasked: string;
  /** 是否启用该渠道 */
  enabled: boolean;
  /** 创建时间 */
  createdAt: string;
  /** 更新时间 */
  updatedAt: string;
}

/** 创建通知渠道请求参数 */
export interface CreateNotificationChannelRequest {
  /** 渠道类型（飞书/企业微信） */
  channelType: NotificationChannelType;
  /** 渠道自定义名称 */
  channelName?: string;
  /** Webhook 完整地址 */
  webhookUrl: string;
}

/** 通知发送结果 */
export interface NotificationSendResult {
  /** 是否发送成功 */
  success: boolean;
  /** 发送结果消息 */
  message: string;
}

/** GitHub Issue 事件处理状态 */
export type GitHubIssueEventStatus =
  | "NEW"           // 新建
  | "NOTIFIED"      // 已通知
  | "IGNORED"       // 已忽略
  | "RUNNING"       // 正在处理
  | "PATCH_READY"   // 补丁已生成
  | "FAILED"        // 处理失败
  | "PR_CREATED";   // PR 已创建

/** GitHub Issue 自动化事件记录 */
export interface GitHubIssueEvent {
  /** 事件唯一标识 */
  id: string;
  /** 关联的仓库监控 ID */
  repoWatchId: string;
  /** 关联的项目仓库 ID */
  projectRepoId?: string | null;
  /** Issue 编号 */
  issueNumber: number;
  /** Issue 标题 */
  issueTitle: string;
  /** Issue 正文内容 */
  issueBody?: string | null;
  /** Issue 网页地址 */
  issueUrl?: string | null;
  /** Issue 当前状态（open/closed） */
  issueState: string;
  /** 事件触发者的 GitHub 登录名 */
  senderLogin?: string | null;
  /** 事件处理状态 */
  status: GitHubIssueEventStatus;
  /** 关联的 Agent 任务 ID */
  agentTaskId?: string | null;
  /** 通知发送时间 */
  notifiedAt?: string | null;
  /** 创建时间 */
  createdAt: string;
  /** 更新时间 */
  updatedAt: string;
}

/** GitHub Issue 事件手动触发执行结果 */
export interface GitHubIssueEventRunResult {
  /** 触发的任务 ID */
  taskId: string;
  /** 任务状态 */
  status: string;
}
