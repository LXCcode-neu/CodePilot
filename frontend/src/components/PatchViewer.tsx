import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { EmptyState } from "@/components/EmptyState";
import { formatDateTime } from "@/lib/utils";
import type { PatchRecord } from "@/types/patch";

const EMPTY_CONTENT = "暂无内容";
const TITLE = "Patch 结果";
const EMPTY_PATCH_TITLE = "还没有 Patch 内容";
const EMPTY_PATCH_DESCRIPTION = "任务进入 Patch 阶段后，这里会展示分析、解法、diff 和风险说明。";
const EMPTY_PATCH_BODY = "暂无 patch 内容";
const CONFIRMED_LABEL = "已确认";
const UNCONFIRMED_LABEL = "待确认";

function Section({ value }: { value?: string | null }) {
  if (!value) {
    return <p className="text-sm leading-7 text-slate-500">{EMPTY_CONTENT}</p>;
  }

  return <p className="whitespace-pre-wrap text-sm leading-7 text-slate-700">{value}</p>;
}

export function PatchViewer({ patch }: { patch: PatchRecord | null }) {
  if (!patch || (!patch.analysis && !patch.solution && !patch.patch && !patch.risk)) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="text-base">{TITLE}</CardTitle>
        </CardHeader>
        <CardContent>
          <EmptyState title={EMPTY_PATCH_TITLE} description={EMPTY_PATCH_DESCRIPTION} />
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader className="space-y-2">
        <CardTitle className="text-base">{TITLE}</CardTitle>
        <div className="text-xs text-slate-500">
          {patch.confirmed
            ? `${CONFIRMED_LABEL}${patch.confirmedAt ? ` · ${formatDateTime(patch.confirmedAt)}` : ""}`
            : UNCONFIRMED_LABEL}
        </div>
      </CardHeader>
      <CardContent>
        <Tabs defaultValue="analysis">
          <TabsList className="flex w-full flex-wrap gap-1 bg-transparent p-0">
            <TabsTrigger
              value="analysis"
              className="border border-border bg-slate-100 data-[state=active]:bg-slate-900 data-[state=active]:text-white"
            >
              Analysis
            </TabsTrigger>
            <TabsTrigger
              value="solution"
              className="border border-border bg-slate-100 data-[state=active]:bg-slate-900 data-[state=active]:text-white"
            >
              Solution
            </TabsTrigger>
            <TabsTrigger
              value="patch"
              className="border border-border bg-slate-100 data-[state=active]:bg-slate-900 data-[state=active]:text-white"
            >
              Patch
            </TabsTrigger>
            <TabsTrigger
              value="risk"
              className="border border-border bg-slate-100 data-[state=active]:bg-slate-900 data-[state=active]:text-white"
            >
              Risk
            </TabsTrigger>
          </TabsList>
          <TabsContent value="analysis">
            <Section value={patch.analysis} />
          </TabsContent>
          <TabsContent value="solution">
            <Section value={patch.solution} />
          </TabsContent>
          <TabsContent value="patch">
            {patch.patch ? (
              <pre className="overflow-x-auto rounded-2xl bg-slate-950 p-4 text-sm text-slate-100">{patch.patch}</pre>
            ) : (
              <Section value={patch.risk || EMPTY_PATCH_BODY} />
            )}
          </TabsContent>
          <TabsContent value="risk">
            <Section value={patch.risk} />
          </TabsContent>
        </Tabs>
      </CardContent>
    </Card>
  );
}
