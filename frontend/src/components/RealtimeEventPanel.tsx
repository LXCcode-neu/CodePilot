import { AlertCircle, CheckCircle2, CircleDot, LoaderCircle, Radio } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ScrollArea } from "@/components/ui/scroll-area";
import { getStepTypeLabel } from "@/lib/task-display";
import { cn, formatDateTime } from "@/lib/utils";
import type { TaskEventMessage } from "@/types/common";

type StageState = "pending" | "active" | "done" | "failed";

interface StageViewModel {
  key: "STARTED" | "RUNNING" | "COMPLETED";
  title: string;
  state: StageState;
  message: string;
  time?: string;
}

const RUNNING_STATUSES = new Set([
  "CLONING",
  "INDEXING",
  "RETRIEVING",
  "ANALYZING",
  "GENERATING_PATCH",
  "WAITING_CONFIRM",
]);

const START_TITLE = "\u5f00\u59cb";
const RUNNING_TITLE = "\u6267\u884c\u4e2d";
const COMPLETED_TITLE = "\u5b8c\u6210";
const FAILED_TITLE = "\u5931\u8d25";
const PANEL_TITLE = "\u5b9e\u65f6\u8fdb\u5ea6";
const DETAIL_TITLE = "\u4e8b\u4ef6\u660e\u7ec6";
const EMPTY_EVENTS_MESSAGE =
  "SSE \u5efa\u7acb\u540e\uff0c\u8fd9\u91cc\u4f1a\u6301\u7eed\u5c55\u793a\u4efb\u52a1\u4e8b\u4ef6\u3002";

function getPhaseLabel(phase?: string) {
  if (phase === "STARTED") {
    return START_TITLE;
  }
  if (phase === "RUNNING") {
    return RUNNING_TITLE;
  }
  if (phase === "COMPLETED") {
    return COMPLETED_TITLE;
  }
  return "EVENT";
}

function getStatusMessage(taskStatus?: string | null, stepType?: string | null) {
  if (taskStatus === "WAITING_CONFIRM") {
    return "\u5df2\u751f\u6210 Patch\uff0c\u7b49\u5f85\u4eba\u5de5\u786e\u8ba4\u3002";
  }
  if (taskStatus === "COMPLETED") {
    return "\u4efb\u52a1\u5df2\u5b8c\u6210\u3002";
  }
  if (taskStatus === "FAILED") {
    return "\u4efb\u52a1\u6267\u884c\u5931\u8d25\uff0c\u8bf7\u67e5\u770b\u9519\u8bef\u4fe1\u606f\u3002";
  }
  if (stepType) {
    return `${getStepTypeLabel(stepType)}\u6b63\u5728\u6267\u884c\u3002`;
  }
  return "\u540e\u53f0\u6b63\u5728\u6267\u884c\u4efb\u52a1\u3002";
}

function getEventDisplayMessage(event?: TaskEventMessage) {
  if (!event) {
    return "";
  }

  if (event.phase === "STARTED") {
    return "\u5df2\u63d0\u4ea4 Agent \u8fd0\u884c\u8bf7\u6c42\u3002";
  }

  if (event.phase === "COMPLETED") {
    return getStatusMessage(event.status, event.stepType);
  }

  if (event.phase === "RUNNING") {
    return getStatusMessage(event.status, event.stepType);
  }

  return event.message;
}

function getEventDisplayTitle(event: TaskEventMessage) {
  if (event.stepType) {
    return getStepTypeLabel(event.stepType);
  }
  return getPhaseLabel(event.phase);
}

export function RealtimeEventPanel({
  events,
  taskStatus,
}: {
  events: TaskEventMessage[];
  taskStatus?: string | null;
}) {
  const startedEvent = events.find((event) => event.phase === "STARTED");
  const runningEvent = events.find((event) => event.phase === "RUNNING");
  const completedEvent = events.find((event) => event.phase === "COMPLETED");

  const isPending = (taskStatus ?? "PENDING") === "PENDING";
  const isFailed = taskStatus === "FAILED";
  const isCompleted = taskStatus === "COMPLETED" || taskStatus === "WAITING_CONFIRM";
  const isRunning = RUNNING_STATUSES.has(taskStatus ?? "");

  const stages: StageViewModel[] = [
    {
      key: "STARTED",
      title: START_TITLE,
      state: isPending ? "pending" : "done",
      message:
        getEventDisplayMessage(startedEvent) ||
        (isPending
          ? "\u4efb\u52a1\u5c1a\u672a\u5f00\u59cb\u3002"
          : "\u4efb\u52a1\u5df2\u5f00\u59cb\uff0c\u5df2\u8fdb\u5165\u672c\u6b21\u6267\u884c\u6d41\u7a0b\u3002"),
      time: startedEvent?.time,
    },
    {
      key: "RUNNING",
      title: RUNNING_TITLE,
      state: isFailed || isCompleted ? "done" : isRunning || runningEvent ? "active" : "pending",
      message:
        getEventDisplayMessage(runningEvent) ||
        (isPending
          ? "\u5f00\u59cb\u540e\u4f1a\u8fdb\u5165\u6267\u884c\u9636\u6bb5\u3002"
          : isRunning
            ? getStatusMessage(taskStatus)
            : "\u7b49\u5f85\u8fdb\u5165\u6267\u884c\u9636\u6bb5\u3002"),
      time: runningEvent?.time,
    },
    {
      key: "COMPLETED",
      title: isFailed ? FAILED_TITLE : COMPLETED_TITLE,
      state: isFailed ? "failed" : isCompleted ? "done" : "pending",
      message:
        getEventDisplayMessage(completedEvent) ||
        (isFailed
          ? "\u4efb\u52a1\u6267\u884c\u5931\u8d25\uff0c\u8bf7\u67e5\u770b\u9519\u8bef\u4fe1\u606f\u3002"
          : isCompleted
            ? getStatusMessage(taskStatus)
            : "\u6267\u884c\u5b8c\u6210\u540e\u4f1a\u5728\u8fd9\u91cc\u663e\u793a\u7ed3\u679c\u3002"),
      time: completedEvent?.time,
    },
  ];

  return (
    <Card className="h-full">
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-base">
          <Radio className="h-4 w-4 text-emerald-600" />
          {PANEL_TITLE}
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-5">
        <div className="space-y-3">
          {stages.map((stage, index) => (
            <div key={stage.key} className="flex gap-4">
              <div className="flex w-6 flex-col items-center">
                <StageIcon state={stage.state} />
                {index < stages.length - 1 ? (
                  <div
                    className={cn(
                      "mt-2 h-full min-h-10 w-px",
                      stage.state === "done" ? "bg-emerald-200" : "bg-slate-200"
                    )}
                  />
                ) : null}
              </div>
              <div className="flex-1 rounded-2xl bg-slate-50 px-4 py-3">
                <div className="flex items-center justify-between gap-3">
                  <p className="text-sm font-semibold text-slate-900">{stage.title}</p>
                  <p className="text-xs text-slate-400">{stage.time ? formatDateTime(stage.time) : "--"}</p>
                </div>
                <p className="mt-2 text-sm text-slate-600">{stage.message}</p>
              </div>
            </div>
          ))}
        </div>

        <div className="rounded-2xl border border-slate-100">
          <div className="border-b border-slate-100 px-4 py-3">
            <p className="text-xs uppercase tracking-wide text-slate-400">{DETAIL_TITLE}</p>
          </div>
          <ScrollArea className="h-[180px] px-4 py-3">
            <div className="space-y-3">
              {events.length ? (
                events.map((event) => (
                  <div key={event.id} className="rounded-2xl bg-slate-50 p-3">
                    <div className="flex items-center justify-between gap-3">
                      <p className="text-xs uppercase tracking-wide text-slate-400">
                        {getEventDisplayTitle(event)}
                      </p>
                      <p className="text-xs text-slate-400">{formatDateTime(event.time)}</p>
                    </div>
                    <p className="mt-2 text-sm text-slate-700">{getEventDisplayMessage(event)}</p>
                  </div>
                ))
              ) : (
                <div className="rounded-2xl bg-slate-50 p-4 text-sm text-slate-500">
                  {EMPTY_EVENTS_MESSAGE}
                </div>
              )}
            </div>
          </ScrollArea>
        </div>
      </CardContent>
    </Card>
  );
}

function StageIcon({ state }: { state: StageState }) {
  if (state === "done") {
    return <CheckCircle2 className="h-5 w-5 text-emerald-600" />;
  }
  if (state === "active") {
    return <LoaderCircle className="h-5 w-5 animate-spin text-blue-600" />;
  }
  if (state === "failed") {
    return <AlertCircle className="h-5 w-5 text-red-600" />;
  }
  return <CircleDot className="h-5 w-5 text-slate-300" />;
}
