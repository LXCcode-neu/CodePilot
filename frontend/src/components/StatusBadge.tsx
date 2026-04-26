import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

const statusStyles: Record<string, string> = {
  PENDING: "border-slate-200 bg-slate-100 text-slate-700",
  CLONING: "border-blue-200 bg-blue-50 text-blue-700",
  INDEXING: "border-blue-200 bg-blue-50 text-blue-700",
  RETRIEVING: "border-cyan-200 bg-cyan-50 text-cyan-700",
  ANALYZING: "border-violet-200 bg-violet-50 text-violet-700",
  GENERATING_PATCH: "border-orange-200 bg-orange-50 text-orange-700",
  WAITING_CONFIRM: "border-amber-200 bg-amber-50 text-amber-700",
  COMPLETED: "border-emerald-200 bg-emerald-50 text-emerald-700",
  FAILED: "border-red-200 bg-red-50 text-red-700",
  RUNNING: "border-blue-200 bg-blue-50 text-blue-700",
  SUCCESS: "border-emerald-200 bg-emerald-50 text-emerald-700",
};

export function StatusBadge({ status }: { status?: string | null }) {
  const value = status || "UNKNOWN";
  return (
    <Badge
      variant="outline"
      className={cn("border-transparent text-[11px] uppercase tracking-wide", statusStyles[value] ?? "bg-slate-100 text-slate-600")}
    >
      {value}
    </Badge>
  );
}
