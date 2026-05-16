import { useEffect, useState } from "react";
import { BellRing, LoaderCircle, Trash2 } from "lucide-react";
import {
  deleteProjectSentryConfig,
  getProjectSentryConfig,
  saveProjectSentryConfig,
} from "@/api/sentry-config";
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
import type { SentryProjectMapping } from "@/types/sentry-config";

interface ProjectSentryConfigDialogProps {
  project: ProjectRepo;
}

export function ProjectSentryConfigDialog({ project }: ProjectSentryConfigDialogProps) {
  const [open, setOpen] = useState(false);
  const [config, setConfig] = useState<SentryProjectMapping | null>(null);
  const [organizationSlug, setOrganizationSlug] = useState("");
  const [projectSlug, setProjectSlug] = useState("");
  const [enabled, setEnabled] = useState(true);
  const [autoRunEnabled, setAutoRunEnabled] = useState(true);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    if (!open) {
      return;
    }
    setLoading(true);
    setMessage("");
    setError("");
    getProjectSentryConfig(project.id)
      .then((data) => {
        setConfig(data);
        setOrganizationSlug(data?.sentryOrganizationSlug || "");
        setProjectSlug(data?.sentryProjectSlug || project.repoName || "");
        setEnabled(data?.enabled ?? true);
        setAutoRunEnabled(data?.autoRunEnabled ?? true);
      })
      .catch((err) => setError(err instanceof Error ? err.message : "加载 Sentry 配置失败"))
      .finally(() => setLoading(false));
  }, [open, project.id, project.repoName]);

  async function handleSave() {
    if (!organizationSlug.trim()) {
      setError("请输入 Sentry organization slug");
      return;
    }
    if (!projectSlug.trim()) {
      setError("请输入 Sentry project slug");
      return;
    }

    setSaving(true);
    setMessage("");
    setError("");
    try {
      const saved = await saveProjectSentryConfig(project.id, {
        sentryOrganizationSlug: organizationSlug.trim(),
        sentryProjectSlug: projectSlug.trim(),
        enabled,
        autoRunEnabled,
      });
      setConfig(saved);
      setMessage("已保存 Sentry 映射");
    } catch (err) {
      setError(err instanceof Error ? err.message : "保存 Sentry 配置失败");
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete() {
    setDeleting(true);
    setMessage("");
    setError("");
    try {
      await deleteProjectSentryConfig(project.id);
      setConfig(null);
      setOrganizationSlug("");
      setProjectSlug(project.repoName || "");
      setEnabled(true);
      setAutoRunEnabled(true);
      setMessage("已移除 Sentry 映射");
    } catch (err) {
      setError(err instanceof Error ? err.message : "删除 Sentry 配置失败");
    } finally {
      setDeleting(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button variant="outline" size="sm">
          <BellRing className="h-4 w-4" />
          Sentry
        </Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Sentry 告警自动修复</DialogTitle>
          <DialogDescription>{project.repoName}</DialogDescription>
        </DialogHeader>

        {loading ? (
          <div className="flex items-center gap-2 py-6 text-sm text-slate-500">
            <LoaderCircle className="h-4 w-4 animate-spin" />
            加载中
          </div>
        ) : (
          <div className="space-y-4">
            <div className="grid gap-2">
              <label className="text-sm font-medium text-slate-700">Organization slug</label>
              <Input
                value={organizationSlug}
                onChange={(event) => setOrganizationSlug(event.target.value)}
                placeholder="your-sentry-org"
              />
            </div>

            <div className="grid gap-2">
              <label className="text-sm font-medium text-slate-700">Project slug</label>
              <Input
                value={projectSlug}
                onChange={(event) => setProjectSlug(event.target.value)}
                placeholder="codepilot-backend"
              />
            </div>

            <label className="flex items-center gap-3 rounded-lg border border-slate-200 px-3 py-2 text-sm text-slate-700">
              <input
                type="checkbox"
                checked={enabled}
                onChange={(event) => setEnabled(event.target.checked)}
                className="h-4 w-4 rounded border-slate-300"
              />
              启用这个 Sentry 映射
            </label>

            <label className="flex items-center gap-3 rounded-lg border border-slate-200 px-3 py-2 text-sm text-slate-700">
              <input
                type="checkbox"
                checked={autoRunEnabled}
                onChange={(event) => setAutoRunEnabled(event.target.checked)}
                className="h-4 w-4 rounded border-slate-300"
              />
              告警到达后自动运行修复任务
            </label>

            {message ? <div className="rounded-lg bg-emerald-50 px-3 py-2 text-sm text-emerald-700">{message}</div> : null}
            {error ? <div className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{error}</div> : null}
          </div>
        )}

        <DialogFooter>
          {config ? (
            <Button variant="outline" onClick={handleDelete} disabled={loading || saving || deleting}>
              {deleting ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <Trash2 className="h-4 w-4" />}
              移除
            </Button>
          ) : null}
          <Button onClick={handleSave} disabled={loading || saving || deleting}>
            {saving ? <LoaderCircle className="h-4 w-4 animate-spin" /> : null}
            保存
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
