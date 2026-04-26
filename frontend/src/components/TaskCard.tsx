import { Link } from "react-router-dom";
import { ArrowRight, Clock3 } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { StatusBadge } from "@/components/StatusBadge";
import { formatDateTime } from "@/lib/utils";
import type { AgentTask } from "@/types/task";

export function TaskCard({ task, repoName }: { task: AgentTask; repoName?: string }) {
  return (
    <Card className="transition hover:-translate-y-0.5">
      <CardHeader className="flex-row items-start justify-between gap-4 space-y-0">
        <div className="space-y-2">
          <p className="text-xs uppercase tracking-[0.18em] text-slate-400">{repoName || `Project #${task.projectId}`}</p>
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
          <Button asChild variant="ghost" size="sm">
            <Link to={`/tasks/${task.id}`}>
              查看详情
              <ArrowRight className="h-4 w-4" />
            </Link>
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}
