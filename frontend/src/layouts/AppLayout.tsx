import { useEffect, useState } from "react";
import { Link, NavLink, Outlet } from "react-router-dom";
import {
  BellRing,
  Bot,
  FolderGit2,
  Github,
  LayoutDashboard,
  LogOut,
  PlusSquare,
  Settings,
  ShieldCheck,
  UserRound,
} from "lucide-react";
import { getCurrentUser } from "@/api/auth";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { clearToken } from "@/lib/token";
import type { User } from "@/types/auth";

const navItems = [
  { to: "/", label: "首页概览", icon: LayoutDashboard, end: true },
  { to: "/github-auth", label: "GitHub 授权", icon: ShieldCheck, end: true },
  { to: "/github-issues", label: "GitHub Issue", icon: Github, end: true },
  { to: "/issue-automation", label: "Issue 自动修复", icon: BellRing, end: true },
  { to: "/sentry", label: "Sentry 集成", icon: BellRing, end: true },
  { to: "/projects", label: "仓库管理", icon: FolderGit2, end: true },
  { to: "/llm-config", label: "模型配置", icon: Settings, end: true },
  { to: "/tasks", label: "任务列表", icon: Bot, end: true },
  { to: "/tasks/new", label: "创建任务", icon: PlusSquare, end: true },
];

export function AppLayout() {
  const [user, setUser] = useState<User | null>(null);

  useEffect(() => {
    getCurrentUser().then(setUser).catch(() => undefined);
  }, []);

  function handleLogout() {
    clearToken();
    window.location.assign("/login");
  }

  return (
    <div className="min-h-screen">
      <div className="mx-auto grid min-h-screen max-w-[1440px] grid-cols-1 gap-6 px-6 py-6 lg:grid-cols-[260px_minmax(0,1fr)]">
        <aside className="rounded-[28px] border border-white/70 bg-white/85 p-5 shadow-soft backdrop-blur">
          <Link to="/" className="flex items-center gap-3 rounded-2xl px-2 py-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-slate-900 text-white">
              <Bot className="h-5 w-5" />
            </div>
            <div>
              <p className="text-base font-bold text-slate-900">CodePilot</p>
              <p className="text-xs text-slate-500">自动修复工作台</p>
            </div>
          </Link>

          <nav className="mt-8 grid gap-2">
            {navItems.map((item) => {
              const Icon = item.icon;
              return (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end={item.end}
                  className={({ isActive }) =>
                    cn(
                      "flex items-center gap-3 rounded-2xl px-4 py-3 text-sm font-medium text-slate-600 transition hover:bg-slate-100 hover:text-slate-900",
                      isActive && "bg-slate-900 text-white hover:bg-slate-900 hover:text-white"
                    )
                  }
                >
                  <Icon className="h-4 w-4" />
                  {item.label}
                </NavLink>
              );
            })}
          </nav>

          <div className="mt-8 rounded-2xl bg-slate-50 p-4">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-full bg-white text-slate-600 shadow-sm">
                <UserRound className="h-4 w-4" />
              </div>
              <div className="min-w-0">
                <p className="truncate text-sm font-semibold text-slate-900">{user?.username ?? "加载中..."}</p>
                <p className="truncate text-xs text-slate-500">{user?.email ?? "当前登录用户"}</p>
              </div>
            </div>
            <Button variant="ghost" className="mt-4 w-full justify-start" onClick={handleLogout}>
              <LogOut className="h-4 w-4" />
              退出登录
            </Button>
          </div>
        </aside>

        <main className="rounded-[28px] border border-white/70 bg-white/80 shadow-soft backdrop-blur">
          <div className="page-shell">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}
