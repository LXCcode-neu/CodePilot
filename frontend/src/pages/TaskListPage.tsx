import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { getProjects } from "@/api/project";
import { getTasks } from "@/api/task";
import { EmptyState } from "@/components/EmptyState";
import { LoadingBlock } from "@/components/LoadingBlock";
import { TaskCard } from "@/components/TaskCard";
import { Button } from "@/components/ui/button";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import type { ProjectRepo } from "@/types/project";
import type { AgentTask, AgentTaskStatus } from "@/types/task";

const filterOptions: Array<{ label: string; value: string }> = [
  { label: "全部状态", value: "ALL" },
  { label: "PENDING", value: "PENDING" },
  { label: "CLONING", value: "CLONING" },
  { label: "INDEXING", value: "INDEXING" },
  { label: "RETRIEVING", value: "RETRIEVING" },
  { label: "ANALYZING", value: "ANALYZING" },
  { label: "GENERATING_PATCH", value: "GENERATING_PATCH" },
  { label: "COMPLETED", value: "COMPLETED" },
  { label: "FAILED", value: "FAILED" },
];

export function TaskListPage() {
  const [tasks, setTasks] = useState<AgentTask[]>([]);
  const [projects, setProjects] = useState<ProjectRepo[]>([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState<string>("ALL");

  useEffect(() => {
    async function load() {
      try {
        const [taskData, projectData] = await Promise.all([getTasks(), getProjects()]);
        setTasks(taskData);
        setProjects(projectData);
      } finally {
        setLoading(false);
      }
    }

    void load();
  }, []);

  const repoMap = useMemo(() => new Map(projects.map((project) => [project.id, project.repoName])), [projects]);
  const filteredTasks = tasks.filter((task) => statusFilter === "ALL" || task.status === statusFilter);

  return (
    <div className="space-y-8">
      <section className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <div>
          <h1 className="text-3xl font-extrabold text-slate-900">任务列表</h1>
          <p className="mt-2 text-sm text-slate-500">按状态筛选任务，查看 Issue 描述和执行进度。</p>
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

      {loading ? (
        <LoadingBlock lines={6} />
      ) : filteredTasks.length ? (
        <div className="grid gap-5 xl:grid-cols-2">
          {filteredTasks.map((task) => (
            <TaskCard key={task.id} task={task} repoName={repoMap.get(task.projectId)} />
          ))}
        </div>
      ) : (
        <EmptyState
          title="没有符合条件的任务"
          description="你可以先创建一个任务，或者切换筛选状态查看其他记录。"
          action={<Button asChild><Link to="/tasks/new">创建任务</Link></Button>}
        />
      )}
    </div>
  );
}
