import * as React from "react";
import { cn } from "@/lib/utils";

/**
 * 多行文本输入框（Textarea）组件
 *
 * 支持自适应最小高度、聚焦高亮和禁用状态。
 * 接受所有原生 textarea 元素的属性。
 */
const Textarea = React.forwardRef<HTMLTextAreaElement, React.ComponentProps<"textarea">>(
  ({ className, ...props }, ref) => {
    return (
      <textarea
        className={cn(
          "flex min-h-[128px] w-full rounded-xl border border-input bg-white px-3 py-3 text-sm text-slate-900 placeholder:text-slate-400 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50",
          className
        )}
        ref={ref}
        {...props}
      />
    );
  }
);
Textarea.displayName = "Textarea";

export { Textarea };
