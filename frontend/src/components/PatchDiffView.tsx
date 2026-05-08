import { Copy, FileCode2, GitPullRequest, Plus, ScrollText } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ScrollArea } from "@/components/ui/scroll-area";
import { cn } from "@/lib/utils";
import type { PatchFileChange, PullRequestPreview } from "@/types/patch";

export function PatchStats({ fileChanges }: { fileChanges: PatchFileChange[] }) {
  const added = fileChanges.reduce((total, file) => total + file.addedLines, 0);
  const removed = fileChanges.reduce((total, file) => total + file.removedLines, 0);

  return (
    <div className="flex flex-wrap gap-2 text-xs">
      <Badge variant="outline">{fileChanges.length} files</Badge>
      <Badge className="border-emerald-200 bg-emerald-50 text-emerald-700" variant="outline">
        +{added}
      </Badge>
      <Badge className="border-rose-200 bg-rose-50 text-rose-700" variant="outline">
        -{removed}
      </Badge>
    </div>
  );
}

export function PatchDiffView({ fileChanges, rawPatch }: { fileChanges: PatchFileChange[]; rawPatch?: string | null }) {
  if (!fileChanges.length) {
    return (
      <div className="rounded-2xl border border-dashed border-slate-200 bg-slate-50 p-5 text-sm text-slate-500">
        No structured diff was found. The raw patch is still available below.
        {rawPatch ? (
          <pre className="mt-4 max-h-72 overflow-auto rounded-xl bg-slate-950 p-4 text-xs text-slate-100">
            {rawPatch}
          </pre>
        ) : null}
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <PatchStats fileChanges={fileChanges} />
      {fileChanges.map((file) => (
        <Card key={file.filePath} className="overflow-hidden border-slate-200 shadow-none">
          <CardHeader className="border-b bg-slate-50 px-4 py-3">
            <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
              <CardTitle className="flex items-center gap-2 break-all text-sm font-semibold text-slate-800">
                <FileCode2 className="h-4 w-4 shrink-0 text-slate-500" />
                {file.filePath}
              </CardTitle>
              <div className="flex gap-2 text-xs">
                <span className="rounded-full bg-emerald-50 px-2 py-1 font-semibold text-emerald-700">
                  +{file.addedLines}
                </span>
                <span className="rounded-full bg-rose-50 px-2 py-1 font-semibold text-rose-700">
                  -{file.removedLines}
                </span>
              </div>
            </div>
          </CardHeader>
          <CardContent className="p-0">
            <ScrollArea className="max-h-[520px]">
              <div className="min-w-[780px] font-mono text-[12px] leading-5">
                {file.hunks.map((hunk, hunkIndex) => (
                  <div key={`${file.filePath}-${hunk.header}-${hunkIndex}`}>
                    <div className="border-y border-sky-100 bg-sky-50 px-4 py-2 text-xs font-semibold text-sky-700">
                      {hunk.header}
                    </div>
                    {hunk.lines.map((line, lineIndex) => (
                      <div
                        key={`${hunk.header}-${lineIndex}`}
                        className={cn(
                          "grid grid-cols-[64px_64px_32px_1fr] border-b border-slate-100",
                          line.type === "added" && "bg-emerald-50/80 text-emerald-950",
                          line.type === "removed" && "bg-rose-50/80 text-rose-950",
                          line.type !== "added" && line.type !== "removed" && "bg-white text-slate-700"
                        )}
                      >
                        <span className="select-none border-r border-slate-100 px-3 py-1 text-right text-slate-400">
                          {line.oldLineNumber ?? ""}
                        </span>
                        <span className="select-none border-r border-slate-100 px-3 py-1 text-right text-slate-400">
                          {line.newLineNumber ?? ""}
                        </span>
                        <span className="select-none px-3 py-1 font-bold">
                          {line.type === "added" ? "+" : line.type === "removed" ? "-" : ""}
                        </span>
                        <code className="whitespace-pre px-2 py-1">{line.content || " "}</code>
                      </div>
                    ))}
                  </div>
                ))}
              </div>
            </ScrollArea>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}

export function PullRequestPreviewCard({ pullRequest }: { pullRequest?: PullRequestPreview | null }) {
  if (!pullRequest) {
    return (
      <div className="rounded-2xl border border-dashed border-slate-200 bg-slate-50 p-5 text-sm text-slate-500">
        No PR preview is available yet.
      </div>
    );
  }

  return (
    <Card className="overflow-hidden border-slate-200 shadow-none">
      <CardHeader className="space-y-3 bg-gradient-to-br from-slate-950 to-slate-800 text-white">
        <div className="flex items-start justify-between gap-4">
          <div className="space-y-2">
            <CardTitle className="flex items-center gap-2 text-base">
              <GitPullRequest className="h-5 w-5 text-emerald-300" />
              {pullRequest.title}
            </CardTitle>
            <p className="text-xs leading-6 text-slate-300">{pullRequest.status}</p>
          </div>
          <Badge className={pullRequest.ready ? "bg-emerald-500 text-white" : "bg-amber-500 text-white"}>
            {pullRequest.ready ? "Ready draft" : "Not ready"}
          </Badge>
        </div>
        <div className="grid gap-2 text-xs text-slate-200 md:grid-cols-3">
          <span className="rounded-xl bg-white/10 px-3 py-2">Branch: {pullRequest.branchName}</span>
          <span className="rounded-xl bg-white/10 px-3 py-2">{pullRequest.changedFiles} files</span>
          <span className="rounded-xl bg-white/10 px-3 py-2">
            +{pullRequest.addedLines} / -{pullRequest.removedLines}
          </span>
        </div>
      </CardHeader>
      <CardContent className="space-y-4 p-4">
        <div>
          <p className="mb-2 flex items-center gap-2 text-xs font-semibold uppercase tracking-wide text-slate-400">
            <Plus className="h-3.5 w-3.5" />
            Commit message
          </p>
          <div className="rounded-xl bg-slate-100 px-3 py-2 font-mono text-sm text-slate-800">
            {pullRequest.commitMessage}
          </div>
        </div>
        <div>
          <p className="mb-2 flex items-center gap-2 text-xs font-semibold uppercase tracking-wide text-slate-400">
            <ScrollText className="h-3.5 w-3.5" />
            PR body
          </p>
          <pre className="max-h-80 overflow-auto whitespace-pre-wrap rounded-xl border border-slate-200 bg-white p-4 text-sm leading-6 text-slate-700">
            {pullRequest.body}
          </pre>
        </div>
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={() => void navigator.clipboard?.writeText(`${pullRequest.title}\n\n${pullRequest.body}`)}
        >
          <Copy className="h-4 w-4" />
          Copy PR draft
        </Button>
      </CardContent>
    </Card>
  );
}
