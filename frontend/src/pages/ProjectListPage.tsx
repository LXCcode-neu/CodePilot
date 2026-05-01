import { useEffect, useState } from "react";
import { LoaderCircle, Plus } from "lucide-react";
import { createProject, deleteProject, getProjects } from "@/api/project";
import { EmptyState } from "@/components/EmptyState";
import { LoadingBlock } from "@/components/LoadingBlock";
import { ProjectCard } from "@/components/ProjectCard";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import type { ProjectRepo } from "@/types/project";

const CREATE_ERROR = "添加仓库失败";
const DELETE_CONFIRM_MESSAGE = "确定删除这个仓库吗？删除后该仓库下的相关任务、执行步骤和 Patch 记录也会一起删除。";

export function ProjectListPage() {
  const [projects, setProjects] = useState<ProjectRepo[]>([]);
  const [loading, setLoading] = useState(true);
  const [open, setOpen] = useState(false);
  const [repoUrl, setRepoUrl] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [deletingId, setDeletingId] = useState<string | null>(null);

  async function loadProjects() {
    const data = await getProjects();
    setProjects(data);
  }

  useEffect(() => {
    loadProjects()
      .catch(() => undefined)
      .finally(() => setLoading(false));
  }, []);

  async function handleCreate(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError("");

    try {
      await createProject({ repoUrl });
      setRepoUrl("");
      setOpen(false);
      await loadProjects();
    } catch (err) {
      setError(err instanceof Error ? err.message : CREATE_ERROR);
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(id: string) {
    if (!window.confirm(DELETE_CONFIRM_MESSAGE)) {
      return;
    }

    setDeletingId(id);
    setError("");
    try {
      await deleteProject(id);
      await loadProjects();
    } catch (err) {
      setError(err instanceof Error ? err.message : CREATE_ERROR);
    } finally {
      setDeletingId(null);
    }
  }

  return (
    <div className="space-y-8">
      <section className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <div>
          <h1 className="text-3xl font-extrabold text-slate-900">仓库管理</h1>
          <p className="mt-2 text-sm text-slate-500">接入 GitHub 公开仓库，查看仓库状态、本地路径，并为后续任务做准备。</p>
        </div>

        <Dialog open={open} onOpenChange={setOpen}>
          <DialogTrigger asChild>
            <Button>
              <Plus className="h-4 w-4" />
              添加 GitHub 仓库
            </Button>
          </DialogTrigger>

          <DialogContent>
            <DialogHeader>
              <DialogTitle>添加公开仓库</DialogTitle>
              <DialogDescription>
                请输入合法的 GitHub 仓库 HTTPS 地址，例如 https://github.com/octocat/Hello-World.git
              </DialogDescription>
            </DialogHeader>

            <form className="space-y-4" onSubmit={handleCreate}>
              <Input
                value={repoUrl}
                onChange={(event) => setRepoUrl(event.target.value)}
                placeholder="https://github.com/owner/repo.git"
                required
              />

              {error ? <div className="rounded-2xl bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div> : null}

              <DialogFooter>
                <Button type="submit" disabled={submitting}>
                  {submitting ? <LoaderCircle className="h-4 w-4 animate-spin" /> : null}
                  保存仓库
                </Button>
              </DialogFooter>
            </form>
          </DialogContent>
        </Dialog>
      </section>

      {loading ? (
        <LoadingBlock lines={6} />
      ) : projects.length ? (
        <div className="grid gap-5 xl:grid-cols-2">
          {projects.map((project) => (
            <ProjectCard key={project.id} project={project} onDelete={handleDelete} deleting={deletingId === project.id} />
          ))}
        </div>
      ) : (
        <EmptyState
          title="还没有仓库"
          description="先添加一个 GitHub 公开仓库，后续才能基于仓库创建任务并运行 Agent。"
          action={<Button onClick={() => setOpen(true)}>立即添加仓库</Button>}
        />
      )}
    </div>
  );
}
