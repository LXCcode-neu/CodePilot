import type { PatchDiffHunk, PatchDiffLine, PatchFileChange, PullRequestPreview } from "@/types/patch";

const HUNK_HEADER_PATTERN = /^@@\s+-(\d+)(?:,(\d+))?\s+\+(\d+)(?:,(\d+))?\s+@@/;

export function parseUnifiedDiff(patch?: string | null): PatchFileChange[] {
  if (!patch?.trim()) {
    return [];
  }

  const files: Array<Omit<PatchFileChange, "hunks"> & { hunks: PatchDiffHunk[] }> = [];
  let currentFile: (Omit<PatchFileChange, "hunks"> & { hunks: PatchDiffHunk[] }) | null = null;
  let currentHunk: PatchDiffHunk | null = null;
  let oldLine = 0;
  let newLine = 0;

  for (const line of patch.split(/\r?\n/)) {
    if (line.startsWith("--- ")) {
      currentFile = {
        oldPath: normalizeDiffPath(line.slice(4).trim()),
        newPath: "",
        filePath: "",
        addedLines: 0,
        removedLines: 0,
        hunks: [],
      };
      files.push(currentFile);
      currentHunk = null;
      continue;
    }

    if (line.startsWith("+++ ")) {
      if (!currentFile) {
        currentFile = { oldPath: "", newPath: "", filePath: "", addedLines: 0, removedLines: 0, hunks: [] };
        files.push(currentFile);
      }
      currentFile.newPath = normalizeDiffPath(line.slice(4).trim());
      currentFile.filePath = currentFile.newPath || currentFile.oldPath || "";
      continue;
    }

    const hunkMatch = line.match(HUNK_HEADER_PATTERN);
    if (hunkMatch) {
      if (!currentFile) {
        currentFile = { oldPath: "", newPath: "", filePath: "", addedLines: 0, removedLines: 0, hunks: [] };
        files.push(currentFile);
      }
      oldLine = Number(hunkMatch[1] ?? 0);
      newLine = Number(hunkMatch[3] ?? 0);
      currentHunk = {
        header: line,
        oldStart: oldLine,
        oldLineCount: Number(hunkMatch[2] ?? 1),
        newStart: newLine,
        newLineCount: Number(hunkMatch[4] ?? 1),
        lines: [],
      };
      currentFile.hunks.push(currentHunk);
      continue;
    }

    if (!currentFile || !currentHunk || line.startsWith("\\ No newline")) {
      continue;
    }

    if (line.startsWith("+")) {
      currentFile.addedLines += 1;
      currentHunk.lines.push({ type: "added", oldLineNumber: null, newLineNumber: newLine, content: line.slice(1) });
      newLine += 1;
    } else if (line.startsWith("-")) {
      currentFile.removedLines += 1;
      currentHunk.lines.push({ type: "removed", oldLineNumber: oldLine, newLineNumber: null, content: line.slice(1) });
      oldLine += 1;
    } else {
      const content = line.startsWith(" ") ? line.slice(1) : line;
      currentHunk.lines.push({ type: "context", oldLineNumber: oldLine, newLineNumber: newLine, content });
      oldLine += 1;
      newLine += 1;
    }
  }

  return files.filter((file) => Boolean(file.filePath));
}

export function buildPullRequestPreview(
  patch: string | null | undefined,
  analysis?: string | null,
  solution?: string | null,
  risk?: string | null,
  taskId?: string | number | null
): PullRequestPreview {
  const fileChanges = parseUnifiedDiff(patch);
  const addedLines = fileChanges.reduce((total, file) => total + file.addedLines, 0);
  const removedLines = fileChanges.reduce((total, file) => total + file.removedLines, 0);
  const touchedFiles = fileChanges.map((file) => file.filePath);
  const id = taskId ?? "unknown";
  const ready = Boolean(patch?.trim()) && fileChanges.length > 0;
  const title = firstSentence(solution, `CodePilot patch for task ${id}`);
  const body = [
    "## Summary",
    solution?.trim() || "No solution summary provided.",
    "",
    "## Analysis",
    analysis?.trim() || "No analysis provided.",
    "",
    "## Risk",
    risk?.trim() || "No risk notes provided.",
    "",
    "## Changes",
    `- Files changed: ${fileChanges.length}`,
    `- Added lines: ${addedLines}`,
    `- Removed lines: ${removedLines}`,
    ...touchedFiles.map((file) => `- \`${file}\``),
  ].join("\n");

  return {
    title,
    branchName: `codepilot/task-${id}`,
    commitMessage: firstSentence(solution, `Apply CodePilot patch for task ${id}`),
    body,
    changedFiles: fileChanges.length,
    addedLines,
    removedLines,
    touchedFiles,
    ready,
    status: ready
      ? "Patch is ready to submit as a GitHub pull request."
      : "Patch is empty or not a valid unified diff; PR preview is not ready.",
  };
}

function normalizeDiffPath(value: string) {
  if (!value || value === "/dev/null") {
    return "";
  }
  return value.replace(/\\/g, "/").replace(/^[ab]\//, "");
}

function firstSentence(value: string | null | undefined, fallback: string) {
  if (!value?.trim()) {
    return fallback;
  }
  const normalized = value.trim().replace(/\s+/g, " ");
  const match = normalized.match(/^(.{1,90}?)([。.]|$)/);
  return match?.[1] ? `${match[1]}${match[2] && match[2] !== "" ? match[2] : ""}` : normalized.slice(0, 90);
}
