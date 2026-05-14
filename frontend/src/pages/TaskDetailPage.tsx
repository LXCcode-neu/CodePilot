import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { LoaderCircle, PlayCircle, RefreshCcw, StopCircle, Trash2 } from "lucide-react";
import { confirmTaskPatch, getTaskPatch, submitTaskPullRequest } from "@/api/patch";
import { getProject } from "@/api/project";
import { createTaskEventSource, parseTaskEventMessage } from "@/api/sse";
import { getTaskSteps } from "@/api/step";
import { cancelTask, deleteTask, getTask, runTask } from "@/api/task";
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

const INVALID_TASK_ID = "无效的任务 ID";
const LOAD_TASK_ERROR = "加载任务详情失败";
const RUN_TASK_ERROR = "运行 Agent 失败";
const CANCEL_TASK_ERROR = "取消任务失败";
const CANCEL_REQUESTED = "已提交取消任务请求。";
const LOAD_FAILED = "加载失败";
const TASK_NOT_FOUND = "任务不存在";
const TASK_NOT_FOUND_DESC = "请返回任务列表重新选择任务。";
const RUN_REQUESTED = "已提交 Agent 运行请求。";
const REFRESH_LABEL = "刷新";
const CONFIRM_LABEL = "确认 Patch";
const CONFIRMING_LABEL = "确认中";
const CONFIRM_REQUESTED = "已确认 Patch，任务已完成。";
const CONFIRM_ERROR = "确认 Patch 失败";
const PR_SUBMIT_REQUESTED = "Pull Request 已提交。";
const PR_SUBMIT_ERROR = "提交 Pull Request 失败";
const DELETE_LABEL = "删除任务";
const DELETING_LABEL = "删除中";
const DELETE_CONFIRM_MESSAGE = "确定删除这个任务吗？删除后对应的执行步骤和 Patch 记录也会一起删除。";
const DELETE_ERROR = "删除任务失败";
const TASK_INFO_TITLE = "任务信息";
const PROJECT_LABEL = "关联仓库";
const MODEL_LABEL = "模型";
const CREATED_AT_LABEL = "创建时间";
const UPDATED_AT_LABEL = "最近更新时间";
const SUMMARY_LABEL = "结果摘要";
const EMPTY_SUMMARY = "任务尚未产出摘要。";
const TIMELINE_TITLE = "执行轨迹";

export function TaskDetailPage() {
  const navigate = useNavigate();
  const params = useParams();
  const taskId = params.id ?? "";

  const [task, setTask] = useState<AgentTask | null>(null);
  const [project, setProject] = useState<ProjectRepo | null>(null);
  const [steps, setSteps] = useState<AgentStep[]>([]);
  const [patch, setPatch] = useState<PatchRecord | null>(null);
  const [events, setEvents] = useState<TaskEventMessage[]>([]);
  const [eventStreamVersion, setEventStreamVersion] = useState(0);
  const [loading, setLoading] = useState(true);
  const [running, setRunning] = useState(false);
  const [cancelling, setCancelling] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [submittingPullRequest, setSubmittingPullRequest] = useState(false);
  const [deleting, setDeleting] = useState(false);
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
    if (taskData.status === "COMPLETED" || taskData.status === "WAITING_CONFIRM" || taskData.status === "VERIFY_FAILED") {
      await loadPatchData();
      return;
    }
    setPatch(null);
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
  }, [taskId, eventStreamVersion]);

  useEffect(() => {
    if (!task || !isRunningTask(task.status)) {
      return;
    }

    const timer = window.setInterval(() => {
      void loadAll().catch(() => undefined);
    }, 3000);

    return () => window.clearInterval(timer);
  }, [task, taskId]);

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

  const canRun = useMemo(
    () => task?.status === "PENDING" || task?.status === "FAILED" || task?.status === "VERIFY_FAILED" || task?.status === "CANCELLED",
    [task?.status]
  );
  const canCancel = useMemo(
    () => Boolean(task && isRunningTask(task.status) && task.status !== "CANCEL_REQUESTED"),
    [task]
  );
  const canConfirm = useMemo(
    () => task?.status === "WAITING_CONFIRM" && Boolean(patch) && !patch?.confirmed,
    [patch, task?.status]
  );
  const canDelete = useMemo(() => (task ? !isRunningTask(task.status) : false), [task]);

  async function handleRun() {
    if (!taskId) {
      return;
    }

    setRunning(true);
    setError("");
    try {
      await runTask(taskId);
      setEventStreamVersion((value) => value + 1);
      setEvents([
        {
          id: crypto.randomUUID(),
          time: new Date().toISOString(),
          status: "RUN",
          phase: "STARTED",
          message: RUN_REQUESTED,
        },
      ]);
      await loadAll();
    } catch (err) {
      setError(err instanceof Error ? err.message : RUN_TASK_ERROR);
    } finally {
      setRunning(false);
    }
  }

  async function handleCancel() {
    if (!taskId || !canCancel) {
      return;
    }

    setCancelling(true);
    setError("");
    try {
      await cancelTask(taskId);
      setEvents((prev) => [
        {
          id: crypto.randomUUID(),
          time: new Date().toISOString(),
          status: "CANCEL_REQUESTED",
          phase: "RUNNING",
          message: CANCEL_REQUESTED,
        },
        ...prev,
      ]);
      await loadAll();
    } catch (err) {
      setError(err instanceof Error ? err.message : CANCEL_TASK_ERROR);
    } finally {
      setCancelling(false);
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

  async function handleSubmitPullRequest() {
    if (!taskId || !patch?.confirmed || patch.prSubmitted) {
      return;
    }

    setSubmittingPullRequest(true);
    setError("");
    try {
      await submitTaskPullRequest(taskId);
      setEvents((prev) => [
        {
          id: crypto.randomUUID(),
          time: new Date().toISOString(),
          status: task?.status ?? "COMPLETED",
          phase: "COMPLETED",
          message: PR_SUBMIT_REQUESTED,
        },
        ...prev,
      ]);
      await loadPatchData();
    } catch (err) {
      setError(err instanceof Error ? err.message : PR_SUBMIT_ERROR);
    } finally {
      setSubmittingPullRequest(false);
    }
  }

  async function handleDelete() {
    if (!taskId || !task || !canDelete) {
      return;
    }
    if (!window.confirm(DELETE_CONFIRM_MESSAGE)) {
      return;
    }

    setDeleting(true);
    setError("");
    try {
      await deleteTask(taskId);
      navigate("/tasks");
    } catch (err) {
      setError(err instanceof Error ? err.message : DELETE_ERROR);
    } finally {
      setDeleting(false);
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
          <p className="text-sm uppercase tracking-[0.24em] text-slate-300">任务详情</p>
          <h1 className="text-3xl font-extrabold">{task.issueTitle}</h1>
          <p className="max-w-3xl text-sm leading-7 text-slate-300">{task.issueDescription}</p>
        </div>
        <div className="flex flex-wrap items-center gap-3">
          <StatusBadge status={task.status} />
          <Button variant="secondary" onClick={() => void loadAll()} disabled={running || cancelling || confirming || submittingPullRequest || deleting}>
            <RefreshCcw className="h-4 w-4" />
            {REFRESH_LABEL}
          </Button>
          {canConfirm ? (
            <Button variant="secondary" onClick={handleConfirmPatch} disabled={confirming || running || cancelling || submittingPullRequest || deleting}>
              {confirming ? <LoaderCircle className="h-4 w-4 animate-spin" /> : null}
              {confirming ? CONFIRMING_LABEL : CONFIRM_LABEL}
            </Button>
          ) : null}
          {canCancel ? (
            <Button variant="destructive" onClick={handleCancel} disabled={cancelling || running || confirming || submittingPullRequest || deleting}>
              {cancelling ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <StopCircle className="h-4 w-4" />}
              {cancelling ? "取消中" : "取消任务"}
            </Button>
          ) : null}
          <Button variant="destructive" onClick={handleDelete} disabled={!canDelete || running || cancelling || confirming || submittingPullRequest || deleting}>
            {deleting ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <Trash2 className="h-4 w-4" />}
            {deleting ? DELETING_LABEL : DELETE_LABEL}
          </Button>
          <Button onClick={handleRun} disabled={!canRun || running || cancelling || confirming || submittingPullRequest || deleting}>
            {running ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <PlayCircle className="h-4 w-4" />}
            运行 Agent
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
              <p className="mt-1 font-semibold text-slate-900">{project?.repoName || `项目 #${task.projectId}`}</p>
              <p className="mt-1 break-all text-xs text-slate-500">{project?.repoUrl || "--"}</p>
            </div>
            <Separator />
            <div>
              <p className="text-xs uppercase tracking-wide text-slate-400">{MODEL_LABEL}</p>
              <p className="mt-1 font-semibold text-slate-900">{task.llmDisplayName || task.llmModelName || "--"}</p>
              <p className="mt-1 text-xs text-slate-500">{task.llmProvider || "--"}</p>
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
              <p className="mt-1 whitespace-pre-wrap leading-7 text-slate-700">{task.resultSummary || EMPTY_SUMMARY}</p>
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

        <PatchViewer patch={patch} submittingPullRequest={submittingPullRequest} onSubmitPullRequest={handleSubmitPullRequest} />
      </section>
    </div>
  );
}
