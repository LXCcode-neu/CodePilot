import { useEffect, useMemo, useState } from "react";
import { FolderPlus, Github, LoaderCircle, Plus, Search } from "lucide-react";
import { getGitHubAccount, getGitHubAuthorizedRepositories } from "@/api/github-auth";
import { createProject, deleteProject, getProjects, importGitHubProject } from "@/api/project";
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
import type { GitHubAccount, GitHubAuthorizedRepository } from "@/types/github-auth";
import type { ProjectRepo } from "@/types/project";

const CREATE_ERROR = "添加仓库失败";
const DELETE_CONFIRM_MESSAGE = "确定删除这个仓库吗？删除后该仓库下的任务、执行步骤和 Patch 记录也会一起删除。";

export function ProjectListPage() {
  const [projects, setProjects] = useState<ProjectRepo[]>([]);
  const [loading, setLoading] = useState(true);
  const [open, setOpen] = useState(false);
  const [importOpen, setImportOpen] = useState(false);
  const [repoUrl, setRepoUrl] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [account, setAccount] = useState<GitHubAccount | null>(null);
  const [authorizedRepos, setAuthorizedRepos] = useState<GitHubAuthorizedRepository[]>([]);
  const [reposLoading, setReposLoading] = useState(false);
  const [repoSearch, setRepoSearch] = useState("");
  const [importingId, setImportingId] = useState<string | null>(null);

  const filteredRepos = useMemo(() => {
    const keyword = repoSearch.trim().toLowerCase();
    if (!keyword) {
      return authorizedRepos;
    }
    return authorizedRepos.filter((repo) =>
      [repo.fullName, repo.owner, repo.name].some((value) => value.toLowerCase().includes(keyword))
    );
  }, [authorizedRepos, repoSearch]);

  async function loadProjects() {
    setProjects(await getProjects());
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

  async function loadAuthorizedRepos() {
    setReposLoading(true);
    setError("");
    try {
      const currentAccount = await getGitHubAccount();
      setAccount(currentAccount);
      setAuthorizedRepos(currentAccount.connected ? await getGitHubAuthorizedRepositories() : []);
    } catch (err) {
      setError(err instanceof Error ? err.message : "加载 GitHub 仓库失败");
      setAuthorizedRepos([]);
      setAccount(null);
    } finally {
      setReposLoading(false);
    }
  }

  async function handleImportDialogChange(nextOpen: boolean) {
    setImportOpen(nextOpen);
    setRepoSearch("");
    if (nextOpen) {
      await loadAuthorizedRepos();
    }
  }

  async function handleImportRepository(repository: GitHubAuthorizedRepository) {
    setImportingId(repository.id);
    setError("");
    try {
      await importGitHubProject({
        githubRepoId: repository.id,
        owner: repository.owner,
        repoName: repository.name,
      });
      setImportOpen(false);
      await loadProjects();
    } catch (err) {
      setError(err instanceof Error ? err.message : "导入 GitHub 仓库失败");
    } finally {
      setImportingId(null);
    }
  }

  return (
    <div className="space-y-8">
      <section className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <div>
          <h1 className="text-3xl font-extrabold text-slate-900">仓库管理</h1>
          <p className="mt-2 text-sm text-slate-500">
            维护项目仓库列表。你可以手动添加公开仓库，也可以把已授权的 GitHub 仓库直接导入进来。
          </p>
        </div>

        <div className="flex flex-wrap gap-3">
          <Dialog open={importOpen} onOpenChange={(value) => void handleImportDialogChange(value)}>
            <DialogTrigger asChild>
              <Button variant="secondary">
                <Github className="h-4 w-4" />
                从 GitHub 导入
              </Button>
            </DialogTrigger>

            <DialogContent className="max-w-3xl">
              <DialogHeader>
                <DialogTitle>从 GitHub 导入仓库</DialogTitle>
                <DialogDescription>
                  选择你已经授权给 CodePilot 的 GitHub 仓库，并把它加入项目仓库列表。
                </DialogDescription>
              </DialogHeader>

              {reposLoading ? (
                <div className="flex items-center justify-center py-12 text-slate-500">
                  <LoaderCircle className="h-5 w-5 animate-spin" />
                </div>
              ) : account?.connected ? (
                <div className="space-y-4">
                  <div className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
                    {account.githubAvatarUrl ? (
                      <img src={account.githubAvatarUrl} alt={account.githubLogin || "GitHub"} className="h-10 w-10 rounded-full" />
                    ) : (
                      <div className="flex h-10 w-10 items-center justify-center rounded-full bg-white text-slate-600 shadow-sm">
                        <Github className="h-4 w-4" />
                      </div>
                    )}
                    <div className="min-w-0">
                      <p className="text-sm font-semibold text-slate-900">
                        {account.githubName || account.githubLogin || "GitHub Account"}
                      </p>
                      <p className="truncate text-xs text-slate-500">
                        @{account.githubLogin} {account.scope ? `· ${account.scope}` : ""}
                      </p>
                    </div>
                  </div>

                  <div className="relative">
                    <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                    <Input
                      value={repoSearch}
                      onChange={(event) => setRepoSearch(event.target.value)}
                      placeholder="搜索 owner/repo"
                      className="pl-9"
                    />
                  </div>

                  <div className="max-h-[420px] space-y-3 overflow-auto pr-1">
                    {filteredRepos.length ? (
                      filteredRepos.map((repository) => (
                        <div
                          key={repository.id}
                          className="flex items-center justify-between gap-4 rounded-2xl border border-slate-200 bg-white px-4 py-4"
                        >
                          <div className="min-w-0">
                            <p className="truncate text-sm font-semibold text-slate-900">{repository.fullName}</p>
                            <p className="mt-1 text-xs text-slate-500">
                              {repository.privateRepo ? "Private" : "Public"}
                              {repository.defaultBranch ? ` · ${repository.defaultBranch}` : ""}
                            </p>
                          </div>
                          <Button
                            size="sm"
                            onClick={() => void handleImportRepository(repository)}
                            disabled={importingId === repository.id}
                          >
                            {importingId === repository.id ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <FolderPlus className="h-4 w-4" />}
                            导入
                          </Button>
                        </div>
                      ))
                    ) : (
                      <div className="rounded-2xl border border-dashed border-slate-200 bg-slate-50 px-4 py-8 text-center text-sm text-slate-500">
                        没有匹配的 GitHub 仓库。
                      </div>
                    )}
                  </div>
                </div>
              ) : (
                <div className="space-y-4 rounded-2xl border border-dashed border-slate-200 bg-slate-50 px-4 py-8 text-center">
                  <p className="text-sm text-slate-600">你还没有连接 GitHub 账号，先完成授权后才能导入仓库。</p>
                  <DialogFooter className="justify-center">
                    <Button onClick={() => window.location.assign("/github-auth")}>
                      <Github className="h-4 w-4" />
                      前往 GitHub 授权
                    </Button>
                  </DialogFooter>
                </div>
              )}
            </DialogContent>
          </Dialog>

          <Dialog open={open} onOpenChange={setOpen}>
            <DialogTrigger asChild>
              <Button>
                <Plus className="h-4 w-4" />
                手动添加仓库
              </Button>
            </DialogTrigger>

            <DialogContent>
              <DialogHeader>
                <DialogTitle>添加 GitHub 仓库</DialogTitle>
                <DialogDescription>
                  输入合法的 GitHub 仓库 HTTPS 地址，例如 `https://github.com/octocat/Hello-World.git`。
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
        </div>
      </section>

      {error ? <div className="rounded-2xl bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div> : null}

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
          description="先添加一个 GitHub 仓库，后续任务才能基于仓库代码运行。"
          action={<Button onClick={() => setOpen(true)}>立即添加仓库</Button>}
        />
      )}
    </div>
  );
}
