import { useEffect, useMemo, useState } from "react";
import { CheckCircle2, KeyRound, LoaderCircle, Plus, RefreshCw } from "lucide-react";
import {
  applyLlmApiKey,
  createLlmApiKey,
  getLlmProviders,
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
import type { LlmApiKey, LlmProvider } from "@/types/llm-config";

export function LlmConfigPage() {
  const [providers, setProviders] = useState<LlmProvider[]>([]);
  const [apiKeys, setApiKeys] = useState<LlmApiKey[]>([]);
  const [open, setOpen] = useState(false);
  const [name, setName] = useState("");
  const [provider, setProvider] = useState("");
  const [modelName, setModelName] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [baseUrl, setBaseUrl] = useState("");
  const [apiKey, setApiKey] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [applyingId, setApplyingId] = useState<string | null>(null);
  const [error, setError] = useState("");

  const selectedProvider = useMemo(
    () => providers.find((item) => item.provider === provider),
    [provider, providers]
  );

  useEffect(() => {
    void loadData();
  }, []);

  async function loadData() {
    setLoading(true);
    setError("");
    try {
      const [availableProviders, keys] = await Promise.all([
        getLlmProviders(),
        listLlmApiKeys(),
      ]);
      setProviders(availableProviders);
      setApiKeys(keys);
      primeForm(availableProviders);
    } catch (err) {
      setError(err instanceof Error ? err.message : "加载 API Key 失败");
    } finally {
      setLoading(false);
    }
  }

  function primeForm(availableProviders = providers) {
    const initialProvider = availableProviders[0];
    setName("");
    setProvider(initialProvider?.provider || "");
    setModelName("");
    setDisplayName("");
    setBaseUrl(initialProvider?.defaultBaseUrl || "");
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
    const nextProvider = providers.find((item) => item.provider === value);
    setProvider(value);
    setModelName("");
    setDisplayName("");
    setBaseUrl(nextProvider?.defaultBaseUrl || "");
  }

  async function handleCreate() {
    if (!provider) {
      setError("请选择模型厂商");
      return;
    }
    if (!name.trim()) {
      setError("请输入名称");
      return;
    }
    if (!modelName.trim()) {
      setError("请输入模型名称");
      return;
    }
    if (!baseUrl.trim()) {
      setError("请输入 Base URL");
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
        modelName: modelName.trim(),
        displayName: displayName.trim() || modelName.trim(),
        baseUrl: baseUrl.trim(),
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
          列表内是你的全部 API Key。API Key 仅在创建时提交，保存后只显示脱敏信息；选择“应用”后，后续任务会使用该配置。
        </p>
      </section>

      <section className="max-w-5xl space-y-6">
        <div className="overflow-hidden rounded-xl bg-slate-50">
          <table className="w-full table-fixed text-left text-sm">
            <thead className="border-b border-slate-200 text-slate-500">
              <tr>
                <th className="w-[18%] px-4 py-4 font-semibold">名称</th>
                <th className="w-[14%] px-4 py-4 font-semibold">厂商</th>
                <th className="w-[18%] px-4 py-4 font-semibold">模型</th>
                <th className="w-[22%] px-4 py-4 font-semibold">Key</th>
                <th className="w-[16%] px-4 py-4 font-semibold">创建日期</th>
                <th className="w-[12%] px-4 py-4 text-right font-semibold">操作</th>
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
                    <td className="truncate px-4 py-3">{item.modelName}</td>
                    <td className="truncate px-4 py-3 font-mono">{item.apiKeyMask}</td>
                    <td className="px-4 py-3">{formatDate(item.createdAt)}</td>
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
          创建 API Key
        </Button>
      </section>

      <Dialog open={open} onOpenChange={handleOpenChange}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>创建 API Key</DialogTitle>
            <DialogDescription>选择供应商后，请手动填写请求地址、模型名称与 API Key。</DialogDescription>
          </DialogHeader>

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
                <label className="text-sm font-medium text-slate-700">供应商名称</label>
                <Input value={name} onChange={(event) => setName(event.target.value)} placeholder="例如：OpenAI 官方" />
              </div>
              <div className="grid gap-2">
                <label className="text-sm font-medium text-slate-700">备注</label>
                <Input value={displayName} onChange={(event) => setDisplayName(event.target.value)} placeholder="例如：公司专用账号" />
              </div>
            </div>

            <div className="grid gap-2">
              <label className="text-sm font-medium text-slate-700">官网链接</label>
              <Input placeholder="https://example.com（可选）" />
            </div>

            <div className="grid gap-2">
              <label className="text-sm font-medium text-slate-700">API Key</label>
              <Input type="password" value={apiKey} onChange={(event) => setApiKey(event.target.value)} placeholder="请输入 API Key" />
            </div>

            <div className="grid gap-2">
              <label className="text-sm font-medium text-slate-700">请求地址</label>
              <Input value={baseUrl} onChange={(event) => setBaseUrl(event.target.value)} placeholder="https://your-api-endpoint.com" />
            </div>

            <div className="grid gap-2">
              <label className="text-sm font-medium text-slate-700">模型名称</label>
              <Input value={modelName} onChange={(event) => setModelName(event.target.value)} placeholder="请输入厂商支持的模型 ID" />
            </div>

            {selectedProvider && !selectedProvider.supportsTools ? (
              <div className="rounded-lg bg-amber-50 px-3 py-2 text-sm text-amber-700">
                当前厂商可能不支持 Agent 工具调用。
              </div>
            ) : null}

            {error ? <div className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{error}</div> : null}
          </div>

          <DialogFooter>
            <Button onClick={handleCreate} disabled={saving}>
              {saving ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <KeyRound className="h-4 w-4" />}
              创建 API Key
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
