import { CheckCircle2, Clock3, ExternalLink, GitPullRequest, LoaderCircle } from "lucide-react";
import { PatchDiffView, PullRequestPreviewCard } from "@/components/PatchDiffView";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { EmptyState } from "@/components/EmptyState";
import { buildPullRequestPreview, parseUnifiedDiff } from "@/lib/patch-diff";
import { formatDateTime } from "@/lib/utils";
import type { PatchRecord } from "@/types/patch";

const TAB_LABELS: Record<string, string> = {
  diff: "Diff",
  pr: "PR 草稿",
  analysis: "分析",
  solution: "方案",
  risk: "风险",
  raw: "原始 Patch",
};

function Section({ value }: { value?: string | null }) {
  if (!value) {
    return <p className="text-sm leading-7 text-slate-500">暂无内容</p>;
  }

  return <p className="whitespace-pre-wrap text-sm leading-7 text-slate-700">{value}</p>;
}

export function PatchViewer({
  patch,
  submittingPullRequest = false,
  onSubmitPullRequest,
}: {
  patch: PatchRecord | null;
  submittingPullRequest?: boolean;
  onSubmitPullRequest?: () => void;
}) {
  if (!patch || (!patch.analysis && !patch.solution && !patch.patch && !patch.risk)) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Patch 结果</CardTitle>
        </CardHeader>
        <CardContent>
          <EmptyState title="还没有 Patch 内容" description="任务进入 Patch 阶段后，这里会展示分析、方案、可读 diff 和 PR 草稿。" />
        </CardContent>
      </Card>
    );
  }

  const fileChanges = patch.fileChanges?.length ? patch.fileChanges : parseUnifiedDiff(patch.patch);
  const pullRequest =
    patch.pullRequest ?? buildPullRequestPreview(patch.patch, patch.analysis, patch.solution, patch.risk, patch.taskId);

  return (
    <Card>
      <CardHeader className="space-y-2">
        <div className="flex items-start justify-between gap-4">
          <CardTitle className="text-base">Patch 结果</CardTitle>
          <div className="flex items-center gap-2 rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-600">
            {patch.confirmed ? <CheckCircle2 className="h-4 w-4 text-emerald-600" /> : <Clock3 className="h-4 w-4 text-amber-600" />}
            {patch.confirmed ? `已确认${patch.confirmedAt ? ` · ${formatDateTime(patch.confirmedAt)}` : ""}` : "待确认"}
          </div>
        </div>
      </CardHeader>
      <CardContent className="space-y-5">
        <Tabs defaultValue="diff">
          <TabsList className="flex w-full flex-wrap gap-1 bg-transparent p-0">
            {["diff", "pr", "analysis", "solution", "risk", "raw"].map((tab) => (
              <TabsTrigger
                key={tab}
                value={tab}
                className="border border-border bg-slate-100 capitalize data-[state=active]:bg-slate-900 data-[state=active]:text-white"
              >
                {TAB_LABELS[tab] ?? tab}
              </TabsTrigger>
            ))}
          </TabsList>
          <TabsContent value="diff" className="mt-4">
            <PatchDiffView fileChanges={fileChanges} rawPatch={patch.patch} />
          </TabsContent>
          <TabsContent value="pr" className="mt-4">
            <PullRequestPreviewCard pullRequest={pullRequest} />
          </TabsContent>
          <TabsContent value="analysis" className="mt-4">
            <Section value={patch.analysis} />
          </TabsContent>
          <TabsContent value="solution" className="mt-4">
            <Section value={patch.solution} />
          </TabsContent>
          <TabsContent value="risk" className="mt-4">
            <Section value={patch.risk} />
          </TabsContent>
          <TabsContent value="raw" className="mt-4">
            <pre className="max-h-[520px] overflow-auto whitespace-pre rounded-2xl bg-slate-950 p-4 text-sm text-slate-100">
              {patch.patch || patch.rawOutput || "暂无 raw patch 内容"}
            </pre>
          </TabsContent>
        </Tabs>
        <div className="flex flex-col gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm font-semibold text-slate-900">提交 Pull Request</p>
            <p className="mt-1 text-sm text-slate-500">
              {patch.prSubmitted
                ? `PR 已提交${patch.prNumber ? ` #${patch.prNumber}` : ""}${patch.prSubmittedAt ? ` · ${formatDateTime(patch.prSubmittedAt)}` : ""}`
                : patch.confirmed
                  ? "Patch 已确认，可以提交 PR。"
                  : "确认 Patch 后可提交 PR。"}
            </p>
          </div>
          {patch.prSubmitted && patch.prUrl ? (
            <Button asChild variant="outline">
              <a href={patch.prUrl} target="_blank" rel="noreferrer">
                <ExternalLink className="h-4 w-4" />
                查看 PR
              </a>
            </Button>
          ) : (
            <Button
              type="button"
              onClick={onSubmitPullRequest}
              disabled={!patch.confirmed || submittingPullRequest || !pullRequest.ready}
            >
              {submittingPullRequest ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <GitPullRequest className="h-4 w-4" />}
              {patch.confirmed ? "提交 PR" : "确认后可提交 PR"}
            </Button>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
