import { useEffect, useMemo, useState } from "react";
import { useParams } from "react-router-dom";
import { LoaderCircle, PlayCircle, RefreshCcw } from "lucide-react";
import { confirmTaskPatch, getTaskPatch } from "@/api/patch";
import { getProject } from "@/api/project";
import { createTaskEventSource, parseTaskEventMessage } from "@/api/sse";
import { getTaskSteps } from "@/api/step";
import { getTask, runTask } from "@/api/task";
import { AgentStepTimeline } from "@/components/AgentStepTimeline";
import { EmptyState } from "@/components/EmptyState";
import { LoadingBlock } from "@/components/LoadingBlock";
import { PatchViewer } from "@/components/PatchViewer";
import { RealtimeEventPanel } from "@/components/RealtimeEventPanel";
import { StatusBadge } from "@/components/StatusBadge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { formatDateTime, isRunningTask } from "@/lib/utils";
import { getToken } from "@/lib/token";
import type { TaskEventMessage } from "@/types/common";
import type { PatchRecord } from "@/types/patch";
import type { ProjectRepo } from "@/types/project";
import type { AgentStep } from "@/types/step";
import type { AgentTask } from "@/types/task";

const INVALID_TASK_ID = "\u65e0\u6548\u7684\u4efb\u52a1 ID";
const LOAD_TASK_ERROR = "\u52a0\u8f7d\u4efb\u52a1\u8be6\u60c5\u5931\u8d25";
const RUN_TASK_ERROR = "\u8fd0\u884c Agent \u5931\u8d25";
const LOAD_FAILED = "\u52a0\u8f7d\u5931\u8d25";
const TASK_NOT_FOUND = "\u4efb\u52a1\u4e0d\u5b58\u5728";
const TASK_NOT_FOUND_DESC = "\u8bf7\u8fd4\u56de\u4efb\u52a1\u5217\u8868\u91cd\u65b0\u9009\u62e9\u4efb\u52a1\u3002";
const RUN_REQUESTED = "\u5df2\u63d0\u4ea4 Agent \u8fd0\u884c\u8bf7\u6c42\u3002";
const REFRESH_LABEL = "\u5237\u65b0";
const CONFIRM_LABEL = "\u786e\u8ba4 Patch";
const CONFIRMING_LABEL = "\u786e\u8ba4\u4e2d";
const CONFIRM_REQUESTED = "\u5df2\u786e\u8ba4 Patch\uff0c\u4efb\u52a1\u5df2\u5b8c\u6210\u3002";
const CONFIRM_ERROR = "\u786e\u8ba4 Patch \u5931\u8d25";
const TASK_INFO_TITLE = "\u4efb\u52a1\u4fe1\u606f";
const PROJECT_LABEL = "\u5173\u8054\u4ed3\u5e93";
const CREATED_AT_LABEL = "\u521b\u5efa\u65f6\u95f4";
const UPDATED_AT_LABEL = "\u6700\u8fd1\u66f4\u65b0\u65f6\u95f4";
const SUMMARY_LABEL = "\u7ed3\u679c\u6458\u8981";
const EMPTY_SUMMARY = "\u4efb\u52a1\u5c1a\u672a\u4ea7\u51fa\u6458\u8981\u3002";
const TIMELINE_TITLE = "\u6267\u884c\u8f68\u8ff9";

export function TaskDetailPage() {
  const params = useParams();
  const taskId = params.id ?? "";

  const [task, setTask] = useState<AgentTask | null>(null);
  const [project, setProject] = useState<ProjectRepo | null>(null);
  const [steps, setSteps] = useState<AgentStep[]>([]);
  const [patch, setPatch] = useState<PatchRecord | null>(null);
  const [events, setEvents] = useState<TaskEventMessage[]>([]);
  const [loading, setLoading] = useState(true);
  const [running, setRunning] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [error, setError] = useState("");

  async function loadTaskData() {
    const taskData = await getTask(taskId);
    setTask(taskData);

    try {
      const projectData = await getProject(taskData.projectId);
      setProject(projectData);
    } catch {
      setProject(null);
    }

    return taskData;
  }

  async function loadStepsData() {
    const stepData = await getTaskSteps(taskId);
    setSteps(stepData);
  }

  async function loadPatchData() {
    try {
      const patchData = await getTaskPatch(taskId);
      setPatch(patchData);
    } catch {
      setPatch(null);
    }
  }

  async function loadAll() {
    setError("");
    const taskData = await loadTaskData();
    await loadStepsData();
    if (taskData.status === "COMPLETED" || taskData.status === "WAITING_CONFIRM") {
      await loadPatchData();
    }
  }

  useEffect(() => {
    if (!taskId) {
      setLoading(false);
      setError(INVALID_TASK_ID);
      return;
    }

    loadAll()
      .catch((err) => setError(err instanceof Error ? err.message : LOAD_TASK_ERROR))
      .finally(() => setLoading(false));
  }, [taskId]);

  useEffect(() => {
    if (!task || !isRunningTask(task.status)) {
      return;
    }

    const timer = window.setInterval(() => {
      void loadAll().catch(() => undefined);
    }, 3000);

    return () => window.clearInterval(timer);
  }, [task?.status, taskId]);

  useEffect(() => {
    const token = getToken();
    if (!taskId || !token) {
      return;
    }

    let source: EventSource | null = null;

    try {
      source = createTaskEventSource(taskId, token);
      const handleTaskEvent = (event: MessageEvent<string>) => {
        const message = parseTaskEventMessage(event);
        setEvents((prev) => [message, ...prev].slice(0, 50));
        void loadAll().catch(() => undefined);
      };

      source.addEventListener("task-event", handleTaskEvent as EventListener);
      source.onmessage = handleTaskEvent;
      source.onerror = () => {
        source?.close();
      };
    } catch {
      source?.close();
    }

    return () => source?.close();
  }, [taskId]);

  const canRun = useMemo(() => task?.status === "PENDING" || task?.status === "FAILED", [task?.status]);
  const canConfirm = useMemo(
    () => task?.status === "WAITING_CONFIRM" && Boolean(patch) && !patch?.confirmed,
    [patch, task?.status]
  );

  async function handleRun() {
    if (!taskId) {
      return;
    }

    setRunning(true);
    setError("");
    try {
      await runTask(taskId);
      setEvents((prev) => [
        {
          id: crypto.randomUUID(),
          time: new Date().toISOString(),
          status: "RUN",
          phase: "STARTED",
          message: RUN_REQUESTED,
        },
        ...prev,
      ]);
      await loadAll();
    } catch (err) {
      setError(err instanceof Error ? err.message : RUN_TASK_ERROR);
    } finally {
      setRunning(false);
    }
  }

  async function handleConfirmPatch() {
    if (!taskId || !canConfirm) {
      return;
    }

    setConfirming(true);
    setError("");
    try {
      await confirmTaskPatch(taskId);
      setEvents((prev) => [
        {
          id: crypto.randomUUID(),
          time: new Date().toISOString(),
          status: "COMPLETED",
          phase: "COMPLETED",
          message: CONFIRM_REQUESTED,
        },
        ...prev,
      ]);
      await loadAll();
    } catch (err) {
      setError(err instanceof Error ? err.message : CONFIRM_ERROR);
    } finally {
      setConfirming(false);
    }
  }

  if (loading) {
    return <LoadingBlock lines={8} />;
  }

  if (error && !task) {
    return <EmptyState title={LOAD_FAILED} description={error} />;
  }

  if (!task) {
    return <EmptyState title={TASK_NOT_FOUND} description={TASK_NOT_FOUND_DESC} />;
  }

  return (
    <div className="space-y-8">
      <section className="flex flex-col gap-5 rounded-[28px] border border-border bg-slate-900 px-8 py-8 text-white lg:flex-row lg:items-end lg:justify-between">
        <div className="space-y-3">
          <p className="text-sm uppercase tracking-[0.24em] text-slate-300">Task Detail</p>
          <h1 className="text-3xl font-extrabold">{task.issueTitle}</h1>
          <p className="max-w-3xl text-sm leading-7 text-slate-300">{task.issueDescription}</p>
        </div>
        <div className="flex flex-wrap items-center gap-3">
          <StatusBadge status={task.status} />
          <Button variant="secondary" onClick={() => void loadAll()} disabled={running || confirming}>
            <RefreshCcw className="h-4 w-4" />
            {REFRESH_LABEL}
          </Button>
          {canConfirm ? (
            <Button variant="secondary" onClick={handleConfirmPatch} disabled={confirming || running}>
              {confirming ? <LoaderCircle className="h-4 w-4 animate-spin" /> : null}
              {confirming ? CONFIRMING_LABEL : CONFIRM_LABEL}
            </Button>
          ) : null}
          <Button onClick={handleRun} disabled={!canRun || running || confirming}>
            {running ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <PlayCircle className="h-4 w-4" />}
            Run Agent
          </Button>
        </div>
      </section>

      {task.errorMessage || error ? (
        <div className="rounded-2xl border border-red-100 bg-red-50 px-5 py-4 text-sm text-red-700">
          {task.errorMessage || error}
        </div>
      ) : null}

      <section className="grid gap-6 xl:grid-cols-[0.95fr_1.05fr]">
        <Card>
          <CardHeader>
            <CardTitle className="section-title">{TASK_INFO_TITLE}</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-4 text-sm text-slate-600">
            <div>
              <p className="text-xs uppercase tracking-wide text-slate-400">{PROJECT_LABEL}</p>
              <p className="mt-1 font-semibold text-slate-900">{project?.repoName || `Project #${task.projectId}`}</p>
              <p className="mt-1 break-all text-xs text-slate-500">{project?.repoUrl || "--"}</p>
            </div>
            <Separator />
            <div className="grid gap-3 md:grid-cols-2">
              <div>
                <p className="text-xs uppercase tracking-wide text-slate-400">{CREATED_AT_LABEL}</p>
                <p className="mt-1 text-slate-700">{formatDateTime(task.createdAt)}</p>
              </div>
              <div>
                <p className="text-xs uppercase tracking-wide text-slate-400">{UPDATED_AT_LABEL}</p>
                <p className="mt-1 text-slate-700">{formatDateTime(task.updatedAt)}</p>
              </div>
            </div>
            <Separator />
            <div>
              <p className="text-xs uppercase tracking-wide text-slate-400">{SUMMARY_LABEL}</p>
              <p className="mt-1 whitespace-pre-wrap leading-7 text-slate-700">
                {task.resultSummary || EMPTY_SUMMARY}
              </p>
            </div>
          </CardContent>
        </Card>

        <RealtimeEventPanel events={events} taskStatus={task.status} />
      </section>

      <section className="grid gap-6 xl:grid-cols-[1.05fr_0.95fr]">
        <div className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="section-title">{TIMELINE_TITLE}</CardTitle>
            </CardHeader>
            <CardContent>
              <AgentStepTimeline steps={steps} />
            </CardContent>
          </Card>
        </div>

        <PatchViewer patch={patch} />
      </section>
    </div>
  );
}
