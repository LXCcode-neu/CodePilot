import { Outlet } from "react-router-dom";

export function AuthLayout() {
  return (
    <div className="min-h-screen bg-transparent">
      <div className="mx-auto grid min-h-screen max-w-[1440px] gap-8 px-6 py-10 lg:grid-cols-[1.1fr_0.9fr]">
        <section className="hidden rounded-[32px] border border-white/60 bg-slate-900 px-10 py-12 text-white shadow-soft lg:flex lg:flex-col lg:justify-between">
          <div className="space-y-6">
            <span className="inline-flex rounded-full border border-white/15 px-4 py-1 text-xs uppercase tracking-[0.24em] text-slate-300">
              CodePilot
            </span>
            <div className="space-y-4">
              <h1 className="max-w-xl text-4xl font-extrabold leading-tight">
                Issue 到 Patch 的 Agent 工作台，适合演示真实代码修复流程。
              </h1>
              <p className="max-w-lg text-sm leading-7 text-slate-300">
                管理仓库、创建任务、观察 Agent 执行轨迹，并在一个简洁的界面里查看生成 Patch。
              </p>
            </div>
          </div>
          <div className="grid gap-4">
            <div className="rounded-2xl border border-white/10 bg-white/5 p-5">
              <p className="text-sm font-semibold text-white">多语言仓库入口</p>
              <p className="mt-2 text-sm text-slate-300">从 GitHub 仓库接入到执行追踪，形成完整演示链路。</p>
            </div>
            <div className="rounded-2xl border border-white/10 bg-white/5 p-5">
              <p className="text-sm font-semibold text-white">实时任务视图</p>
              <p className="mt-2 text-sm text-slate-300">聚焦状态、步骤、风险和 Patch，适合简历项目讲解。</p>
            </div>
          </div>
        </section>
        <section className="flex items-center justify-center">
          <div className="w-full max-w-xl">
            <Outlet />
          </div>
        </section>
      </div>
    </div>
  );
}
