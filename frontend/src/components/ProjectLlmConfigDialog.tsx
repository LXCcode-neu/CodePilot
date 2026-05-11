import { useEffect, useMemo, useState } from "react";
import { LoaderCircle, PlugZap, Settings } from "lucide-react";
import {
  getAvailableLlmModels,
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
import type { LlmAvailableModel, ProjectLlmConfig } from "@/types/llm-config";
import type { ProjectRepo } from "@/types/project";

interface ProjectLlmConfigDialogProps {
  project: ProjectRepo;
  onSaved?: () => void;
}

export function ProjectLlmConfigDialog({ project, onSaved }: ProjectLlmConfigDialogProps) {
  const [open, setOpen] = useState(false);
  const [models, setModels] = useState<LlmAvailableModel[]>([]);
  const [config, setConfig] = useState<ProjectLlmConfig | null>(null);
  const [provider, setProvider] = useState("");
  const [modelName, setModelName] = useState("");
  const [baseUrl, setBaseUrl] = useState("");
  const [apiKey, setApiKey] = useState("");
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [testing, setTesting] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const providerModels = useMemo(
    () => models.filter((model) => model.provider === provider),
    [models, provider]
  );

  const selectedModel = useMemo(
    () => models.find((model) => model.provider === provider && model.modelName === modelName),
    [modelName, models, provider]
  );

  useEffect(() => {
    if (!open) {
      return;
    }

    setLoading(true);
    setError("");
    setMessage("");
    Promise.all([getAvailableLlmModels(), getProjectLlmConfig(project.id)])
      .then(([availableModels, projectConfig]) => {
        setModels(availableModels);
        setConfig(projectConfig);
        const initialModel =
          projectConfig
            ? availableModels.find(
                (model) => model.provider === projectConfig.provider && model.modelName === projectConfig.modelName
              )
            : availableModels[0];
        setProvider(projectConfig?.provider || initialModel?.provider || "");
        setModelName(projectConfig?.modelName || initialModel?.modelName || "");
        setBaseUrl(projectConfig?.baseUrl || initialModel?.defaultBaseUrl || "");
        setApiKey("");
      })
      .catch((err) => setError(err instanceof Error ? err.message : "加载模型配置失败"))
      .finally(() => setLoading(false));
  }, [open, project.id]);

  function handleProviderChange(value: string) {
    const nextModel = models.find((model) => model.provider === value);
    setProvider(value);
    setModelName(nextModel?.modelName || "");
    setBaseUrl(nextModel?.defaultBaseUrl || "");
  }

  function handleModelChange(value: string) {
    const nextModel = models.find((model) => model.provider === provider && model.modelName === value);
    setModelName(value);
    setBaseUrl(nextModel?.defaultBaseUrl || baseUrl);
  }

  async function handleSave() {
    if (!selectedModel) {
      setError("请选择模型");
      return;
    }

    setSaving(true);
    setError("");
    setMessage("");
    try {
      const saved = await saveProjectLlmConfig(project.id, {
        provider,
        modelName,
        displayName: selectedModel.displayName,
        baseUrl,
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
              <label className="text-sm font-medium text-slate-700">模型供应商</label>
              <Select value={provider} onValueChange={handleProviderChange}>
                <SelectTrigger>
                  <SelectValue placeholder="选择模型供应商" />
                </SelectTrigger>
                <SelectContent>
                  {[...new Set(models.map((model) => model.provider))].map((item) => (
                    <SelectItem key={item} value={item}>
                      {item}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="grid gap-2">
              <label className="text-sm font-medium text-slate-700">模型</label>
              <Select value={modelName} onValueChange={handleModelChange}>
                <SelectTrigger>
                  <SelectValue placeholder="选择模型" />
                </SelectTrigger>
                <SelectContent>
                  {providerModels.map((model) => (
                    <SelectItem key={`${model.provider}:${model.modelName}`} value={model.modelName}>
                      {model.displayName}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="grid gap-2">
              <label className="text-sm font-medium text-slate-700">Base URL</label>
              <Input value={baseUrl} onChange={(event) => setBaseUrl(event.target.value)} />
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

            {config?.hasApiKey ? (
              <div className="rounded-lg bg-slate-50 px-3 py-2 text-xs text-slate-500">
                API Key：{config.apiKeyMask || "已配置"}
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
