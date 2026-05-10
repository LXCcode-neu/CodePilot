export interface PatchRecord {
  id?: string | number | null;
  taskId?: string | number | null;
  analysis?: string | null;
  solution?: string | null;
  patch?: string | null;
  risk?: string | null;
  safetyCheckResult?: string | null;
  rawOutput?: string | null;
  confirmed?: boolean | null;
  confirmedAt?: string | null;
  prSubmitted?: boolean | null;
  prSubmittedAt?: string | null;
  prUrl?: string | null;
  prNumber?: number | null;
  prBranch?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
  fileChanges?: PatchFileChange[];
  pullRequest?: PullRequestPreview | null;
  raw?: unknown;
}

export interface PullRequestSubmitResult {
  taskId: string | number;
  number?: number | null;
  url?: string | null;
  branch: string;
  submittedAt: string;
}

export type PatchLineType = "added" | "removed" | "context";

export interface PatchDiffLine {
  type: PatchLineType | string;
  oldLineNumber?: number | null;
  newLineNumber?: number | null;
  content: string;
}

export interface PatchDiffHunk {
  header: string;
  oldStart?: number | null;
  oldLineCount?: number | null;
  newStart?: number | null;
  newLineCount?: number | null;
  lines: PatchDiffLine[];
}

export interface PatchFileChange {
  oldPath?: string | null;
  newPath?: string | null;
  filePath: string;
  addedLines: number;
  removedLines: number;
  hunks: PatchDiffHunk[];
}

export interface PullRequestPreview {
  title: string;
  branchName: string;
  commitMessage: string;
  body: string;
  changedFiles: number;
  addedLines: number;
  removedLines: number;
  touchedFiles: string[];
  ready: boolean;
  status: string;
}
