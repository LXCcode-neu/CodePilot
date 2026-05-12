import { useEffect, useMemo, useState } from "react";
import { LoaderCircle, PlugZap, Settings } from "lucide-react";
import {
  getLlmProviders,
  getProjectLlmConfig,
  saveProjectLlmConfig,
  testProjectLlmConfig,
} from "@/api/llm-config";
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
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import type { LlmProvider, ProjectLlmConfig } from "@/types/llm-config";
import type { ProjectRepo } from "@/types/project";

interface ProjectLlmConfigDialogProps {
  project: ProjectRepo;
  onSaved?: () => void;
}

export function ProjectLlmConfigDialog({ project, onSaved }: ProjectLlmConfigDialogProps) {
  const [open, setOpen] = useState(false);
  const [providers, setProviders] = useState<LlmProvider[]>([]);
  const [config, setConfig] = useState<ProjectLlmConfig | null>(null);
  const [provider, setProvider] = useState("");
  const [modelName, setModelName] = useState("");
  const [remark, setRemark] = useState("");
  const [websiteUrl, setWebsiteUrl] = useState("");
  const [baseUrl, setBaseUrl] = useState("");
  const [apiKey, setApiKey] = useState("");
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [testing, setTesting] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const selectedProvider = useMemo(
    () => providers.find((item) => item.provider === provider),
    [provider, providers]
  );

  useEffect(() => {
    if (!open) {
      return;
    }

    setLoading(true);
    setError("");
    setMessage("");
    Promise.all([getLlmProviders(), getProjectLlmConfig(project.id)])
      .then(([availableProviders, projectConfig]) => {
        setProviders(availableProviders);
        setConfig(projectConfig);
        const initialProvider = projectConfig
          ? availableProviders.find((item) => item.provider === projectConfig.provider)
          : availableProviders[0];
        setProvider(projectConfig?.provider || initialProvider?.provider || "");
        setModelName(projectConfig?.modelName || "");
        setRemark(projectConfig?.displayName || "");
        setWebsiteUrl("");
        setBaseUrl(projectConfig?.baseUrl || initialProvider?.defaultBaseUrl || "");
        setApiKey("");
      })
      .catch((err) => setError(err instanceof Error ? err.message : "加载模型配置失败"))
      .finally(() => setLoading(false));
  }, [open, project.id]);

  function handleProviderChange(value: string) {
    const nextProvider = providers.find((item) => item.provider === value);
    setProvider(value);
    setModelName("");
    setRemark("");
    setWebsiteUrl("");
    setBaseUrl(nextProvider?.defaultBaseUrl || "");
  }

  async function handleSave() {
    if (!provider) {
      setError("请选择供应商");
      return;
    }
    if (!modelName.trim()) {
      setError("请输入模型名称");
      return;
    }
    if (!baseUrl.trim()) {
      setError("请输入请求地址");
      return;
    }

    setSaving(true);
    setError("");
    setMessage("");
    try {
      const saved = await saveProjectLlmConfig(project.id, {
        provider,
        modelName: modelName.trim(),
        displayName: remark.trim() || modelName.trim(),
        baseUrl: baseUrl.trim(),
        apiKey: apiKey.trim() || undefined,
      });
      setConfig(saved);
      setApiKey("");
      setMessage("已保存");
      onSaved?.();
    } catch (err) {
      setError(err instanceof Error ? err.message : "保存模型配置失败");
    } finally {
      setSaving(false);
    }
  }

  async function handleTest() {
    setTesting(true);
    setError("");
    setMessage("");
    try {
      const result = await testProjectLlmConfig(project.id);
      if (result.success) {
        setMessage(result.message || "连接成功");
      } else {
        setError(result.message || "连接失败");
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "连接失败");
    } finally {
      setTesting(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button variant="outline" size="sm">
          <Settings className="h-4 w-4" />
          配置模型 / API Key
        </Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>配置模型 / API Key</DialogTitle>
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
              <label className="text-sm font-medium text-slate-700">供应商</label>
              <Select value={provider} onValueChange={handleProviderChange}>
                <SelectTrigger>
                  <SelectValue placeholder="选择供应商" />
                </SelectTrigger>
                <SelectContent>
                  {providers.map((item) => (
                    <SelectItem key={item.provider} value={item.provider}>
                      {item.displayName}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="grid gap-4 md:grid-cols-2">
              <div className="grid gap-2">
                <label className="text-sm font-medium text-slate-700">模型名称</label>
                <Input value={modelName} onChange={(event) => setModelName(event.target.value)} placeholder="例如：glm-4-flash" />
              </div>
              <div className="grid gap-2">
                <label className="text-sm font-medium text-slate-700">备注</label>
                <Input value={remark} onChange={(event) => setRemark(event.target.value)} placeholder="例如：公司专用账号" />
              </div>
            </div>

            <div className="grid gap-2">
              <label className="text-sm font-medium text-slate-700">官网链接</label>
              <Input value={websiteUrl} onChange={(event) => setWebsiteUrl(event.target.value)} placeholder="https://example.com（可选）" />
            </div>

            <div className="grid gap-2">
              <label className="text-sm font-medium text-slate-700">API Key</label>
              <Input
                value={apiKey}
                onChange={(event) => setApiKey(event.target.value)}
                placeholder={config?.hasApiKey ? config.apiKeyMask || "已配置，留空则不修改" : "请输入 API Key"}
                type="password"
              />
            </div>

            <div className="grid gap-2">
              <label className="text-sm font-medium text-slate-700">请求地址</label>
              <Input value={baseUrl} onChange={(event) => setBaseUrl(event.target.value)} placeholder="https://your-api-endpoint.com" />
            </div>

            {config?.hasApiKey ? (
              <div className="rounded-lg bg-slate-50 px-3 py-2 text-xs text-slate-500">
                API Key：{config.apiKeyMask || "已配置"}
              </div>
            ) : null}

            {selectedProvider && !selectedProvider.supportsTools ? (
              <div className="rounded-lg bg-amber-50 px-3 py-2 text-sm text-amber-700">
                当前供应商可能不支持 Agent 工具调用。
              </div>
            ) : null}

            {message ? <div className="rounded-lg bg-emerald-50 px-3 py-2 text-sm text-emerald-700">{message}</div> : null}
            {error ? <div className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{error}</div> : null}
          </div>
        )}

        <DialogFooter>
          <Button variant="outline" onClick={handleTest} disabled={loading || testing || saving || !config?.hasApiKey}>
            {testing ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <PlugZap className="h-4 w-4" />}
            测试连接
          </Button>
          <Button onClick={handleSave} disabled={loading || saving || testing}>
            {saving ? <LoaderCircle className="h-4 w-4 animate-spin" /> : null}
            保存
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
