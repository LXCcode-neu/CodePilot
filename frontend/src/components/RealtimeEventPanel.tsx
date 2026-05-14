import { AlertCircle, CheckCircle2, CircleDot, LoaderCircle, Radio } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ScrollArea } from "@/components/ui/scroll-area";
import { getStepTypeLabel, isVisibleStepType } from "@/lib/task-display";
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
  "RETRIEVING",
  "ANALYZING",
  "GENERATING_PATCH",
  "VERIFYING",
  "REPAIRING_PATCH",
  "CANCEL_REQUESTED",
  "WAITING_CONFIRM",
]);

const START_TITLE = "开始";
const RUNNING_TITLE = "执行中";
const COMPLETED_TITLE = "完成";
const FAILED_TITLE = "失败";
const PANEL_TITLE = "实时进度";
const DETAIL_TITLE = "事件明细";
const EMPTY_EVENTS_MESSAGE = "SSE 建立后，这里会持续展示任务事件。";

function getPhaseLabel(phase?: string) {
  if (phase === "STARTED" || phase === "PENDING") {
    return START_TITLE;
  }
  if (phase === "RUNNING") {
    return RUNNING_TITLE;
  }
  if (phase === "COMPLETED") {
    return COMPLETED_TITLE;
  }
  return "事件";
}

function getStatusMessage(taskStatus?: string | null, stepType?: string | null) {
  if (taskStatus === "PENDING") {
    return "任务尚未开始。";
  }
  if (taskStatus === "WAITING_CONFIRM") {
    return "已生成 Patch，等待人工确认。";
  }
  if (taskStatus === "COMPLETED") {
    return "任务已完成。";
  }
  if (taskStatus === "CANCEL_REQUESTED") {
    return "Task cancellation requested. Waiting for the current step to stop.";
  }
  if (taskStatus === "CANCELLED") {
    return "Task cancelled.";
  }
  if (taskStatus === "FAILED") {
    return "任务执行失败，请查看错误信息。";
  }
  if (taskStatus === "VERIFY_FAILED") {
    return "Patch 自动验证失败，已阻止确认 PR。";
  }
  if (stepType) {
    return `${getStepTypeLabel(stepType)}正在执行。`;
  }
  return "后台正在执行任务。";
}

function getEventDisplayMessage(event?: TaskEventMessage) {
  if (!event) {
    return "";
  }

  return getStatusMessage(event.status, event.stepType);
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
  const visibleEvents = events.filter((event) => !event.stepType || isVisibleStepType(event.stepType));
  const startedEvent = visibleEvents.find((event) => event.phase === "STARTED" || event.phase === "PENDING");
  const runningEvent = [...visibleEvents].reverse().find((event) => event.phase === "RUNNING");
  const completedEvent = visibleEvents.find((event) => event.phase === "COMPLETED");

  const isPending = (taskStatus ?? "PENDING") === "PENDING";
  const isFailed = taskStatus === "FAILED" || taskStatus === "VERIFY_FAILED";
  const isCompleted = taskStatus === "COMPLETED" || taskStatus === "WAITING_CONFIRM" || taskStatus === "CANCELLED";
  const isRunning = RUNNING_STATUSES.has(taskStatus ?? "");

  const stages: StageViewModel[] = [
    {
      key: "STARTED",
      title: START_TITLE,
      state: isPending ? "pending" : "done",
      message:
        getEventDisplayMessage(startedEvent) ||
        (isPending ? "任务尚未开始。" : "任务已开始，已进入本次执行流程。"),
      time: startedEvent?.time,
    },
    {
      key: "RUNNING",
      title: RUNNING_TITLE,
      state: isFailed || isCompleted ? "done" : isRunning || runningEvent ? "active" : "pending",
      message:
        getEventDisplayMessage(runningEvent) ||
        (isPending ? "开始后会进入执行阶段。" : isRunning ? getStatusMessage(taskStatus) : "等待进入执行阶段。"),
      time: runningEvent?.time,
    },
    {
      key: "COMPLETED",
      title: isFailed ? FAILED_TITLE : COMPLETED_TITLE,
      state: isFailed ? "failed" : isCompleted ? "done" : "pending",
      message:
        getEventDisplayMessage(completedEvent) ||
        (isFailed ? "任务执行失败，请查看错误信息。" : isCompleted ? getStatusMessage(taskStatus) : "执行完成后会在这里显示结果。"),
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
                    className={cn("mt-2 h-full min-h-10 w-px", stage.state === "done" ? "bg-emerald-200" : "bg-slate-200")}
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
              {visibleEvents.length ? (
                visibleEvents.map((event) => (
                  <div key={event.id} className="rounded-2xl bg-slate-50 p-3">
                    <div className="flex items-center justify-between gap-3">
                      <p className="text-xs uppercase tracking-wide text-slate-400">{getEventDisplayTitle(event)}</p>
                      <p className="text-xs text-slate-400">{formatDateTime(event.time)}</p>
                    </div>
                    <p className="mt-2 text-sm text-slate-700">{getEventDisplayMessage(event)}</p>
                  </div>
                ))
              ) : (
                <div className="rounded-2xl bg-slate-50 p-4 text-sm text-slate-500">{EMPTY_EVENTS_MESSAGE}</div>
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
