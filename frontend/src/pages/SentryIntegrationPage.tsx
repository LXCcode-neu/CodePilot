import { useEffect, useMemo, useState } from "react";
import { BellRing, CheckCircle2, LoaderCircle, Save, Trash2 } from "lucide-react";
import { getProjects } from "@/api/project";
import {
  deleteProjectSentryConfig,
  getProjectSentryConfig,
  saveProjectSentryConfig,
} from "@/api/sentry-config";
import { EmptyState } from "@/components/EmptyState";
import { LoadingBlock } from "@/components/LoadingBlock";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import type { ProjectRepo } from "@/types/project";
import type { SentryProjectMapping } from "@/types/sentry-config";

type MappingForm = {
  config: SentryProjectMapping | null;
  organizationSlug: string;
  projectSlug: string;
  enabled: boolean;
  autoRunEnabled: boolean;
  loading: boolean;
  saving: boolean;
  deleting: boolean;
  message: string;
  error: string;
};

function emptyForm(project?: ProjectRepo): MappingForm {
  return {
    config: null,
    organizationSlug: "",
    projectSlug: project?.repoName || "",
    enabled: true,
    autoRunEnabled: true,
    loading: true,
    saving: false,
    deleting: false,
    message: "",
    error: "",
  };
}

export function SentryIntegrationPage() {
  const [projects, setProjects] = useState<ProjectRepo[]>([]);
  const [forms, setForms] = useState<Record<string, MappingForm>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    void loadPage();
  }, []);

  async function loadPage() {
    setLoading(true);
    setError("");
    try {
      const projectData = await getProjects();
      setProjects(projectData);
      setForms(Object.fromEntries(projectData.map((project) => [project.id, emptyForm(project)])));
      await Promise.all(projectData.map((project) => loadMapping(project)));
    } catch (err) {
      setError(err instanceof Error ? err.message : "加载 Sentry 集成配置失败");
    } finally {
      setLoading(false);
    }
  }

  async function loadMapping(project: ProjectRepo) {
    try {
      const config = await getProjectSentryConfig(project.id);
      setForms((prev) => ({
        ...prev,
        [project.id]: {
          ...emptyForm(project),
          config,
          organizationSlug: config?.sentryOrganizationSlug || "",
          projectSlug: config?.sentryProjectSlug || project.repoName || "",
          enabled: config?.enabled ?? true,
          autoRunEnabled: config?.autoRunEnabled ?? true,
          loading: false,
        },
      }));
    } catch (err) {
      setForms((prev) => ({
        ...prev,
        [project.id]: {
          ...emptyForm(project),
          loading: false,
          error: err instanceof Error ? err.message : "加载映射失败",
        },
      }));
    }
  }

  function updateForm(projectId: string, patch: Partial<MappingForm>) {
    setForms((prev) => ({
      ...prev,
      [projectId]: {
        ...prev[projectId],
        ...patch,
        message: patch.message ?? "",
        error: patch.error ?? "",
      },
    }));
  }

  async function handleSave(project: ProjectRepo) {
    const form = forms[project.id] ?? emptyForm(project);
    if (!form.organizationSlug.trim()) {
      updateForm(project.id, { error: "请输入 Sentry organization slug" });
      return;
    }
    if (!form.projectSlug.trim()) {
      updateForm(project.id, { error: "请输入 Sentry project slug" });
      return;
    }

    updateForm(project.id, { saving: true });
    try {
      const saved = await saveProjectSentryConfig(project.id, {
        sentryOrganizationSlug: form.organizationSlug.trim(),
        sentryProjectSlug: form.projectSlug.trim(),
        enabled: form.enabled,
        autoRunEnabled: form.autoRunEnabled,
      });
      updateForm(project.id, { config: saved, saving: false, message: "已保存 Sentry 映射" });
    } catch (err) {
      updateForm(project.id, {
        saving: false,
        error: err instanceof Error ? err.message : "保存 Sentry 映射失败",
      });
    }
  }

  async function handleDelete(project: ProjectRepo) {
    updateForm(project.id, { deleting: true });
    try {
      await deleteProjectSentryConfig(project.id);
      updateForm(project.id, {
        ...emptyForm(project),
        loading: false,
        message: "已移除 Sentry 映射",
      });
    } catch (err) {
      updateForm(project.id, {
        deleting: false,
        error: err instanceof Error ? err.message : "移除 Sentry 映射失败",
      });
    }
  }

  const mappedCount = useMemo(
    () => Object.values(forms).filter((form) => form.config && form.enabled).length,
    [forms]
  );

  if (loading) {
    return <LoadingBlock lines={8} />;
  }

  if (error) {
    return <EmptyState title="加载失败" description={error} />;
  }

  return (
    <div className="space-y-8">
      <section className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <div>
          <h1 className="text-3xl font-extrabold text-slate-900">Sentry 集成</h1>
          <p className="mt-2 max-w-4xl text-sm leading-7 text-slate-500">
            把 Sentry project slug 映射到 CodePilot 仓库。Sentry 告警打到后端 webhook 后，会按这里的映射创建自动修复任务。
          </p>
        </div>
        <div className="rounded-2xl bg-slate-100 px-4 py-3 text-sm font-semibold text-slate-700">
          已启用 {mappedCount} / {projects.length}
        </div>
      </section>

      <Card className="border-emerald-100 bg-emerald-50/60 shadow-none">
        <CardContent className="flex items-start gap-3 p-4 text-sm leading-7 text-emerald-800">
          <BellRing className="mt-1 h-4 w-4 shrink-0" />
          <div>
            <p className="font-semibold">Sentry webhook 地址</p>
            <p className="break-all font-mono text-xs">POST /api/sentry/alerts</p>
            <p className="mt-1">请求头需要携带 X-CodePilot-Sentry-Token，token 在后端配置中维护，不会暴露给前端。</p>
          </div>
        </CardContent>
      </Card>

      {projects.length ? (
        <div className="grid gap-5 xl:grid-cols-2">
          {projects.map((project) => {
            const form = forms[project.id] ?? emptyForm(project);
            return (
              <Card key={project.id} className="shadow-none">
                <CardHeader className="space-y-2">
                  <div className="flex items-start justify-between gap-4">
                    <div>
                      <CardTitle className="text-base">{project.repoName}</CardTitle>
                      <p className="mt-1 break-all text-xs text-slate-500">{project.repoUrl}</p>
                    </div>
                    {form.config && form.enabled ? (
                      <span className="inline-flex items-center gap-1 rounded-full bg-emerald-50 px-3 py-1 text-xs font-semibold text-emerald-700">
                        <CheckCircle2 className="h-3.5 w-3.5" />
                        已启用
                      </span>
                    ) : (
                      <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-500">未启用</span>
                    )}
                  </div>
                </CardHeader>
                <CardContent className="space-y-4">
                  {form.loading ? (
                    <div className="flex items-center gap-2 py-8 text-sm text-slate-500">
                      <LoaderCircle className="h-4 w-4 animate-spin" />
                      加载映射中
                    </div>
                  ) : (
                    <>
                      <div className="grid gap-3 md:grid-cols-2">
                        <div className="grid gap-2">
                          <label className="text-sm font-medium text-slate-700">Organization slug</label>
                          <Input
                            value={form.organizationSlug}
                            onChange={(event) => updateForm(project.id, { organizationSlug: event.target.value })}
                            placeholder="your-sentry-org"
                          />
                        </div>
                        <div className="grid gap-2">
                          <label className="text-sm font-medium text-slate-700">Project slug</label>
                          <Input
                            value={form.projectSlug}
                            onChange={(event) => updateForm(project.id, { projectSlug: event.target.value })}
                            placeholder="codepilot-backend"
                          />
                        </div>
                      </div>

                      <div className="grid gap-2 text-sm text-slate-700">
                        <label className="flex items-center gap-3 rounded-lg border border-slate-200 px-3 py-2">
                          <input
                            type="checkbox"
                            checked={form.enabled}
                            onChange={(event) => updateForm(project.id, { enabled: event.target.checked })}
                            className="h-4 w-4 rounded border-slate-300"
                          />
                          启用 Sentry 映射
                        </label>
                        <label className="flex items-center gap-3 rounded-lg border border-slate-200 px-3 py-2">
                          <input
                            type="checkbox"
                            checked={form.autoRunEnabled}
                            onChange={(event) => updateForm(project.id, { autoRunEnabled: event.target.checked })}
                            className="h-4 w-4 rounded border-slate-300"
                          />
                          告警到达后自动运行修复任务
                        </label>
                      </div>

                      {form.message ? <div className="rounded-lg bg-emerald-50 px-3 py-2 text-sm text-emerald-700">{form.message}</div> : null}
                      {form.error ? <div className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{form.error}</div> : null}

                      <div className="flex flex-wrap gap-2">
                        <Button onClick={() => void handleSave(project)} disabled={form.saving || form.deleting}>
                          {form.saving ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
                          保存映射
                        </Button>
                        {form.config ? (
                          <Button variant="outline" onClick={() => void handleDelete(project)} disabled={form.saving || form.deleting}>
                            {form.deleting ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <Trash2 className="h-4 w-4" />}
                            移除映射
                          </Button>
                        ) : null}
                      </div>
                    </>
                  )}
                </CardContent>
              </Card>
            );
          })}
        </div>
      ) : (
        <EmptyState title="还没有仓库" description="先添加一个仓库，再配置它和 Sentry project 的映射。" />
      )}
    </div>
  );
}
