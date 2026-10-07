import { useCallback, useState } from "react";
export type LogEntry = {
  id: number;
  time: string;
  level: "info" | "warning";
  message: string;
};
export function useLogs() {
  const [logs, setLogs] = useState<LogEntry[]>([
    {
      id: 0,
      time: new Date().toLocaleTimeString("zh-CN", { hour12: false }),
      level: "info",
      message: "已载入示例任务。当前为界面演示，未连接运动控制。",
    },
  ]);
  const addLog = useCallback(
    (message: string, level: LogEntry["level"] = "info") => {
      setLogs((old) => [
        ...old.slice(-199),
        {
          id: Date.now() + Math.random(),
          time: new Date().toLocaleTimeString("zh-CN", { hour12: false }),
          level,
          message,
        },
      ]);
    },
    [],
  );
  return { logs, addLog, clearLogs: () => setLogs([]) };
}
