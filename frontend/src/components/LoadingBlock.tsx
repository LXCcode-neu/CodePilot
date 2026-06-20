/**
 * 骨架屏加载占位组件
 *
 * 用于内容加载中时展示脉冲动画的占位条，
 * 模拟文本行的加载效果，每行宽度递减以营造自然的视觉节奏。
 */
export function LoadingBlock({ lines = 4 }: { /** 占位行数，默认为 4 */ lines?: number }) {
  return (
    <div className="space-y-3">
      {Array.from({ length: lines }).map((_, index) => (
        <div
          key={index}
          className="h-4 animate-pulse rounded-full bg-slate-200"
          style={{ width: `${88 - index * 10}%` }}
        />
      ))}
    </div>
  );
}
