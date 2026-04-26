import { Radio } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ScrollArea } from "@/components/ui/scroll-area";
import { EmptyState } from "@/components/EmptyState";
import { formatDateTime } from "@/lib/utils";
import type { TaskEventMessage } from "@/types/common";

export function RealtimeEventPanel({ events }: { events: TaskEventMessage[] }) {
  return (
    <Card className="h-full">
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-base">
          <Radio className="h-4 w-4 text-emerald-600" />
          实时事件
        </CardTitle>
      </CardHeader>
      <CardContent>
        {!events.length ? (
          <EmptyState title="暂无实时事件" description="连接 SSE 后，这里会显示状态变更和运行消息。" />
        ) : (
          <ScrollArea className="h-[320px] pr-4">
            <div className="space-y-3">
              {events.map((event) => (
                <div key={event.id} className="rounded-2xl bg-slate-50 p-4">
                  <div className="flex items-center justify-between gap-3">
                    <p className="text-xs uppercase tracking-wide text-slate-400">{event.status || "EVENT"}</p>
                    <p className="text-xs text-slate-400">{formatDateTime(event.time)}</p>
                  </div>
                  <p className="mt-2 text-sm text-slate-700">{event.message}</p>
                </div>
              ))}
            </div>
          </ScrollArea>
        )}
      </CardContent>
    </Card>
  );
}
