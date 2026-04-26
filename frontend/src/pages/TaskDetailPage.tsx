import { useEffect, useMemo, useState } from "react";
import { useParams } from "react-router-dom";
import { LoaderCircle, PlayCircle, RefreshCcw } from "lucide-react";
import { getProject } from "@/api/project";
import { createTaskEventSource, parseTaskEventMessage } from "@/api/sse";
import { getTaskSteps } from "@/api/step";
import { getTask, runTask } from "@/api/task";
import { getTaskPatch } from "@/api/patch";
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
      setError("无效的任务 ID");
      return;
    }

    loadAll()
      .catch((err) => setError(err instanceof Error ? err.message : "加载任务详情失败"))
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
      source.onmessage = (event) => {
        const message = parseTaskEventMessage(event);
        setEvents((prev) => [message, ...prev].slice(0, 50));
        void loadAll().catch(() => undefined);
      };
      source.onerror = () => {
        setEvents((prev) => [
          {
            id: crypto.randomUUID(),
            time: new Date().toISOString(),
            status: "SSE",
            message: "SSE 连接中断或后端尚未实现事件推送。",
          },
          ...prev,
        ].slice(0, 50));
        source?.close();
      };
    } catch {
      source?.close();
    }

    return () => source?.close();
  }, [taskId]);

  const canRun = useMemo(() => task?.status === "PENDING" || task?.status === "FAILED", [task?.status]);

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
          message: "已触发 Agent 运行请求。",
        },
        ...prev,
      ]);
      await loadAll();
    } catch (err) {
      setError(err instanceof Error ? err.message : "运行 Agent 失败");
    } finally {
      setRunning(false);
    }
  }

  if (loading) {
    return <LoadingBlock lines={8} />;
  }

  if (error && !task) {
    return <EmptyState title="加载失败" description={error} />;
  }

  if (!task) {
    return <EmptyState title="任务不存在" description="请返回任务列表重新选择任务。" />;
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
          <Button variant="secondary" onClick={() => void loadAll()} disabled={running}>
            <RefreshCcw className="h-4 w-4" />
            刷新
          </Button>
          <Button onClick={handleRun} disabled={!canRun || running}>
            {running ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <PlayCircle className="h-4 w-4" />}
            Run Agent
          </Button>
        </div>
      </section>

      {task.errorMessage || error ? (
        <div className="rounded-2xl border border-red-100 bg-red-50 px-5 py-4 text-sm text-red-700">{task.errorMessage || error}</div>
      ) : null}

      <section className="grid gap-6 xl:grid-cols-[0.95fr_1.05fr]">
        <Card>
          <CardHeader>
            <CardTitle className="section-title">任务信息</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-4 text-sm text-slate-600">
            <div>
              <p className="text-xs uppercase tracking-wide text-slate-400">关联仓库</p>
              <p className="mt-1 font-semibold text-slate-900">{project?.repoName || `Project #${task.projectId}`}</p>
              <p className="mt-1 break-all text-xs text-slate-500">{project?.repoUrl || "--"}</p>
            </div>
            <Separator />
            <div className="grid gap-3 md:grid-cols-2">
              <div>
                <p className="text-xs uppercase tracking-wide text-slate-400">创建时间</p>
                <p className="mt-1 text-slate-700">{formatDateTime(task.createdAt)}</p>
              </div>
              <div>
                <p className="text-xs uppercase tracking-wide text-slate-400">最近更新时间</p>
                <p className="mt-1 text-slate-700">{formatDateTime(task.updatedAt)}</p>
              </div>
            </div>
            <Separator />
            <div>
              <p className="text-xs uppercase tracking-wide text-slate-400">结果摘要</p>
              <p className="mt-1 whitespace-pre-wrap leading-7 text-slate-700">{task.resultSummary || "任务尚未产出摘要。"}</p>
            </div>
          </CardContent>
        </Card>

        <RealtimeEventPanel events={events} />
      </section>

      <section className="grid gap-6 xl:grid-cols-[1.05fr_0.95fr]">
        <div className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="section-title">执行轨迹</CardTitle>
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
