import { FolderGit2, Trash2 } from "lucide-react";
import { StatusBadge } from "@/components/StatusBadge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import type { ProjectRepo } from "@/types/project";

export function ProjectCard({
  project,
  onDelete,
  deleting,
}: {
  project: ProjectRepo;
  onDelete: (id: string) => void;
  deleting?: boolean;
}) {
  return (
    <Card>
      <CardHeader className="flex-row items-start justify-between gap-4 space-y-0">
        <div className="space-y-2">
          <div className="flex items-center gap-2">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-slate-100 text-slate-700">
              <FolderGit2 className="h-4 w-4" />
            </div>
            <div>
              <CardTitle>{project.repoName}</CardTitle>
              <p className="text-sm text-slate-500">{project.repoUrl}</p>
            </div>
          </div>
        </div>
        <StatusBadge status={project.status} />
      </CardHeader>
      <CardContent className="space-y-4">
        <Separator />
        <div className="grid gap-3 text-sm text-slate-600">
          <div>
            <p className="text-xs uppercase tracking-wide text-slate-400">Local Path</p>
            <p className="mt-1 break-all text-slate-700">{project.localPath || "--"}</p>
          </div>
          <div>
            <p className="text-xs uppercase tracking-wide text-slate-400">Default Branch</p>
            <p className="mt-1 text-slate-700">{project.defaultBranch || "--"}</p>
          </div>
        </div>
        <Button variant="destructive" size="sm" onClick={() => onDelete(project.id)} disabled={deleting}>
          <Trash2 className="h-4 w-4" />
          删除仓库
        </Button>
      </CardContent>
    </Card>
  );
}
