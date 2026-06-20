import * as React from "react";
import { Slot } from "@radix-ui/react-slot";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";

/** 按钮变体样式配置，定义了变体和尺寸两种分类维度 */
const buttonVariants = cva(
  "inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-xl text-sm font-semibold transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50",
  {
    variants: {
      variant: {
        default: "bg-primary text-primary-foreground hover:bg-slate-800",
        secondary: "bg-secondary text-secondary-foreground hover:bg-slate-200",
        outline: "border border-border bg-white hover:bg-slate-50",
        ghost: "text-slate-700 hover:bg-slate-100",
        destructive: "bg-red-600 text-white hover:bg-red-700",
      },
      size: {
        default: "h-11 px-5 py-2.5",
        sm: "h-9 rounded-lg px-3",
        lg: "h-12 rounded-xl px-6",
        icon: "h-10 w-10 rounded-xl",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  }
);

/** 按钮组件的属性接口 */
export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {
  /** 当设为 true 时，按钮将渲染为子元素（通过 Radix Slot），用于自定义底层元素 */
  asChild?: boolean;
}

/**
 * 按钮（Button）组件
 *
 * 通用操作按钮，支持多种样式变体和尺寸。
 * 变体：default（主色）、secondary（次要）、outline（描边）、ghost（透明）、destructive（危险/红色）。
 * 尺寸：default、sm（小）、lg（大）、icon（方形图标按钮）。
 * 通过 asChild 可将按钮样式应用到子元素上。
 */
const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, asChild = false, ...props }, ref) => {
    const Comp = asChild ? Slot : "button";
    return <Comp className={cn(buttonVariants({ variant, size, className }))} ref={ref} {...props} />;
  }
);
Button.displayName = "Button";

export { Button, buttonVariants };
