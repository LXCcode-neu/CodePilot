import { Navigate, Outlet, createBrowserRouter } from "react-router-dom";
import { AppLayout } from "@/layouts/AppLayout";
import { AuthLayout } from "@/layouts/AuthLayout";
import { DashboardPage } from "@/pages/DashboardPage";
import { LoginPage } from "@/pages/LoginPage";
import { RegisterPage } from "@/pages/RegisterPage";
import { ProjectListPage } from "@/pages/ProjectListPage";
import { GitHubIssuePage } from "@/pages/GitHubIssuePage";
import { TaskListPage } from "@/pages/TaskListPage";
import { CreateTaskPage } from "@/pages/CreateTaskPage";
import { TaskDetailPage } from "@/pages/TaskDetailPage";
import { LlmConfigPage } from "@/pages/LlmConfigPage";
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
          { path: "/github-issues", element: <GitHubIssuePage /> },
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
