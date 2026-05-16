import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

const statusStyles: Record<string, string> = {
  PENDING: "border-slate-200 bg-slate-100 text-slate-700",
  CLONING: "border-blue-200 bg-blue-50 text-blue-700",
  RETRIEVING: "border-cyan-200 bg-cyan-50 text-cyan-700",
  ANALYZING: "border-violet-200 bg-violet-50 text-violet-700",
  GENERATING_PATCH: "border-orange-200 bg-orange-50 text-orange-700",
  VERIFYING: "border-sky-200 bg-sky-50 text-sky-700",
  REPAIRING_PATCH: "border-fuchsia-200 bg-fuchsia-50 text-fuchsia-700",
  REVIEWING_PATCH: "border-indigo-200 bg-indigo-50 text-indigo-700",
  CANCEL_REQUESTED: "border-rose-200 bg-rose-50 text-rose-700",
  CANCELLED: "border-slate-300 bg-slate-100 text-slate-700",
  VERIFY_FAILED: "border-red-200 bg-red-50 text-red-700",
  WAITING_CONFIRM: "border-amber-200 bg-amber-50 text-amber-700",
  COMPLETED: "border-emerald-200 bg-emerald-50 text-emerald-700",
  FAILED: "border-red-200 bg-red-50 text-red-700",
  RUNNING: "border-blue-200 bg-blue-50 text-blue-700",
  SUCCESS: "border-emerald-200 bg-emerald-50 text-emerald-700",
};

const statusLabels: Record<string, string> = {
  PENDING: "待执行",
  CLONING: "克隆中",
  RETRIEVING: "检索中",
  ANALYZING: "分析中",
  GENERATING_PATCH: "生成 Patch 中",
  VERIFYING: "验证中",
  REPAIRING_PATCH: "修复 Patch 中",
  CANCEL_REQUESTED: "取消中",
  CANCELLED: "已取消",
  VERIFY_FAILED: "验证失败",
  WAITING_CONFIRM: "待确认",
  COMPLETED: "已完成",
  FAILED: "失败",
  RUNNING: "运行中",
  SUCCESS: "成功",
  UNKNOWN: "未知",
};

export function StatusBadge({ status }: { status?: string | null }) {
  const value = status || "UNKNOWN";
  return (
    <Badge
      variant="outline"
      className={cn("border-transparent text-[11px] uppercase tracking-wide", statusStyles[value] ?? "bg-slate-100 text-slate-600")}
    >
      {statusLabels[value] ?? value}
    </Badge>
  );
}
