import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { CheckCircle2, LoaderCircle, XCircle } from "lucide-react";
import { connectGitHubAccount } from "@/api/github-auth";
import { Button } from "@/components/ui/button";

export function GitHubCallbackPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const code = searchParams.get("code");
    const state = searchParams.get("state");
    if (!code || !state) {
      setLoading(false);
      setError("GitHub 回调参数不完整。");
      return;
    }

    connectGitHubAccount({ code, state })
      .then(() => {
        setLoading(false);
        setTimeout(() => {
          navigate("/github-auth", { replace: true });
        }, 1200);
      })
      .catch((err) => {
        setLoading(false);
        setError(err instanceof Error ? err.message : "GitHub 授权失败");
      });
  }, [navigate, searchParams]);

  return (
    <div className="flex min-h-[60vh] items-center justify-center">
      <div className="w-full max-w-lg rounded-[28px] border border-slate-200 bg-white px-8 py-10 text-center shadow-soft">
        {loading ? (
          <div className="space-y-4">
            <LoaderCircle className="mx-auto h-8 w-8 animate-spin text-slate-500" />
            <h1 className="text-xl font-semibold text-slate-900">正在完成 GitHub 授权</h1>
            <p className="text-sm text-slate-500">稍等一下，我们正在把你的 GitHub 账号绑定到 CodePilot。</p>
          </div>
        ) : error ? (
          <div className="space-y-4">
            <XCircle className="mx-auto h-8 w-8 text-red-500" />
            <h1 className="text-xl font-semibold text-slate-900">GitHub 授权失败</h1>
            <p className="text-sm text-slate-500">{error}</p>
            <Button onClick={() => navigate("/github-auth", { replace: true })}>返回 GitHub 授权页</Button>
          </div>
        ) : (
          <div className="space-y-4">
            <CheckCircle2 className="mx-auto h-8 w-8 text-emerald-500" />
            <h1 className="text-xl font-semibold text-slate-900">GitHub 账号已连接</h1>
            <p className="text-sm text-slate-500">马上带你回到 GitHub 授权页。</p>
          </div>
        )}
      </div>
    </div>
  );
}
