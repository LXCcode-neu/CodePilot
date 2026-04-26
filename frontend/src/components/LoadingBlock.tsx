export function LoadingBlock({ lines = 4 }: { lines?: number }) {
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
