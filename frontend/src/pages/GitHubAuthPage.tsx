import { useEffect, useState } from "react";
import { ExternalLink, Github, LoaderCircle, ShieldCheck, Unplug } from "lucide-react";
import { disconnectGitHubAccount, getGitHubAccount, getGitHubAuthUrl } from "@/api/github-auth";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import type { GitHubAccount } from "@/types/github-auth";

export function GitHubAuthPage() {
  const [account, setAccount] = useState<GitHubAccount | null>(null);
  const [loading, setLoading] = useState(true);
  const [connecting, setConnecting] = useState(false);
  const [disconnecting, setDisconnecting] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    void loadAccount();
  }, []);

  async function loadAccount() {
    setLoading(true);
    setError("");
    try {
      setAccount(await getGitHubAccount());
    } catch (err) {
      setError(err instanceof Error ? err.message : "加载 GitHub 账号失败");
    } finally {
      setLoading(false);
    }
  }

  async function handleConnect() {
    setConnecting(true);
    setError("");
    try {
      const result = await getGitHubAuthUrl();
      window.location.assign(result.url);
    } catch (err) {
      setError(formatGitHubAuthError(err, "发起 GitHub 授权失败"));
      setConnecting(false);
    }
  }

  async function handleDisconnect() {
    if (!window.confirm("确定解绑当前 GitHub 账号吗？解绑后将无法继续导入仓库或提交 Pull Request。")) {
      return;
    }
    setDisconnecting(true);
    setError("");
    try {
      await disconnectGitHubAccount();
      await loadAccount();
    } catch (err) {
      setError(err instanceof Error ? err.message : "解绑 GitHub 账号失败");
    } finally {
      setDisconnecting(false);
    }
  }

  return (
    <div className="space-y-8">
      <section className="space-y-4">
        <h1 className="text-3xl font-extrabold text-slate-900">GitHub 授权</h1>
        <p className="max-w-3xl text-sm leading-7 text-slate-500">
          连接你的 GitHub 账号后，CodePilot 就能读取你授权的仓库列表，并在你确认 Patch 后以该账号身份提交 Pull Request。
        </p>
      </section>

      {error ? <div className="rounded-2xl bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div> : null}

      {loading ? (
        <div className="flex items-center justify-center py-16 text-slate-500">
          <LoaderCircle className="h-5 w-5 animate-spin" />
        </div>
      ) : account?.connected ? (
        <Card className="max-w-3xl">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <ShieldCheck className="h-5 w-5 text-emerald-600" />
              已连接 GitHub
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-5">
            <div className="flex items-center gap-4">
              {account.githubAvatarUrl ? (
                <img src={account.githubAvatarUrl} alt={account.githubLogin || "GitHub"} className="h-14 w-14 rounded-full" />
              ) : (
                <div className="flex h-14 w-14 items-center justify-center rounded-full bg-slate-100 text-slate-600">
                  <Github className="h-5 w-5" />
                </div>
              )}
              <div>
                <p className="text-lg font-semibold text-slate-900">{account.githubName || account.githubLogin}</p>
                <p className="text-sm text-slate-500">@{account.githubLogin}</p>
              </div>
            </div>

            <div className="grid gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4 text-sm text-slate-600">
              <div>
                <p className="text-xs uppercase tracking-wide text-slate-400">授权范围</p>
                <p className="mt-1 text-slate-700">{account.scope || "--"}</p>
              </div>
              <div className="flex flex-wrap gap-3">
                <Button variant="secondary" onClick={() => window.location.assign("/projects")}>
                  <ExternalLink className="h-4 w-4" />
                  去导入仓库
                </Button>
                <Button variant="outline" onClick={handleDisconnect} disabled={disconnecting}>
                  {disconnecting ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <Unplug className="h-4 w-4" />}
                  解绑 GitHub
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      ) : (
        <Card className="max-w-3xl">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Github className="h-5 w-5 text-slate-700" />
              连接 GitHub 账号
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-5">
            <p className="text-sm leading-7 text-slate-600">
              连接之后，你就可以把自己授权的仓库加入项目列表，并在任务完成后直接提交 Pull Request。
            </p>
            <Button onClick={handleConnect} disabled={connecting}>
              {connecting ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <Github className="h-4 w-4" />}
              连接 GitHub
            </Button>
          </CardContent>
        </Card>
      )}
    </div>
  );
}

function formatGitHubAuthError(err: unknown, fallback: string) {
  const message = err instanceof Error ? err.message : "";
  if (message.includes("GitHub OAuth") && message.includes("not configured")) {
    return "GitHub OAuth 尚未配置，请在后端设置 GITHUB_CLIENT_ID、GITHUB_CLIENT_SECRET 和 GITHUB_OAUTH_REDIRECT_URI 后重启服务。";
  }
  return message || fallback;
}
