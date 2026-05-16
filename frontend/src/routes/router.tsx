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

function RequireAuth() {
  return hasToken() ? <Outlet /> : <Navigate to="/login" replace />;
}

function RedirectIfAuthenticated() {
  return hasToken() ? <Navigate to="/" replace /> : <Outlet />;
}

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
