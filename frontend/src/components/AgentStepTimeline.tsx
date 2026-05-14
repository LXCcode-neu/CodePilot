import { useMemo, useState } from "react";
import { ChevronRight, FileJson2 } from "lucide-react";
import { PatchDiffView, PullRequestPreviewCard } from "@/components/PatchDiffView";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Separator } from "@/components/ui/separator";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { StatusBadge } from "@/components/StatusBadge";
import { EmptyState } from "@/components/EmptyState";
import { buildPullRequestPreview, parseUnifiedDiff } from "@/lib/patch-diff";
import { getStepDisplayName, getStepTypeLabel, isVisibleStepType } from "@/lib/task-display";
import { formatDateTime, parseJsonString, stringifyPretty } from "@/lib/utils";
import type { PatchFileChange, PullRequestPreview } from "@/types/patch";
import type { AgentStep } from "@/types/step";

type StepOutput = {
  taskId?: string | number;
  stepType?: string;
  success?: boolean;
  message?: string;
  data?: Record<string, unknown>;
};

export function AgentStepTimeline({ steps }: { steps: AgentStep[] }) {
  const [activeStep, setActiveStep] = useState<AgentStep | null>(null);
  const visibleSteps = useMemo(() => steps.filter((step) => isVisibleStepType(step.stepType)), [steps]);

  const details = useMemo(() => {
    if (!activeStep) {
      return { input: "", output: "", parsedOutput: null as StepOutput | null };
    }

    const parsedOutput = parseJsonString<StepOutput>(activeStep.output);
    return {
      input: stringifyPretty(parseJsonString(activeStep.input) ?? activeStep.input ?? ""),
      output: stringifyPretty(parsedOutput ?? activeStep.output ?? ""),
      parsedOutput,
    };
  }, [activeStep]);

  if (!visibleSteps.length) {
    return <EmptyState title="还没有执行轨迹" description="运行 Agent 后，这里会按时间顺序展示步骤明细。" />;
  }

  return (
    <>
      <div className="space-y-4">
        {visibleSteps.map((step, index) => (
          <Card key={step.id} className="shadow-none">
            <CardHeader className="flex-row items-start justify-between gap-4 space-y-0">
              <div className="space-y-2">
                <div className="flex items-center gap-3 text-xs uppercase tracking-wide text-slate-400">
                  <span>步骤 {index + 1}</span>
                  <span>{step.stepType}</span>
                </div>
                <CardTitle className="text-base">{getStepDisplayName(step.stepType, step.stepName)}</CardTitle>
              </div>
              <StatusBadge status={step.status} />
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid gap-3 text-sm text-slate-500 md:grid-cols-2">
                <p>开始时间: {formatDateTime(step.startTime)}</p>
                <p>结束时间: {formatDateTime(step.endTime)}</p>
              </div>
              {step.errorMessage ? (
                <div className="rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-sm text-red-700">
                  {step.errorMessage}
                </div>
              ) : null}
              <Separator />
              <Button variant="ghost" size="sm" onClick={() => setActiveStep(step)}>
                <FileJson2 className="h-4 w-4" />
                查看输入 / 输出
                <ChevronRight className="h-4 w-4" />
              </Button>
            </CardContent>
          </Card>
        ))}
      </div>

      <Dialog open={Boolean(activeStep)} onOpenChange={(open) => !open && setActiveStep(null)}>
        <DialogContent className="max-w-6xl">
          <DialogHeader>
            <DialogTitle>{getStepDisplayName(activeStep?.stepType, activeStep?.stepName)}</DialogTitle>
            <DialogDescription>
              查看当前步骤的输入、输出和失败信息。
              {activeStep?.stepType ? ` 当前步骤: ${getStepTypeLabel(activeStep.stepType)}` : ""}
            </DialogDescription>
          </DialogHeader>
          <Tabs defaultValue="input">
            <TabsList>
              <TabsTrigger value="input">输入</TabsTrigger>
              <TabsTrigger value="output">输出</TabsTrigger>
              {isPatchOutput(activeStep, details.parsedOutput) ? <TabsTrigger value="diff">可读 Diff</TabsTrigger> : null}
              {isPatchOutput(activeStep, details.parsedOutput) ? <TabsTrigger value="pr">PR 草稿</TabsTrigger> : null}
            </TabsList>
            <TabsContent value="input">
              <ScrollArea className="h-72 rounded-2xl border border-border bg-slate-950 p-4 text-sm text-slate-100">
                <pre className="whitespace-pre-wrap break-words">{details.input || "无输入内容"}</pre>
              </ScrollArea>
            </TabsContent>
            <TabsContent value="output">
              <ScrollArea className="h-72 rounded-2xl border border-border bg-slate-950 p-4 text-sm text-slate-100">
                <pre className="whitespace-pre-wrap break-words">{details.output || "无输出内容"}</pre>
              </ScrollArea>
            </TabsContent>
            {isPatchOutput(activeStep, details.parsedOutput) ? (
              <TabsContent value="diff">
                <PatchDiffView fileChanges={extractFileChanges(details.parsedOutput)} rawPatch={extractPatch(details.parsedOutput)} />
              </TabsContent>
            ) : null}
            {isPatchOutput(activeStep, details.parsedOutput) ? (
              <TabsContent value="pr">
                <PullRequestPreviewCard pullRequest={extractPullRequest(details.parsedOutput)} />
              </TabsContent>
            ) : null}
          </Tabs>
          {activeStep?.errorMessage ? (
            <div className="rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-sm text-red-700">
              {activeStep.errorMessage}
            </div>
          ) : null}
        </DialogContent>
      </Dialog>
    </>
  );
}

function isPatchOutput(step: AgentStep | null, output: StepOutput | null) {
  return step?.stepType === "GENERATE_PATCH" && Boolean(output?.data);
}

function extractPatch(output: StepOutput | null) {
  const value = output?.data?.patch;
  return typeof value === "string" ? value : "";
}

function extractFileChanges(output: StepOutput | null): PatchFileChange[] {
  const value = output?.data?.fileChanges;
  if (Array.isArray(value)) {
    return value as PatchFileChange[];
  }
  return parseUnifiedDiff(extractPatch(output));
}

function extractPullRequest(output: StepOutput | null): PullRequestPreview {
  const value = output?.data?.pullRequest;
  if (value && typeof value === "object") {
    return value as PullRequestPreview;
  }
  const data = output?.data;
  return buildPullRequestPreview(
    extractPatch(output),
    typeof data?.analysis === "string" ? data.analysis : "",
    typeof data?.solution === "string" ? data.solution : "",
    typeof data?.risk === "string" ? data.risk : "",
    typeof data?.taskId === "string" || typeof data?.taskId === "number" ? data.taskId : output?.taskId
  );
}
