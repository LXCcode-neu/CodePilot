import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { EmptyState } from "@/components/EmptyState";
import type { PatchRecord } from "@/types/patch";

function Section({ value }: { value?: string | null }) {
  if (!value) {
    return <p className="text-sm leading-7 text-slate-500">暂无内容</p>;
  }

  return <p className="whitespace-pre-wrap text-sm leading-7 text-slate-700">{value}</p>;
}

export function PatchViewer({ patch }: { patch: PatchRecord | null }) {
  if (!patch || (!patch.analysis && !patch.solution && !patch.patch && !patch.risk)) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Patch 结果</CardTitle>
        </CardHeader>
        <CardContent>
          <EmptyState
            title="还没有 Patch 内容"
            description="任务完成后会自动拉取 Patch；如果没有 Patch，通常会在风险说明里给出原因。"
          />
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">Patch 结果</CardTitle>
      </CardHeader>
      <CardContent>
        <Tabs defaultValue="analysis">
          <TabsList className="flex w-full flex-wrap gap-1 bg-transparent p-0">
            <TabsTrigger value="analysis" className="border border-border bg-slate-100 data-[state=active]:bg-slate-900 data-[state=active]:text-white">
              Analysis
            </TabsTrigger>
            <TabsTrigger value="solution" className="border border-border bg-slate-100 data-[state=active]:bg-slate-900 data-[state=active]:text-white">
              Solution
            </TabsTrigger>
            <TabsTrigger value="patch" className="border border-border bg-slate-100 data-[state=active]:bg-slate-900 data-[state=active]:text-white">
              Patch
            </TabsTrigger>
            <TabsTrigger value="risk" className="border border-border bg-slate-100 data-[state=active]:bg-slate-900 data-[state=active]:text-white">
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
              <Section value={patch.risk || "暂无 patch 内容"} />
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
