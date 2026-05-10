export type GitHubIssueState = "open" | "closed" | "all";

export interface GitHubIssue {
  id: number;
  number: number;
  title: string;
  body?: string | null;
  state: "open" | "closed";
  htmlUrl: string;
  authorLogin?: string | null;
  labels: string[];
  comments: number;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface GitHubIssuePage {
  items: GitHubIssue[];
  page: number;
  pageSize: number;
  totalCount: number;
  totalPages: number;
  hasPrevious: boolean;
  hasNext: boolean;
}
