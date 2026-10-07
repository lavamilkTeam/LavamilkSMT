import { Download, Trash2 } from "lucide-react";
import { Button } from "@/shared/ui/button";
import { download } from "@/shared/lib/download";
import type { LogEntry } from "./use-logs";
export function LogPanel({
  logs,
  clear,
}: {
  logs: LogEntry[];
  clear: () => void;
}) {
  return (
    <div className="configuration-panel">
      <div className="configuration-heading">
        <div>
          <p className="eyebrow">WORKSPACE / EVENTS</p>
          <h2>运行日志</h2>
        </div>
        <div className="flex gap-2">
          <Button
            variant="outline"
            size="sm"
            disabled={!logs.length}
            onClick={() =>
              download(
                "smt-preview.log",
                logs
                  .map((l) => `${l.time} [${l.level}] ${l.message}`)
                  .join("\n"),
              )
            }
          >
            <Download />
            导出
          </Button>
          <Button
            variant="ghost"
            size="sm"
            disabled={!logs.length}
            onClick={clear}
          >
            <Trash2 />
            清空
          </Button>
        </div>
      </div>
      <div className="log-list" role="log" aria-label="演示操作日志">
        {logs.length ? (
          [...logs].reverse().map((l) => (
            <div className={`log-entry ${l.level}`} key={l.id}>
              <time className="mono">{l.time}</time>
              <span className="mono">{l.level.toUpperCase()}</span>
              <p>{l.message}</p>
            </div>
          ))
        ) : (
          <p className="empty-message">
            暂无日志。操作界面后，事件会显示在这里。
          </p>
        )}
      </div>
    </div>
  );
}
