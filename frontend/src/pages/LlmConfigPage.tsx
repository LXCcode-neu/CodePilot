import { useEffect, useMemo, useState } from "react";
import { CheckCircle2, KeyRound, LoaderCircle, Plus, RefreshCw } from "lucide-react";
import {
  applyLlmApiKey,
  createLlmApiKey,
  getAvailableLlmModels,
  listLlmApiKeys,
} from "@/api/llm-config";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { formatDateTime } from "@/lib/utils";
import type { LlmApiKey, LlmAvailableModel } from "@/types/llm-config";

export function LlmConfigPage() {
  const [models, setModels] = useState<LlmAvailableModel[]>([]);
  const [apiKeys, setApiKeys] = useState<LlmApiKey[]>([]);
  const [open, setOpen] = useState(false);
  const [name, setName] = useState("");
  const [provider, setProvider] = useState("");
  const [modelName, setModelName] = useState("");
  const [baseUrl, setBaseUrl] = useState("");
  const [apiKey, setApiKey] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [applyingId, setApplyingId] = useState<string | null>(null);
  const [error, setError] = useState("");

  const providerModels = useMemo(() => models.filter((model) => model.provider === provider), [models, provider]);
  const selectedModel = useMemo(
    () => models.find((model) => model.provider === provider && model.modelName === modelName),
    [modelName, models, provider]
  );

  useEffect(() => {
    void loadData();
  }, []);

  async function loadData() {
    setLoading(true);
    setError("");
    try {
      const [availableModels, keys] = await Promise.all([getAvailableLlmModels(), listLlmApiKeys()]);
      setModels(availableModels);
      setApiKeys(keys);
      primeForm(availableModels);
    } catch (err) {
      setError(err instanceof Error ? err.message : "加载 API Key 失败");
    } finally {
      setLoading(false);
    }
  }

  function primeForm(availableModels = models) {
    const initialModel = availableModels[0];
    setName("");
    setProvider(initialModel?.provider || "");
    setModelName(initialModel?.modelName || "");
    setBaseUrl(initialModel?.defaultBaseUrl || "");
    setApiKey("");
  }

  function handleOpenChange(nextOpen: boolean) {
    setOpen(nextOpen);
    setError("");
    if (nextOpen) {
      primeForm();
    }
  }

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

  async function handleCreate() {
    if (!selectedModel) {
      setError("请选择模型");
      return;
    }
    if (!name.trim()) {
      setError("请输入名称");
      return;
    }
    if (!apiKey.trim()) {
      setError("请输入 API Key");
      return;
    }

    setSaving(true);
    setError("");
    try {
      await createLlmApiKey({
        name: name.trim(),
        provider,
        modelName,
        displayName: selectedModel.displayName,
        baseUrl,
        apiKey: apiKey.trim(),
      });
      setOpen(false);
      await loadData();
    } catch (err) {
      setError(err instanceof Error ? err.message : "创建 API Key 失败");
    } finally {
      setSaving(false);
    }
  }

  async function handleApply(id: string) {
    setApplyingId(id);
    setError("");
    try {
      await applyLlmApiKey(id);
      await loadData();
    } catch (err) {
      setError(err instanceof Error ? err.message : "应用 API Key 失败");
    } finally {
      setApplyingId(null);
    }
  }

  return (
    <div className="space-y-8">
      <section className="space-y-4">
        <h1 className="text-3xl font-bold text-slate-900">API keys</h1>
        <p className="max-w-5xl text-sm leading-7 text-slate-600">
          列表内是你的全部 API key。API key 仅在创建时提交，保存后只显示脱敏首尾。选择“应用”后，后续任务会使用该 key。
        </p>
      </section>

      <section className="max-w-5xl space-y-6">
        <div className="overflow-hidden rounded-xl bg-slate-50">
          <table className="w-full table-fixed text-left text-sm">
            <thead className="border-b border-slate-200 text-slate-500">
              <tr>
                <th className="w-[18%] px-4 py-4 font-semibold">名称</th>
                <th className="w-[15%] px-4 py-4 font-semibold">厂商</th>
                <th className="w-[25%] px-4 py-4 font-semibold">Key</th>
                <th className="w-[16%] px-4 py-4 font-semibold">创建日期</th>
                <th className="w-[16%] px-4 py-4 font-semibold">最近使用日期</th>
                <th className="w-[10%] px-4 py-4 text-right font-semibold">操作</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-slate-700">
              {loading ? (
                <tr>
                  <td colSpan={6} className="px-4 py-8 text-center text-slate-500">
                    <LoaderCircle className="mx-auto h-5 w-5 animate-spin" />
                  </td>
                </tr>
              ) : apiKeys.length ? (
                apiKeys.map((item) => (
                  <tr key={item.id}>
                    <td className="px-4 py-3 font-medium text-slate-900">{item.name}</td>
                    <td className="px-4 py-3">{item.provider}</td>
                    <td className="truncate px-4 py-3 font-mono">{item.apiKeyMask}</td>
                    <td className="px-4 py-3">{formatDate(item.createdAt)}</td>
                    <td className="px-4 py-3">{item.lastUsedAt ? formatDate(item.lastUsedAt) : "-"}</td>
                    <td className="px-4 py-3">
                      <div className="flex justify-end">
                        {item.active ? (
                          <span className="inline-flex items-center gap-1 rounded-md bg-emerald-50 px-2 py-1 text-xs font-medium text-emerald-700">
                            <CheckCircle2 className="h-3.5 w-3.5" />
                            应用中
                          </span>
                        ) : (
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleApply(item.id)}
                            disabled={applyingId === item.id}
                          >
                            {applyingId === item.id ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <RefreshCw className="h-4 w-4" />}
                            应用
                          </Button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={6} className="px-4 py-8 text-center text-slate-500">
                    暂无 API Key
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {error ? <div className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{error}</div> : null}

        <Button onClick={() => handleOpenChange(true)}>
          <Plus className="h-4 w-4" />
          创建 API key
        </Button>
      </section>

      <Dialog open={open} onOpenChange={handleOpenChange}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>创建 API key</DialogTitle>
            <DialogDescription>API Key 保存后将只显示脱敏首尾。</DialogDescription>
          </DialogHeader>

          <div className="space-y-4">
            <div className="grid gap-2">
              <label className="text-sm font-medium text-slate-700">名称</label>
              <Input value={name} onChange={(event) => setName(event.target.value)} placeholder="例如 Codepilot" />
            </div>

            <div className="grid gap-2">
              <label className="text-sm font-medium text-slate-700">厂商</label>
              <Select value={provider} onValueChange={handleProviderChange}>
                <SelectTrigger>
                  <SelectValue placeholder="选择厂商" />
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
              <Input type="password" value={apiKey} onChange={(event) => setApiKey(event.target.value)} placeholder="sk-..." />
            </div>

            {error ? <div className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{error}</div> : null}
          </div>

          <DialogFooter>
            <Button onClick={handleCreate} disabled={saving}>
              {saving ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <KeyRound className="h-4 w-4" />}
              创建 API key
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

function formatDate(value?: string | null) {
  if (!value) {
    return "-";
  }
  return formatDateTime(value).slice(0, 10);
}
