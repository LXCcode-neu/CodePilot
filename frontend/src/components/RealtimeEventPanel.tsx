import { AlertCircle, CheckCircle2, CircleDot, LoaderCircle, Radio } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ScrollArea } from "@/components/ui/scroll-area";
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
]);

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
  const isCompleted = taskStatus === "COMPLETED";
  const isRunning = RUNNING_STATUSES.has(taskStatus ?? "");

  const stages: StageViewModel[] = [
    {
      key: "STARTED",
      title: "开始",
      state: isPending ? "pending" : "done",
      message:
        startedEvent?.message ??
        (isPending ? "任务尚未开始。" : "任务已开始，已经进入本次执行流程。"),
      time: startedEvent?.time,
    },
    {
      key: "RUNNING",
      title: "执行中",
      state: isFailed || isCompleted ? "done" : isRunning || runningEvent ? "active" : "pending",
      message:
        runningEvent?.message ??
        (isPending ? "开始后会进入执行阶段。" : isRunning ? "后台正在执行任务步骤。" : "等待进入执行阶段。"),
      time: runningEvent?.time,
    },
    {
      key: "COMPLETED",
      title: isFailed ? "失败" : "完成",
      state: isFailed ? "failed" : isCompleted ? "done" : "pending",
      message:
        completedEvent?.message ??
        (isFailed ? "任务执行失败，请查看错误信息。" : isCompleted ? "任务已完成。" : "执行完成后会在这里显示结果。"),
      time: completedEvent?.time,
    },
  ];

  return (
    <Card className="h-full">
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-base">
          <Radio className="h-4 w-4 text-emerald-600" />
          实时进度
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
            <p className="text-xs uppercase tracking-wide text-slate-400">事件明细</p>
          </div>
          <ScrollArea className="h-[180px] px-4 py-3">
            <div className="space-y-3">
              {events.length ? (
                events.map((event) => (
                  <div key={event.id} className="rounded-2xl bg-slate-50 p-3">
                    <div className="flex items-center justify-between gap-3">
                      <p className="text-xs uppercase tracking-wide text-slate-400">
                        {event.stepType || event.phase || event.status || "EVENT"}
                      </p>
                      <p className="text-xs text-slate-400">{formatDateTime(event.time)}</p>
                    </div>
                    <p className="mt-2 text-sm text-slate-700">{event.message}</p>
                  </div>
                ))
              ) : (
                <div className="rounded-2xl bg-slate-50 p-4 text-sm text-slate-500">
                  SSE 建立后，这里会持续展示任务事件。
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
