import type { ReactNode } from "react";
import { Card, CardContent } from "@/components/ui/card";

/**
 * 空状态占位组件
 *
 * 当列表或区域无数据时展示，包含标题、描述说明和可选的操作按钮。
 * 使用虚线边框卡片样式，内容垂直居中显示。
 */
export function EmptyState({
  title,
  description,
  action,
}: {
  /** 空状态标题 */
  title: string;
  /** 空状态描述文字 */
  description: string;
  /** 可选的操作区域，通常放置按钮 */
  action?: ReactNode;
}) {
  return (
    <Card className="border-dashed shadow-none">
      <CardContent className="flex flex-col items-center justify-center gap-4 py-12 text-center">
        <div className="space-y-2">
          <p className="text-base font-semibold text-slate-900">{title}</p>
          <p className="max-w-md text-sm text-slate-500">{description}</p>
        </div>
        {action}
      </CardContent>
    </Card>
  );
}
