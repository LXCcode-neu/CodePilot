import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";

/** 徽章变体样式配置 */
const badgeVariants = cva(
  "inline-flex items-center rounded-full border px-2.5 py-1 text-xs font-semibold transition-colors",
  {
    variants: {
      variant: {
        default: "border-transparent bg-slate-900 text-white",
        secondary: "border-transparent bg-slate-100 text-slate-700",
        outline: "border-border bg-white text-slate-700",
      },
    },
    defaultVariants: {
      variant: "default",
    },
  }
);

/** 徽章组件的属性接口 */
export interface BadgeProps extends React.HTMLAttributes<HTMLDivElement>, VariantProps<typeof badgeVariants> {}

/**
 * 徽章（Badge）组件
 *
 * 用于标记状态、分类或计数的小型标签元素。
 * 支持 default（深色）、secondary（浅灰）、outline（描边）三种样式变体。
 */
function Badge({ className, variant, ...props }: BadgeProps) {
  return <div className={cn(badgeVariants({ variant }), className)} {...props} />;
}

export { Badge, badgeVariants };
