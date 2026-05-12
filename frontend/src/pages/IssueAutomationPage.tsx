import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  BellRing,
  CheckCircle2,
  ExternalLink,
  LoaderCircle,
  PauseCircle,
  PlayCircle,
  Plus,
  RefreshCw,
  Trash2,
  Wrench,
} from "lucide-react";
import {
  createNotificationChannel,
  createRepoWatch,
  deleteNotificationChannel,
  getGitHubIssueEvents,
  getNotificationChannels,
  getRepoWatches,
  ignoreGitHubIssueEvent,
  runGitHubIssueEvent,
  testNotificationChannel,
  updateRepoWatchEnabled,
} from "@/api/issue-automation";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
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
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import type {
  GitHubIssueEvent,
  GitHubIssueEventStatus,
  NotificationChannel,
  NotificationChannelType,
  UserRepoWatch,
} from "@/types/issue-automation";

const statusOptions: Array<{ value: GitHubIssueEventStatus | "ALL"; label: string }> = [
  { value: "ALL", label: "全部状态" },
  { value: "NEW", label: "新发现" },
  { value: "NOTIFIED", label: "已通知" },
  { value: "RUNNING", label: "修复中" },
  { value: "PATCH_READY", label: "Diff 已生成" },
  { value: "FAILED", label: "修复失败" },
  { value: "IGNORED", label: "已忽略" },
];

const statusLabel: Record<string, string> = {
  NEW: "新发现",
  NOTIFIED: "已通知",
  IGNORED: "已忽略",
  RUNNING: "修复中",
  PATCH_READY: "Diff 已生成",
  FAILED: "修复失败",
  PR_CREATED: "PR 已创建",
};

const channelLabel: Record<NotificationChannelType, string> = {
  FEISHU: "飞书",
  WE_COM: "企业微信",
};

export function IssueAutomationPage() {
  const navigate = useNavigate();
  const [watches, setWatches] = useState<UserRepoWatch[]>([]);
  const [channels, setChannels] = useState<NotificationChannel[]>([]);
  const [events, setEvents] = useState<GitHubIssueEvent[]>([]);
  const [status, setStatus] = useState<GitHubIssueEventStatus | "ALL">("ALL");
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState("");
  const [repoDialogOpen, setRepoDialogOpen] = useState(false);
  const [channelDialogOpen, setChannelDialogOpen] = useState(false);
  const [repoForm, setRepoForm] = useState({ repoUrl: "", owner: "", repoName: "", defaultBranch: "main" });
  const [channelForm, setChannelForm] = useState({
    channelType: "FEISHU" as NotificationChannelType,
    channelName: "",
    webhookUrl: "",
  });
  const [repoSubmitting, setRepoSubmitting] = useState(false);
  const [channelSubmitting, setChannelSubmitting] = useState(false);
  const [busyId, setBusyId] = useState<string | null>(null);

  const watchMap = useMemo(() => new Map(watches.map((watch) => [watch.id, watch])), [watches]);

  useEffect(() => {
    void loadAll();
  }, []);

  useEffect(() => {
    void loadEvents();
  }, [status]);

  async function loadAll() {
    setLoading(true);
    setError("");
    try {
      const [nextWatches, nextChannels, nextEvents] = await Promise.all([
        getRepoWatches(),
        getNotificationChannels(),
        getGitHubIssueEvents({ status, page: 1, pageSize: 50 }),
      ]);
      setWatches(nextWatches);
      setChannels(nextChannels);
      setEvents(nextEvents);
    } catch (err) {
      setError(err instanceof Error ? err.message : "加载自动修复配置失败");
    } finally {
      setLoading(false);
    }
  }

  async function loadEvents() {
    setRefreshing(true);
    setError("");
    try {
      setEvents(await getGitHubIssueEvents({ status, page: 1, pageSize: 50 }));
    } catch (err) {
      setError(err instanceof Error ? err.message : "加载 Issue 事件失败");
    } finally {
      setRefreshing(false);
    }
  }

  function handleRepoUrlChange(repoUrl: string) {
    const parsed = parseGitHubRepoUrl(repoUrl);
    setRepoForm((current) => ({
      ...current,
      repoUrl,
      owner: parsed?.owner ?? current.owner,
      repoName: parsed?.repoName ?? current.repoName,
    }));
  }

  async function handleCreateWatch(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setRepoSubmitting(true);
    setError("");
    try {
      await createRepoWatch({
        repoUrl: repoForm.repoUrl.trim(),
        owner: repoForm.owner.trim(),
        repoName: repoForm.repoName.trim(),
        defaultBranch: repoForm.defaultBranch.trim() || "main",
      });
      setRepoDialogOpen(false);
      setRepoForm({ repoUrl: "", owner: "", repoName: "", defaultBranch: "main" });
      setWatches(await getRepoWatches());
    } catch (err) {
      setError(err instanceof Error ? err.message : "添加监听仓库失败");
    } finally {
      setRepoSubmitting(false);
    }
  }

  async function handleCreateChannel(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setChannelSubmitting(true);
    setError("");
    try {
      await createNotificationChannel({
        channelType: channelForm.channelType,
        channelName: channelForm.channelName.trim(),
        webhookUrl: channelForm.webhookUrl.trim(),
      });
      setChannelDialogOpen(false);
      setChannelForm({ channelType: "FEISHU", channelName: "", webhookUrl: "" });
      setChannels(await getNotificationChannels());
    } catch (err) {
      setError(err instanceof Error ? err.message : "添加通知渠道失败");
    } finally {
      setChannelSubmitting(false);
    }
  }

  async function toggleWatch(watch: UserRepoWatch) {
    setBusyId(watch.id);
    setError("");
    try {
      const updated = await updateRepoWatchEnabled(watch.id, !watch.watchEnabled);
      setWatches((current) => current.map((item) => (item.id === updated.id ? updated : item)));
    } catch (err) {
      setError(err instanceof Error ? err.message : "更新监听状态失败");
    } finally {
      setBusyId(null);
    }
  }

  async function testChannel(channel: NotificationChannel) {
    setBusyId(channel.id);
    setError("");
    try {
      const result = await testNotificationChannel(channel.id);
      if (!result.success) {
        setError(result.message || "测试通知发送失败");
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "测试通知发送失败");
    } finally {
      setBusyId(null);
    }
  }

  async function removeChannel(channel: NotificationChannel) {
    if (!window.confirm("确定删除这个通知渠道吗？")) {
      return;
    }
    setBusyId(channel.id);
    setError("");
    try {
      await deleteNotificationChannel(channel.id);
      setChannels((current) => current.filter((item) => item.id !== channel.id));
    } catch (err) {
      setError(err instanceof Error ? err.message : "删除通知渠道失败");
    } finally {
      setBusyId(null);
    }
  }

  async function runEvent(issueEvent: GitHubIssueEvent) {
    setBusyId(issueEvent.id);
    setError("");
    try {
      const result = await runGitHubIssueEvent(issueEvent.id);
      await loadEvents();
      navigate(`/tasks/${result.taskId}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "启动自动修复失败");
    } finally {
      setBusyId(null);
    }
  }

  async function ignoreEvent(issueEvent: GitHubIssueEvent) {
    setBusyId(issueEvent.id);
    setError("");
    try {
      await ignoreGitHubIssueEvent(issueEvent.id);
      await loadEvents();
    } catch (err) {
      setError(err instanceof Error ? err.message : "忽略 Issue 事件失败");
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div className="space-y-6">
      <section className="flex flex-col gap-4 border-b border-slate-100 pb-6 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <h1 className="text-3xl font-extrabold text-slate-900">Issue 自动修复</h1>
          <p className="mt-2 max-w-3xl text-sm leading-6 text-slate-500">
            监听开源仓库的新 Issue，自动推送到飞书或企业微信；确认后创建 Agent 任务，生成 Diff 后再通知你审核。
          </p>
        </div>
        <Button onClick={() => void loadAll()} disabled={loading || refreshing}>
          {loading || refreshing ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <RefreshCw className="h-4 w-4" />}
          刷新
        </Button>
      </section>

      {error ? <div className="rounded-xl border border-red-100 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div> : null}

      <div className="grid gap-5 xl:grid-cols-2">
        <Card>
          <CardHeader className="flex-row items-center justify-between gap-3">
            <div>
              <CardTitle className="section-title">监听仓库</CardTitle>
              <p className="section-subtitle mt-1">定时轮询这些仓库的 open Issue。</p>
            </div>
            <Dialog open={repoDialogOpen} onOpenChange={setRepoDialogOpen}>
              <DialogTrigger asChild>
                <Button size="sm">
                  <Plus className="h-4 w-4" />
                  添加
                </Button>
              </DialogTrigger>
              <DialogContent>
                <DialogHeader>
                  <DialogTitle>添加监听仓库</DialogTitle>
                  <DialogDescription>填写 GitHub 仓库地址，owner 和仓库名会自动识别，也可以手动调整。</DialogDescription>
                </DialogHeader>
                <form className="space-y-4" onSubmit={handleCreateWatch}>
                  <Input
                    value={repoForm.repoUrl}
                    onChange={(event) => handleRepoUrlChange(event.target.value)}
                    placeholder="https://github.com/owner/repo.git"
                    required
                  />
                  <div className="grid gap-3 md:grid-cols-2">
                    <Input
                      value={repoForm.owner}
                      onChange={(event) => setRepoForm((current) => ({ ...current, owner: event.target.value }))}
                      placeholder="owner"
                      required
                    />
                    <Input
                      value={repoForm.repoName}
                      onChange={(event) => setRepoForm((current) => ({ ...current, repoName: event.target.value }))}
                      placeholder="repo"
                      required
                    />
                  </div>
                  <Input
                    value={repoForm.defaultBranch}
                    onChange={(event) => setRepoForm((current) => ({ ...current, defaultBranch: event.target.value }))}
                    placeholder="默认分支，例如 main"
                  />
                  <DialogFooter>
                    <Button type="submit" disabled={repoSubmitting}>
                      {repoSubmitting ? <LoaderCircle className="h-4 w-4 animate-spin" /> : null}
                      保存
                    </Button>
                  </DialogFooter>
                </form>
              </DialogContent>
            </Dialog>
          </CardHeader>
          <CardContent>
            {loading ? (
              <LoadingRows />
            ) : watches.length ? (
              <div className="space-y-3">
                {watches.map((watch) => (
                  <div key={watch.id} className="rounded-xl border border-slate-200 bg-white px-4 py-3">
                    <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
                      <div className="min-w-0">
                        <div className="flex flex-wrap items-center gap-2">
                          <p className="break-all text-sm font-semibold text-slate-900">
                            {watch.owner}/{watch.repoName}
                          </p>
                          <Badge variant={watch.watchEnabled ? "default" : "secondary"}>
                            {watch.watchEnabled ? "监听中" : "已暂停"}
                          </Badge>
                        </div>
                        <p className="mt-1 break-all text-xs text-slate-500">{watch.repoUrl}</p>
                        <p className="mt-2 text-xs text-slate-500">
                          默认分支：{watch.defaultBranch || "main"} · 最近检查：{formatDate(watch.lastCheckedAt)}
                        </p>
                      </div>
                      <Button size="sm" variant="outline" onClick={() => void toggleWatch(watch)} disabled={busyId === watch.id}>
                        {busyId === watch.id ? (
                          <LoaderCircle className="h-4 w-4 animate-spin" />
                        ) : watch.watchEnabled ? (
                          <PauseCircle className="h-4 w-4" />
                        ) : (
                          <PlayCircle className="h-4 w-4" />
                        )}
                        {watch.watchEnabled ? "暂停" : "启用"}
                      </Button>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <EmptyPanel title="还没有监听仓库" description="添加仓库后，系统会定时拉取新的 GitHub Issue。" />
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex-row items-center justify-between gap-3">
            <div>
              <CardTitle className="section-title">通知渠道</CardTitle>
              <p className="section-subtitle mt-1">支持飞书和企业微信机器人 webhook。</p>
            </div>
            <Dialog open={channelDialogOpen} onOpenChange={setChannelDialogOpen}>
              <DialogTrigger asChild>
                <Button size="sm">
                  <BellRing className="h-4 w-4" />
                  添加
                </Button>
              </DialogTrigger>
              <DialogContent>
                <DialogHeader>
                  <DialogTitle>添加通知渠道</DialogTitle>
                  <DialogDescription>Webhook 会加密保存在服务端，前端只展示脱敏信息。</DialogDescription>
                </DialogHeader>
                <form className="space-y-4" onSubmit={handleCreateChannel}>
                  <Select
                    value={channelForm.channelType}
                    onValueChange={(value) =>
                      setChannelForm((current) => ({ ...current, channelType: value as NotificationChannelType }))
                    }
                  >
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="FEISHU">飞书</SelectItem>
                      <SelectItem value="WE_COM">企业微信</SelectItem>
                    </SelectContent>
                  </Select>
                  <Input
                    value={channelForm.channelName}
                    onChange={(event) => setChannelForm((current) => ({ ...current, channelName: event.target.value }))}
                    placeholder="渠道名称，例如：研发群"
                  />
                  <Textarea
                    value={channelForm.webhookUrl}
                    onChange={(event) => setChannelForm((current) => ({ ...current, webhookUrl: event.target.value }))}
                    placeholder="粘贴机器人 webhook URL"
                    required
                  />
                  <DialogFooter>
                    <Button type="submit" disabled={channelSubmitting}>
                      {channelSubmitting ? <LoaderCircle className="h-4 w-4 animate-spin" /> : null}
                      保存
                    </Button>
                  </DialogFooter>
                </form>
              </DialogContent>
            </Dialog>
          </CardHeader>
          <CardContent>
            {loading ? (
              <LoadingRows />
            ) : channels.length ? (
              <div className="space-y-3">
                {channels.map((channel) => (
                  <div key={channel.id} className="rounded-xl border border-slate-200 bg-white px-4 py-3">
                    <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
                      <div className="min-w-0">
                        <div className="flex flex-wrap items-center gap-2">
                          <p className="text-sm font-semibold text-slate-900">
                            {channel.channelName || channelLabel[channel.channelType]}
                          </p>
                          <Badge variant="outline">{channelLabel[channel.channelType]}</Badge>
                        </div>
                        <p className="mt-1 break-all text-xs text-slate-500">{channel.webhookMasked}</p>
                      </div>
                      <div className="flex gap-2">
                        <Button size="sm" variant="outline" onClick={() => void testChannel(channel)} disabled={busyId === channel.id}>
                          {busyId === channel.id ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <CheckCircle2 className="h-4 w-4" />}
                          测试
                        </Button>
                        <Button size="icon" variant="outline" onClick={() => void removeChannel(channel)} disabled={busyId === channel.id}>
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <EmptyPanel title="还没有通知渠道" description="添加 webhook 后，新 Issue 和 Diff 生成结果会主动推送。" />
            )}
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader className="gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <CardTitle className="section-title">Issue 事件</CardTitle>
            <p className="section-subtitle mt-1">自动拉取到的新 Issue 会显示在这里，你可以选择执行修复或忽略。</p>
          </div>
          <div className="flex flex-wrap gap-3">
            <Select value={status} onValueChange={(value) => setStatus(value as GitHubIssueEventStatus | "ALL")}>
              <SelectTrigger className="w-[160px]">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {statusOptions.map((option) => (
                  <SelectItem key={option.value} value={option.value}>
                    {option.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Button variant="outline" onClick={() => void loadEvents()} disabled={refreshing}>
              {refreshing ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <RefreshCw className="h-4 w-4" />}
              刷新事件
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          {loading ? (
            <LoadingRows />
          ) : events.length ? (
            <div className="divide-y divide-slate-100 rounded-xl border border-slate-200">
              {events.map((issueEvent) => {
                const watch = watchMap.get(issueEvent.repoWatchId);
                const canRun = issueEvent.status === "NEW" || issueEvent.status === "NOTIFIED";
                const canIgnore = canRun;
                return (
                  <div key={issueEvent.id} className="space-y-3 p-4">
                    <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
                      <div className="min-w-0 space-y-2">
                        <div className="flex flex-wrap items-center gap-2">
                          <Badge>{statusLabel[issueEvent.status] || issueEvent.status}</Badge>
                          <Badge variant="outline">#{issueEvent.issueNumber}</Badge>
                          <span className="text-xs text-slate-500">
                            {watch ? `${watch.owner}/${watch.repoName}` : "未知仓库"}
                          </span>
                        </div>
                        <h3 className="break-words text-base font-semibold text-slate-950">{issueEvent.issueTitle}</h3>
                        <p className="line-clamp-2 text-sm leading-6 text-slate-500">
                          {issueEvent.issueBody || "这个 Issue 没有正文。"}
                        </p>
                        <div className="flex flex-wrap gap-x-4 gap-y-1 text-xs text-slate-500">
                          <span>作者：{issueEvent.senderLogin || "未知"}</span>
                          <span>发现时间：{formatDate(issueEvent.createdAt)}</span>
                          {issueEvent.agentTaskId ? <span>任务 ID：{issueEvent.agentTaskId}</span> : null}
                        </div>
                      </div>
                      <div className="flex shrink-0 flex-wrap gap-2">
                        {issueEvent.issueUrl ? (
                          <Button asChild size="sm" variant="outline">
                            <a href={issueEvent.issueUrl} target="_blank" rel="noreferrer">
                              <ExternalLink className="h-4 w-4" />
                              GitHub
                            </a>
                          </Button>
                        ) : null}
                        {issueEvent.agentTaskId ? (
                          <Button size="sm" variant="outline" onClick={() => navigate(`/tasks/${issueEvent.agentTaskId}`)}>
                            查看任务
                          </Button>
                        ) : null}
                        {canIgnore ? (
                          <Button size="sm" variant="outline" onClick={() => void ignoreEvent(issueEvent)} disabled={busyId === issueEvent.id}>
                            忽略
                          </Button>
                        ) : null}
                        {canRun ? (
                          <Button size="sm" onClick={() => void runEvent(issueEvent)} disabled={busyId === issueEvent.id}>
                            {busyId === issueEvent.id ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <Wrench className="h-4 w-4" />}
                            执行修复
                          </Button>
                        ) : null}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          ) : (
            <EmptyPanel title="暂无 Issue 事件" description="当轮询发现新的 open Issue 后，会在这里出现。" />
          )}
        </CardContent>
      </Card>
    </div>
  );
}

function parseGitHubRepoUrl(repoUrl: string) {
  const match = repoUrl.trim().match(/^https:\/\/github\.com\/([^/]+)\/([^/.]+)(?:\.git)?\/?$/);
  if (!match) {
    return null;
  }
  return { owner: match[1], repoName: match[2] };
}

function formatDate(value?: string | null) {
  if (!value) {
    return "--";
  }
  return new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

function LoadingRows() {
  return (
    <div className="space-y-3">
      {Array.from({ length: 3 }).map((_, index) => (
        <div key={index} className="h-20 animate-pulse rounded-xl bg-slate-100" />
      ))}
    </div>
  );
}

function EmptyPanel({ title, description }: { title: string; description: string }) {
  return (
    <div className="rounded-xl border border-dashed border-slate-200 bg-slate-50 px-4 py-10 text-center">
      <p className="text-sm font-semibold text-slate-900">{title}</p>
      <p className="mt-2 text-sm text-slate-500">{description}</p>
    </div>
  );
}
