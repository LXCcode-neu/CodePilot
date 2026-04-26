import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { LoaderCircle } from "lucide-react";
import { getProjects } from "@/api/project";
import { createTask } from "@/api/task";
import { EmptyState } from "@/components/EmptyState";
import { LoadingBlock } from "@/components/LoadingBlock";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import type { ProjectRepo } from "@/types/project";

export function CreateTaskPage() {
  const navigate = useNavigate();
  const [projects, setProjects] = useState<ProjectRepo[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [form, setForm] = useState({
    projectId: "",
    issueTitle: "",
    issueDescription: "",
  });

  useEffect(() => {
    getProjects()
      .then((data) => {
        setProjects(data);
        if (data[0]) {
          setForm((prev) => ({ ...prev, projectId: String(data[0].id) }));
        }
      })
      .finally(() => setLoading(false));
  }, []);

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError("");

    try {
      const result = await createTask({
        projectId: form.projectId,
        issueTitle: form.issueTitle,
        issueDescription: form.issueDescription,
      });
      navigate(`/tasks/${result.id}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "创建任务失败");
    } finally {
      setSubmitting(false);
    }
  }

  if (!loading && !projects.length) {
    return (
      <EmptyState
        title="请先添加仓库"
        description="创建任务前必须先接入一个 GitHub 公开仓库。"
        action={
          <Button asChild>
            <Link to="/projects">前往仓库管理</Link>
          </Button>
        }
      />
    );
  }

  return (
    <Card className="max-w-4xl">
      <CardHeader>
        <CardTitle className="text-2xl font-bold">创建任务</CardTitle>
        <CardDescription>选择一个仓库，填写 Issue 标题和问题描述，提交后会自动跳转到任务详情页。</CardDescription>
      </CardHeader>
      <CardContent>
        {loading ? (
          <div className="max-w-xl">
            <LoadingBlock lines={5} />
          </div>
        ) : (
          <form className="space-y-5" onSubmit={handleSubmit}>
            <div className="space-y-2">
              <label className="text-sm font-medium text-slate-700">选择仓库</label>
              <Select value={form.projectId} onValueChange={(value) => setForm((prev) => ({ ...prev, projectId: value }))}>
                <SelectTrigger>
                  <SelectValue placeholder="请选择一个仓库" />
                </SelectTrigger>
                <SelectContent>
                  {projects.map((project) => (
                    <SelectItem key={project.id} value={String(project.id)}>
                      {project.repoName}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <label className="text-sm font-medium text-slate-700">Issue 标题</label>
              <Input
                value={form.issueTitle}
                onChange={(event) => setForm((prev) => ({ ...prev, issueTitle: event.target.value }))}
                placeholder="例如：修复任务详情页在 token 过期时的异常处理"
                required
              />
            </div>

            <div className="space-y-2">
              <label className="text-sm font-medium text-slate-700">Issue 描述</label>
              <Textarea
                value={form.issueDescription}
                onChange={(event) => setForm((prev) => ({ ...prev, issueDescription: event.target.value }))}
                placeholder="请描述问题表现、复现步骤、预期结果，以及希望 Agent 优先关注的文件或模块。"
                required
              />
            </div>

            {error ? <div className="rounded-2xl bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div> : null}

            <Button type="submit" disabled={submitting}>
              {submitting ? <LoaderCircle className="h-4 w-4 animate-spin" /> : null}
              创建任务
            </Button>
          </form>
        )}
      </CardContent>
    </Card>
  );
}
