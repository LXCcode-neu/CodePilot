import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { deleteTask, getTasks } from "@/api/task";
import { getProjects } from "@/api/project";
import { EmptyState } from "@/components/EmptyState";
import { LoadingBlock } from "@/components/LoadingBlock";
import { TaskCard } from "@/components/TaskCard";
import { Button } from "@/components/ui/button";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import type { ProjectRepo } from "@/types/project";
import type { AgentTask } from "@/types/task";

const filterOptions: Array<{ label: string; value: string }> = [
  { label: "全部状态", value: "ALL" },
  { label: "PENDING", value: "PENDING" },
  { label: "CLONING", value: "CLONING" },
  { label: "RETRIEVING", value: "RETRIEVING" },
  { label: "ANALYZING", value: "ANALYZING" },
  { label: "GENERATING_PATCH", value: "GENERATING_PATCH" },
  { label: "VERIFYING", value: "VERIFYING" },
  { label: "WAITING_CONFIRM", value: "WAITING_CONFIRM" },
  { label: "COMPLETED", value: "COMPLETED" },
  { label: "FAILED", value: "FAILED" },
];

const DELETE_CONFIRM_MESSAGE = "确定删除这个任务吗？删除后对应的执行步骤和 Patch 记录也会一起删除。";
const LOAD_TASKS_ERROR = "加载任务失败";

export function TaskListPage() {
  const [tasks, setTasks] = useState<AgentTask[]>([]);
  const [projects, setProjects] = useState<ProjectRepo[]>([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState<string>("ALL");
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [error, setError] = useState("");

  async function loadPageData() {
    const [taskData, projectData] = await Promise.all([getTasks(), getProjects()]);
    setTasks(taskData);
    setProjects(projectData);
  }

  useEffect(() => {
    loadPageData()
      .catch((err) => setError(err instanceof Error ? err.message : LOAD_TASKS_ERROR))
      .finally(() => setLoading(false));
  }, []);

  async function handleDelete(id: string) {
    if (!window.confirm(DELETE_CONFIRM_MESSAGE)) {
      return;
    }

    setDeletingId(id);
    setError("");
    try {
      await deleteTask(id);
      await loadPageData();
    } catch (err) {
      setError(err instanceof Error ? err.message : LOAD_TASKS_ERROR);
    } finally {
      setDeletingId(null);
    }
  }

  const repoMap = useMemo(() => new Map(projects.map((project) => [project.id, project.repoName])), [projects]);
  const filteredTasks = tasks.filter((task) => statusFilter === "ALL" || task.status === statusFilter);

  return (
    <div className="space-y-8">
      <section className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <div>
          <h1 className="text-3xl font-extrabold text-slate-900">任务列表</h1>
          <p className="mt-2 text-sm text-slate-500">按状态筛选任务，查看 Issue 描述、执行进度与删除入口。</p>
        </div>
        <div className="flex flex-col gap-3 sm:flex-row">
          <Select value={statusFilter} onValueChange={setStatusFilter}>
            <SelectTrigger className="w-[220px]">
              <SelectValue placeholder="选择状态" />
            </SelectTrigger>
            <SelectContent>
              {filterOptions.map((option) => (
                <SelectItem key={option.value} value={option.value}>
                  {option.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Button asChild>
            <Link to="/tasks/new">创建任务</Link>
          </Button>
        </div>
      </section>

      {error ? (
        <div className="rounded-2xl border border-red-100 bg-red-50 px-5 py-4 text-sm text-red-700">{error}</div>
      ) : null}

      {loading ? (
        <LoadingBlock lines={6} />
      ) : filteredTasks.length ? (
        <div className="grid gap-5 xl:grid-cols-2">
          {filteredTasks.map((task) => (
            <TaskCard
              key={task.id}
              task={task}
              repoName={repoMap.get(task.projectId)}
              onDelete={handleDelete}
              deleting={deletingId === task.id}
            />
          ))}
        </div>
      ) : (
        <EmptyState
          title="没有符合条件的任务"
          description="你可以先创建一个任务，或者切换筛选条件查看其他记录。"
          action={
            <Button asChild>
              <Link to="/tasks/new">创建任务</Link>
            </Button>
          }
        />
      )}
    </div>
  );
}
