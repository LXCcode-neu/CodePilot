import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { ArrowRight, Bot, CircleCheckBig, FolderGit2, OctagonAlert } from "lucide-react";
import { getProjects } from "@/api/project";
import { deleteTask, getTasks } from "@/api/task";
import { EmptyState } from "@/components/EmptyState";
import { LoadingBlock } from "@/components/LoadingBlock";
import { TaskCard } from "@/components/TaskCard";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import type { ProjectRepo } from "@/types/project";
import type { AgentTask } from "@/types/task";

const DELETE_CONFIRM_MESSAGE = "确定删除这个任务吗？删除后对应的执行步骤和 Patch 记录也会一起删除。";
const DELETE_TASK_ERROR = "删除任务失败";

export function DashboardPage() {
  const [projects, setProjects] = useState<ProjectRepo[]>([]);
  const [tasks, setTasks] = useState<AgentTask[]>([]);
  const [loading, setLoading] = useState(true);
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [error, setError] = useState("");

  async function loadDashboardData() {
    const [projectData, taskData] = await Promise.all([getProjects(), getTasks()]);
    setProjects(projectData);
    setTasks(taskData);
  }

  useEffect(() => {
    loadDashboardData()
      .catch(() => undefined)
      .finally(() => setLoading(false));
  }, []);

  async function handleDeleteTask(id: string) {
    if (!window.confirm(DELETE_CONFIRM_MESSAGE)) {
      return;
    }

    setDeletingId(id);
    setError("");
    try {
      await deleteTask(id);
      await loadDashboardData();
    } catch (err) {
      setError(err instanceof Error ? err.message : DELETE_TASK_ERROR);
    } finally {
      setDeletingId(null);
    }
  }

  const repoMap = useMemo(() => new Map(projects.map((project) => [project.id, project.repoName])), [projects]);
  const recentTasks = tasks.slice(0, 5);

  const stats = [
    { label: "仓库数量", value: projects.length, icon: FolderGit2 },
    { label: "任务数量", value: tasks.length, icon: Bot },
    { label: "已完成任务", value: tasks.filter((task) => task.status === "COMPLETED").length, icon: CircleCheckBig },
    { label: "失败任务", value: tasks.filter((task) => task.status === "FAILED").length, icon: OctagonAlert },
  ];

  return (
    <div className="space-y-8">
      <section className="flex flex-col gap-5 rounded-[28px] bg-slate-900 px-8 py-8 text-white lg:flex-row lg:items-end lg:justify-between">
        <div className="space-y-3">
          <p className="text-sm uppercase tracking-[0.24em] text-slate-300">Dashboard</p>
          <h1 className="text-3xl font-extrabold">CodePilot 控制台</h1>
          <p className="max-w-2xl text-sm leading-7 text-slate-300">
            在一个入口里查看仓库接入、Agent 任务状态，以及最近的 Issue-to-Patch 执行轨迹。
          </p>
        </div>
        <div className="flex flex-wrap gap-3">
          <Button asChild variant="secondary">
            <Link to="/projects">添加仓库</Link>
          </Button>
          <Button asChild>
            <Link to="/tasks/new">创建任务</Link>
          </Button>
        </div>
      </section>

      <section className="grid gap-4 xl:grid-cols-4">
        {stats.map((item) => {
          const Icon = item.icon;
          return (
            <Card key={item.label}>
              <CardContent className="flex items-center justify-between gap-4 py-6">
                <div>
                  <p className="text-sm text-slate-500">{item.label}</p>
                  <p className="mt-2 text-3xl font-bold text-slate-900">{item.value}</p>
                </div>
                <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-slate-100 text-slate-700">
                  <Icon className="h-5 w-5" />
                </div>
              </CardContent>
            </Card>
          );
        })}
      </section>

      <section className="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
        <Card>
          <CardHeader className="flex-row items-end justify-between gap-4 space-y-0">
            <div>
              <CardTitle className="section-title">最近任务</CardTitle>
              <p className="section-subtitle mt-1">最近 5 条任务记录，可以直接删除或进入详情页。</p>
            </div>
            <Button asChild variant="ghost" size="sm">
              <Link to="/tasks">
                查看全部
                <ArrowRight className="h-4 w-4" />
              </Link>
            </Button>
          </CardHeader>
          <CardContent>
            {error ? (
              <div className="mb-4 rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>
            ) : null}

            {loading ? (
              <LoadingBlock lines={5} />
            ) : recentTasks.length ? (
              <div className="grid gap-4">
                {recentTasks.map((task) => (
                  <TaskCard
                    key={task.id}
                    task={task}
                    repoName={repoMap.get(task.projectId)}
                    onDelete={handleDeleteTask}
                    deleting={deletingId === task.id}
                  />
                ))}
              </div>
            ) : (
              <EmptyState
                title="还没有任务"
                description="先创建一个仓库，然后发起第一个 Agent 任务。"
                action={
                  <Button asChild>
                    <Link to="/tasks/new">去创建任务</Link>
                  </Button>
                }
              />
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="section-title">仓库概览</CardTitle>
            <p className="section-subtitle mt-1">当前已接入的仓库与本地路径状态。</p>
          </CardHeader>
          <CardContent>
            {loading ? (
              <LoadingBlock />
            ) : projects.length ? (
              <div className="space-y-4">
                {projects.slice(0, 4).map((project) => (
                  <div key={project.id}>
                    <div className="space-y-1">
                      <p className="text-sm font-semibold text-slate-900">{project.repoName}</p>
                      <p className="truncate text-xs text-slate-500">{project.repoUrl}</p>
                      <p className="truncate text-xs text-slate-400">{project.localPath || "--"}</p>
                    </div>
                    <Separator className="mt-4" />
                  </div>
                ))}
              </div>
            ) : (
              <EmptyState
                title="暂无仓库"
                description="添加 GitHub 公开仓库后，这里会显示仓库总览。"
                action={
                  <Button asChild>
                    <Link to="/projects">添加仓库</Link>
                  </Button>
                }
              />
            )}
          </CardContent>
        </Card>
      </section>
    </div>
  );
}
