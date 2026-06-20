/** GitHub Issue 筛选状态类型 */
export type GitHubIssueState = "open" | "closed" | "all";

/** GitHub Issue 信息 */
export interface GitHubIssue {
  /** Issue 唯一标识 */
  id: number;
  /** Issue 编号 */
  number: number;
  /** Issue 标题 */
  title: string;
  /** Issue 正文内容 */
  body?: string | null;
  /** Issue 状态：open 或 closed */
  state: "open" | "closed";
  /** Issue 网页地址 */
  htmlUrl: string;
  /** 作者登录名 */
  authorLogin?: string | null;
  /** 标签列表 */
  labels: string[];
  /** 评论数量 */
  comments: number;
  /** 创建时间 */
  createdAt?: string | null;
  /** 更新时间 */
  updatedAt?: string | null;
}

/** GitHub Issue 分页查询结果 */
export interface GitHubIssuePage {
  /** 当前页的 Issue 列表 */
  items: GitHubIssue[];
  /** 当前页码 */
  page: number;
  /** 每页条数 */
  pageSize: number;
  /** 总记录数 */
  totalCount: number;
  /** 总页数 */
  totalPages: number;
  /** 是否有上一页 */
  hasPrevious: boolean;
  /** 是否有下一页 */
  hasNext: boolean;
}
