import { Link } from "react-router-dom";
import { ArrowRight, Clock3, LoaderCircle, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { StatusBadge } from "@/components/StatusBadge";
import { formatDateTime } from "@/lib/utils";
import type { AgentTask } from "@/types/task";

interface TaskCardProps {
  task: AgentTask;
  repoName?: string;
  onDelete?: (id: string) => void;
  deleting?: boolean;
}

export function TaskCard({ task, repoName, onDelete, deleting = false }: TaskCardProps) {
  const sourceLabel = getSourceLabel(task.sourceType);

  return (
    <Card className="transition hover:-translate-y-0.5">
      <CardHeader className="flex-row items-start justify-between gap-4 space-y-0">
        <div className="space-y-2">
          <div className="flex flex-wrap items-center gap-2">
            <p className="text-xs uppercase tracking-[0.18em] text-slate-400">{repoName || `项目 #${task.projectId}`}</p>
            {sourceLabel ? (
              <span className="rounded-full bg-slate-100 px-2 py-0.5 text-[11px] font-semibold text-slate-500">
                {sourceLabel}
              </span>
            ) : null}
          </div>
          <CardTitle className="text-base">{task.issueTitle}</CardTitle>
        </div>
        <StatusBadge status={task.status} />
      </CardHeader>
      <CardContent className="space-y-4">
        <p className="line-clamp-2 text-sm leading-6 text-slate-500">{task.issueDescription}</p>
        <div className="flex items-center justify-between gap-3">
          <div className="flex items-center gap-2 text-xs text-slate-400">
            <Clock3 className="h-3.5 w-3.5" />
            {formatDateTime(task.createdAt)}
          </div>
          <div className="flex items-center gap-2">
            {onDelete ? (
              <Button variant="destructive" size="sm" onClick={() => onDelete(task.id)} disabled={deleting}>
                {deleting ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <Trash2 className="h-4 w-4" />}
                删除任务
              </Button>
            ) : null}
            <Button asChild variant="ghost" size="sm">
              <Link to={`/tasks/${task.id}`}>
                查看详情
                <ArrowRight className="h-4 w-4" />
              </Link>
            </Button>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

function getSourceLabel(sourceType?: string | null) {
  if (sourceType === "GITHUB_ISSUE") {
    return "GitHub Issue";
  }
  if (sourceType === "SENTRY_ALERT") {
    return "Sentry 告警";
  }
  if (sourceType === "MANUAL") {
    return "手动创建";
  }
  return null;
}
