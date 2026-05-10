import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ExternalLink, GitPullRequest, LoaderCircle, MessageSquare, RefreshCw } from "lucide-react";
import { getGitHubIssues, importGitHubIssueAsTask } from "@/api/github-issue";
import { getProjects } from "@/api/project";
import { EmptyState } from "@/components/EmptyState";
import { LoadingBlock } from "@/components/LoadingBlock";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import type { GitHubIssue, GitHubIssuePage as GitHubIssuePageData, GitHubIssueState } from "@/types/github-issue";
import type { ProjectRepo } from "@/types/project";

const ISSUE_PAGE_SIZE = 10;
const ISSUE_LOAD_ERROR = "拉取 GitHub Issue 失败";
const ISSUE_IMPORT_ERROR = "导入 GitHub Issue 失败";

const stateLabels: Record<GitHubIssueState, string> = {
  open: "开启",
  closed: "已关闭",
  all: "全部",
};

export function GitHubIssuePage() {
  const navigate = useNavigate();
  const [projects, setProjects] = useState<ProjectRepo[]>([]);
  const [projectLoading, setProjectLoading] = useState(true);
  const [selectedProjectId, setSelectedProjectId] = useState("");
  const [issueState, setIssueState] = useState<GitHubIssueState>("all");
  const [issuePage, setIssuePage] = useState(1);
  const [pageInput, setPageInput] = useState("1");
  const [issueData, setIssueData] = useState<GitHubIssuePageData | null>(null);
  const [issueLoading, setIssueLoading] = useState(false);
  const [issueError, setIssueError] = useState("");
  const [importingIssueNumber, setImportingIssueNumber] = useState<number | null>(null);

  useEffect(() => {
    getProjects()
      .then(setProjects)
      .catch(() => undefined)
      .finally(() => setProjectLoading(false));
  }, []);

  useEffect(() => {
    if (!selectedProjectId && projects.length) {
      setSelectedProjectId(projects[0].id);
    }
  }, [projects, selectedProjectId]);

  useEffect(() => {
    if (selectedProjectId) {
      void loadIssues(selectedProjectId, issueState, issuePage);
    } else {
      setIssueData(null);
    }
  }, [selectedProjectId, issueState, issuePage]);

  useEffect(() => {
    setPageInput(String(issuePage));
  }, [issuePage]);

  async function loadIssues(projectId: string, state: GitHubIssueState, page: number) {
    setIssueLoading(true);
    setIssueError("");
    try {
      const data = await getGitHubIssues(projectId, { state, page, pageSize: ISSUE_PAGE_SIZE });
      setIssueData(data);
    } catch (err) {
      setIssueData(null);
      setIssueError(err instanceof Error ? err.message : ISSUE_LOAD_ERROR);
    } finally {
      setIssueLoading(false);
    }
  }

  function handleProjectChange(projectId: string) {
    setSelectedProjectId(projectId);
    setIssuePage(1);
  }

  function handleIssueStateChange(state: GitHubIssueState) {
    setIssueState(state);
    setIssuePage(1);
  }

  function handlePageJump() {
    const parsedPage = Number.parseInt(pageInput, 10);
    if (!Number.isFinite(parsedPage)) {
      setPageInput(String(issuePage));
      return;
    }
    const totalPages = issueData?.totalPages ?? 1;
    const nextPage = Math.min(Math.max(parsedPage, 1), Math.max(totalPages, 1));
    setIssuePage(nextPage);
  }

  async function handleImportIssue(issue: GitHubIssue) {
    if (!selectedProjectId) {
      return;
    }

    setImportingIssueNumber(issue.number);
    setIssueError("");
    try {
      const task = await importGitHubIssueAsTask(selectedProjectId, issue.number);
      navigate(`/tasks/${task.id}`);
    } catch (err) {
      setIssueError(err instanceof Error ? err.message : ISSUE_IMPORT_ERROR);
    } finally {
      setImportingIssueNumber(null);
    }
  }

  const selectedProject = projects.find((project) => project.id === selectedProjectId);

  return (
    <div className="space-y-8">
      <section className="flex flex-col gap-5 rounded-[28px] bg-slate-900 px-8 py-8 text-white lg:flex-row lg:items-end lg:justify-between">
        <div className="space-y-3">
          <p className="text-sm uppercase tracking-[0.24em] text-slate-300">GITHUB ISSUES</p>
          <h1 className="text-3xl font-extrabold">GitHub Issue 列表</h1>
          <p className="max-w-2xl text-sm leading-7 text-slate-300">
            切换已添加的仓库，查看从 GitHub 实时拉取的 Issue，并一键导入为 Agent 任务。
          </p>
        </div>
        <Button asChild variant="secondary">
          <Link to="/projects">添加仓库</Link>
        </Button>
      </section>

      <Card>
        <CardHeader className="gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <CardTitle className="section-title">Issue 筛选</CardTitle>
            <p className="section-subtitle mt-1">选择仓库和状态后，列表会自动刷新。</p>
          </div>
          <div className="grid gap-3 md:grid-cols-[minmax(240px,1fr)_150px_auto]">
            <Select value={selectedProjectId} onValueChange={handleProjectChange} disabled={!projects.length}>
              <SelectTrigger>
                <SelectValue placeholder="选择仓库" />
              </SelectTrigger>
              <SelectContent>
                {projects.map((project) => (
                  <SelectItem key={project.id} value={project.id}>
                    {project.repoName}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

            <Select value={issueState} onValueChange={(value) => handleIssueStateChange(value as GitHubIssueState)}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="open">开启</SelectItem>
                <SelectItem value="closed">已关闭</SelectItem>
                <SelectItem value="all">全部</SelectItem>
              </SelectContent>
            </Select>

            <Button
              type="button"
              variant="outline"
              onClick={() => selectedProjectId && loadIssues(selectedProjectId, issueState, issuePage)}
              disabled={!selectedProjectId || issueLoading}
            >
              {issueLoading ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <RefreshCw className="h-4 w-4" />}
              刷新
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          {projectLoading ? (
            <LoadingBlock lines={4} />
          ) : !projects.length ? (
            <EmptyState
              title="还没有仓库"
              description="先添加一个 GitHub 仓库，然后就能在这里查看它的 Issue。"
              action={
                <Button asChild>
                  <Link to="/projects">添加仓库</Link>
                </Button>
              }
            />
          ) : issueLoading && !issueData ? (
            <LoadingBlock lines={4} />
          ) : (
            <div className="space-y-4">
              {selectedProject ? (
                <div className="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-600">
                  当前仓库 <span className="font-semibold text-slate-900">{selectedProject.repoName}</span>
                  <span className="mx-2 text-slate-300">|</span>
                  <span className="break-all">{selectedProject.repoUrl}</span>
                </div>
              ) : null}

              {issueError ? (
                <div className="rounded-xl border border-red-100 bg-red-50 px-4 py-3 text-sm text-red-700">{issueError}</div>
              ) : null}

              {issueData?.items.length ? (
                <div className="divide-y divide-slate-100 rounded-xl border border-slate-200">
                  {issueData.items.map((issue) => (
                    <div key={issue.id} className="space-y-3 p-4">
                      <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
                        <div className="min-w-0 space-y-2">
                          <div className="flex flex-wrap items-center gap-2">
                            <Badge variant={issue.state === "open" ? "default" : "secondary"}>#{issue.number}</Badge>
                            <Badge variant="outline">{issue.state === "open" ? "开启" : "已关闭"}</Badge>
                            {issue.labels.slice(0, 4).map((label) => (
                              <Badge key={label} variant="secondary">
                                {label}
                              </Badge>
                            ))}
                          </div>
                          <h3 className="break-words text-base font-semibold text-slate-950">{issue.title}</h3>
                          <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-slate-500">
                            <span>作者：{issue.authorLogin || "未知"}</span>
                            <span className="inline-flex items-center gap-1">
                              <MessageSquare className="h-3.5 w-3.5" />
                              评论 {issue.comments}
                            </span>
                            <span>更新时间：{formatDate(issue.updatedAt)}</span>
                          </div>
                        </div>
                        <div className="flex shrink-0 flex-wrap gap-2">
                          <Button asChild variant="outline" size="sm">
                            <a href={issue.htmlUrl} target="_blank" rel="noreferrer">
                              <ExternalLink className="h-4 w-4" />
                              打开 GitHub
                            </a>
                          </Button>
                          <Button size="sm" onClick={() => handleImportIssue(issue)} disabled={importingIssueNumber === issue.number}>
                            {importingIssueNumber === issue.number ? (
                              <LoaderCircle className="h-4 w-4 animate-spin" />
                            ) : (
                              <GitPullRequest className="h-4 w-4" />
                            )}
                            导入任务
                          </Button>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              ) : selectedProjectId && !issueLoading && !issueError ? (
                <EmptyState title="没有找到 Issue" description={`当前仓库没有状态为“${stateLabels[issueState]}”的 Issue。`} />
              ) : null}

              {issueData ? (
                <div className="flex flex-col gap-3 border-t border-slate-100 pt-4 md:flex-row md:items-center md:justify-between">
                  <p className="text-sm text-slate-500">
                    每页 {issueData.pageSize} 条，共 {issueData.totalCount} 条，第 {issueData.page} / {Math.max(issueData.totalPages, 1)} 页
                  </p>
                  <div className="flex flex-wrap items-center gap-2">
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      disabled={!issueData.hasPrevious || issueLoading}
                      onClick={() => setIssuePage((current) => Math.max(current - 1, 1))}
                    >
                      上一页
                    </Button>
                    <div className="flex items-center gap-2">
                      <Input
                        className="h-9 w-20"
                        inputMode="numeric"
                        value={pageInput}
                        onChange={(event) => setPageInput(event.target.value)}
                        onKeyDown={(event) => {
                          if (event.key === "Enter") {
                            handlePageJump();
                          }
                        }}
                        disabled={issueLoading}
                      />
                      <Button type="button" variant="outline" size="sm" onClick={handlePageJump} disabled={issueLoading}>
                        跳转
                      </Button>
                    </div>
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      disabled={!issueData.hasNext || issueLoading}
                      onClick={() => setIssuePage((current) => current + 1)}
                    >
                      下一页
                    </Button>
                  </div>
                </div>
              ) : null}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

function formatDate(value?: string | null) {
  if (!value) {
    return "--";
  }
  return new Intl.DateTimeFormat("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}
