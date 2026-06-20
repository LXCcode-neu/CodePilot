import { Navigate, Outlet, createBrowserRouter } from "react-router-dom";
import { AppLayout } from "@/layouts/AppLayout";
import { AuthLayout } from "@/layouts/AuthLayout";
import { CreateTaskPage } from "@/pages/CreateTaskPage";
import { DashboardPage } from "@/pages/DashboardPage";
import { GitHubAuthPage } from "@/pages/GitHubAuthPage";
import { GitHubCallbackPage } from "@/pages/GitHubCallbackPage";
import { GitHubIssuePage } from "@/pages/GitHubIssuePage";
import { IssueAutomationPage } from "@/pages/IssueAutomationPage";
import { LlmConfigPage } from "@/pages/LlmConfigPage";
import { LoginPage } from "@/pages/LoginPage";
import { ProjectListPage } from "@/pages/ProjectListPage";
import { RegisterPage } from "@/pages/RegisterPage";
import { SentryIntegrationPage } from "@/pages/SentryIntegrationPage";
import { TaskDetailPage } from "@/pages/TaskDetailPage";
import { TaskListPage } from "@/pages/TaskListPage";
import { hasToken } from "@/lib/token";

/**
 * 路由守卫 —— 要求已登录
 * 若本地存在 Token 则渲染子路由（通过 Outlet），否则重定向到登录页。
 */
function RequireAuth() {
  return hasToken() ? <Outlet /> : <Navigate to="/login" replace />;
}

/**
 * 路由守卫 —— 已登录时重定向
 * 若本地存在 Token 则直接跳转到首页，避免已登录用户再次访问登录/注册页。
 */
function RedirectIfAuthenticated() {
  return hasToken() ? <Navigate to="/" replace /> : <Outlet />;
}

/**
 * 应用路由配置
 * 使用 createBrowserRouter 创建浏览器路由实例，整体结构分为三部分：
 * 1. 认证页面路由（登录/注册）—— 仅未登录用户可访问
 * 2. 应用主页面路由 —— 仅已登录用户可访问，外层包裹 AppLayout
 * 3. 通配路由 —— 所有未匹配路径根据登录状态做兜底跳转
 */
export const router = createBrowserRouter([
  {
    element: <RedirectIfAuthenticated />,
    children: [
      {
        element: <AuthLayout />,
        children: [
          { path: "/login", element: <LoginPage /> },
          { path: "/register", element: <RegisterPage /> },
        ],
      },
    ],
  },
  {
    element: <RequireAuth />,
    children: [
      {
        element: <AppLayout />,
        children: [
          { path: "/", element: <DashboardPage /> },
          { path: "/github-auth", element: <GitHubAuthPage /> },
          { path: "/github/callback", element: <GitHubCallbackPage /> },
          { path: "/github-issues", element: <GitHubIssuePage /> },
          { path: "/issue-automation", element: <IssueAutomationPage /> },
          { path: "/sentry", element: <SentryIntegrationPage /> },
          { path: "/projects", element: <ProjectListPage /> },
          { path: "/llm-config", element: <LlmConfigPage /> },
          { path: "/tasks", element: <TaskListPage /> },
          { path: "/tasks/new", element: <CreateTaskPage /> },
          { path: "/tasks/:id", element: <TaskDetailPage /> },
        ],
      },
    ],
  },
  {
    path: "*",
    element: <Navigate to={hasToken() ? "/" : "/login"} replace />,
  },
]);
