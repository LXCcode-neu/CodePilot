import { useMemo, useState } from "react";
import { ChevronRight, FileJson2 } from "lucide-react";
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
import { getStepDisplayName, getStepTypeLabel } from "@/lib/task-display";
import { formatDateTime, parseJsonString, stringifyPretty } from "@/lib/utils";
import type { AgentStep } from "@/types/step";

const EMPTY_TITLE = "\u8fd8\u6ca1\u6709\u6267\u884c\u8f68\u8ff9";
const EMPTY_DESCRIPTION =
  "\u8fd0\u884c Agent \u540e\uff0c\u8fd9\u91cc\u4f1a\u6309\u65f6\u95f4\u987a\u5e8f\u5c55\u793a\u6b65\u9aa4\u660e\u7ec6\u3002";
const START_TIME_LABEL = "\u5f00\u59cb\u65f6\u95f4";
const END_TIME_LABEL = "\u7ed3\u675f\u65f6\u95f4";
const VIEW_IO_LABEL = "\u67e5\u770b input / output";
const DIALOG_DESCRIPTION =
  "\u67e5\u770b\u5f53\u524d\u6b65\u9aa4\u7684\u8f93\u5165\u8f93\u51fa\u548c\u5931\u8d25\u4fe1\u606f\u3002";
const CURRENT_STEP_LABEL = "\u5f53\u524d\u6b65\u9aa4";
const EMPTY_INPUT = "\u65e0\u8f93\u5165\u5185\u5bb9";
const EMPTY_OUTPUT = "\u65e0\u8f93\u51fa\u5185\u5bb9";

export function AgentStepTimeline({ steps }: { steps: AgentStep[] }) {
  const [activeStep, setActiveStep] = useState<AgentStep | null>(null);

  const details = useMemo(() => {
    if (!activeStep) {
      return { input: "", output: "" };
    }

    return {
      input: stringifyPretty(parseJsonString(activeStep.input) ?? activeStep.input ?? ""),
      output: stringifyPretty(parseJsonString(activeStep.output) ?? activeStep.output ?? ""),
    };
  }, [activeStep]);

  if (!steps.length) {
    return <EmptyState title={EMPTY_TITLE} description={EMPTY_DESCRIPTION} />;
  }

  return (
    <>
      <div className="space-y-4">
        {steps.map((step, index) => (
          <Card key={step.id} className="shadow-none">
            <CardHeader className="flex-row items-start justify-between gap-4 space-y-0">
              <div className="space-y-2">
                <div className="flex items-center gap-3 text-xs uppercase tracking-wide text-slate-400">
                  <span>Step {index + 1}</span>
                  <span>{step.stepType}</span>
                </div>
                <CardTitle className="text-base">
                  {getStepDisplayName(step.stepType, step.stepName)}
                </CardTitle>
              </div>
              <StatusBadge status={step.status} />
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid gap-3 text-sm text-slate-500 md:grid-cols-2">
                <p>
                  {START_TIME_LABEL}: {formatDateTime(step.startTime)}
                </p>
                <p>
                  {END_TIME_LABEL}: {formatDateTime(step.endTime)}
                </p>
              </div>
              {step.errorMessage ? (
                <div className="rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-sm text-red-700">
                  {step.errorMessage}
                </div>
              ) : null}
              <Separator />
              <Button variant="ghost" size="sm" onClick={() => setActiveStep(step)}>
                <FileJson2 className="h-4 w-4" />
                {VIEW_IO_LABEL}
                <ChevronRight className="h-4 w-4" />
              </Button>
            </CardContent>
          </Card>
        ))}
      </div>

      <Dialog open={Boolean(activeStep)} onOpenChange={(open) => !open && setActiveStep(null)}>
        <DialogContent className="max-w-4xl">
          <DialogHeader>
            <DialogTitle>{getStepDisplayName(activeStep?.stepType, activeStep?.stepName)}</DialogTitle>
            <DialogDescription>
              {DIALOG_DESCRIPTION}
              {activeStep?.stepType
                ? ` ${CURRENT_STEP_LABEL}: ${getStepTypeLabel(activeStep.stepType)}`
                : ""}
            </DialogDescription>
          </DialogHeader>
          <Tabs defaultValue="input">
            <TabsList>
              <TabsTrigger value="input">Input</TabsTrigger>
              <TabsTrigger value="output">Output</TabsTrigger>
            </TabsList>
            <TabsContent value="input">
              <ScrollArea className="h-72 rounded-2xl border border-border bg-slate-950 p-4 text-sm text-slate-100">
                <pre className="whitespace-pre-wrap break-all">{details.input || EMPTY_INPUT}</pre>
              </ScrollArea>
            </TabsContent>
            <TabsContent value="output">
              <ScrollArea className="h-72 rounded-2xl border border-border bg-slate-950 p-4 text-sm text-slate-100">
                <pre className="whitespace-pre-wrap break-all">{details.output || EMPTY_OUTPUT}</pre>
              </ScrollArea>
            </TabsContent>
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
