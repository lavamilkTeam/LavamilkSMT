import { useEffect, useRef, useState } from "react";
import {
  Box,
  ChevronRight,
  CircleHelp,
  Download,
  FilePlus2,
  FolderOpen,
  Layers3,
  Pause,
  Play,
  RotateCcw,
  Square,
  Upload,
  CheckCircle2,
  CircleAlert,
  Cpu,
} from "lucide-react";
import { Button } from "@/shared/ui/button";
import { Badge } from "@/shared/ui/badge";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/shared/ui/tabs";
import { Progress } from "@/shared/ui/progress";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
  TooltipProvider,
} from "@/shared/ui/tooltip";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/shared/ui/dialog";
import { CameraPanel } from "@/features/camera/camera-panel";
import { MachinePanel } from "@/features/machine/machine-panel";
import { useMachine } from "@/features/machine/use-machine";
import { PlacementTable } from "@/features/job/placement-table";
import { BoardPreview } from "@/features/job/board-preview";
import {
  samplePlacements,
  parsePlacementCsv,
  encodePlacementCsv,
  nextPending,
  type Placement,
  type JobState,
} from "@/features/job/model";
import { ConfigurationPanel } from "@/features/configuration/configuration-panel";
import { LogPanel } from "@/features/logs/log-panel";
import { useLogs } from "@/features/logs/use-logs";
import { download } from "@/shared/lib/download";
import { usePreviewTools } from "@/shared/lib/preview-tools";

const pages = [
  ["job", "贴装任务"],
  ["panels", "拼板"],
  ["boards", "单板"],
  ["parts", "元件库"],
  ["packages", "封装库"],
  ["vision", "视觉"],
  ["feeders", "飞达"],
  ["machine", "机器配置"],
  ["issues", "问题与解决方案"],
  ["logs", "运行日志"],
];
const states: Record<JobState, string> = {
  idle: "待演示",
  running: "演示运行中",
  paused: "已暂停",
  completed: "演示完成",
  stopped: "已停止",
};
export default function App() {
  const { logs, addLog, clearLogs } = useLogs();
  const machine = useMachine(addLog);
  const [rows, setRows] = useState<Placement[]>(() =>
    samplePlacements.map((p) => ({ ...p })),
  );
  const [name, setName] = useState("演示控制板");
  const [selected, setSelected] = useState<string | null>("R1");
  const [completed, setCompleted] = useState<string[]>([]);
  const [job, setJob] = useState<JobState>("idle");
  const [page, setPage] = useState("job");
  const [notice, setNotice] = useState("");
  const [confirm, setConfirm] = useState<"new" | "sample" | null>(null);
  const [help, setHelp] = useState(false);
  const fileInput = useRef<HTMLInputElement>(null);
  const busy = job === "running" || job === "paused";
  const enabled = rows.filter((p) => p.enabled).length;
  const progress = enabled ? Math.round((completed.length / enabled) * 100) : 0;
  const active = rows.find((p) => p.id === selected);
  usePreviewTools({
    mode: "simulation",
    jobState: job,
    total: enabled,
    completed: completed.length,
    selected,
  });
  const canStart = machine.powered && machine.homed && enabled > 0;
  useEffect(() => {
    if (job !== "running") return;
    const next = nextPending(rows, completed);
    if (!next) {
      setJob("completed");
      addLog("演示任务完成；未发送任何硬件指令");
      return;
    }
    const timer = window.setTimeout(
      () => {
        setSelected(next.id);
        machine.setPosition({ x: next.x, y: next.y, z: 0, c: next.rotation });
        setCompleted((done) => [...done, next.id]);
        addLog(
          `模拟贴装 ${next.id} · X ${next.x.toFixed(2)} / Y ${next.y.toFixed(2)} mm`,
        );
      },
      Math.round((1200 * 50) / machine.speed),
    );
    return () => window.clearTimeout(timer);
  }, [job, completed, rows, machine.speed, machine.setPosition, addLog]);
  function resetProgress() {
    setCompleted([]);
    setJob("idle");
  }
  function update(row: Placement) {
    if (busy) return;
    setRows((old) => old.map((p) => (p.id === row.id ? row : p)));
    resetProgress();
    addLog(`已更新元件 ${row.id}`);
  }
  function stop() {
    setJob("stopped");
    machine.setVacuum(false);
    addLog("已停止界面演示；未执行真实急停", "warning");
  }
  function start() {
    if (!canStart || job === "running") return;
    if (job !== "paused") setCompleted([]);
    setJob("running");
    addLog(job === "paused" ? "继续演示任务" : "开始演示任务（无硬件动作）");
  }
  async function importCsv(file: File) {
    if (busy) return;
    try {
      if (file.size > 2 * 1024 * 1024)
        throw new Error("文件过大，请使用小于 2MB 的 CSV");
      const parsed = parsePlacementCsv(await file.text());
      setRows(parsed);
      setName(file.name.replace(/\.csv$/i, ""));
      setSelected(parsed[0]?.id ?? null);
      resetProgress();
      setNotice("");
      setPage("job");
      addLog(`已导入 ${file.name}，共 ${parsed.length} 个元件`);
    } catch (err) {
      setNotice(err instanceof Error ? err.message : "导入失败");
      addLog("CSV 导入失败，原任务保留", "warning");
    }
  }
  return (
    <TooltipProvider>
      <div className="workbench">
        <header className="app-header">
          <div className="brand">
            <div className="brand-icon">
              <Layers3 size={22} />
            </div>
            <strong>
              SMT<span>工作台</span>
            </strong>
            <span className="header-divider" />
            <span className="header-context">贴片机控制台</span>
          </div>
          <div className="header-right">
            <span className="demo-badge">
              <span className="status-dot amber" />
              界面演示
            </span>
            <span className="connection-status">
              <span className="status-dot" />
              硬件未连接
            </span>
            <Button
              variant="ghost"
              size="icon"
              className="header-help"
              aria-label="预览使用说明"
              onClick={() => setHelp(true)}
            >
              <CircleHelp />
            </Button>
          </div>
        </header>
        <div className="project-bar">
          <div className="project-breadcrumb">
            <FolderOpen size={17} />
            <span>工作空间</span>
            <ChevronRight size={14} />
            <strong>{name}</strong>
            <Badge variant="outline">本地草稿</Badge>
          </div>
          <div className="project-actions">
            <Button
              variant="ghost"
              size="sm"
              disabled={busy}
              onClick={() => setConfirm("new")}
            >
              <FilePlus2 />
              新建
            </Button>
            <Button
              variant="ghost"
              size="sm"
              disabled={busy}
              onClick={() => setConfirm("sample")}
            >
              <RotateCcw />
              载入示例
            </Button>
            <Button
              variant="outline"
              size="sm"
              disabled={busy}
              onClick={() => fileInput.current?.click()}
            >
              <Upload />
              导入 CSV
            </Button>
            <Button
              variant="outline"
              size="sm"
              disabled={!rows.length}
              onClick={() => {
                download(
                  `${name}.csv`,
                  "\uFEFF" + encodePlacementCsv(rows),
                  "text/csv;charset=utf-8",
                );
                addLog("已导出当前元件坐标 CSV（不含演示进度）");
              }}
            >
              <Download />
              导出坐标
            </Button>
            <input
              ref={fileInput}
              type="file"
              accept=".csv,text/csv"
              className="hidden"
              aria-label="导入贴片坐标 CSV"
              onChange={(e) => {
                const file = e.target.files?.[0];
                if (file) void importCsv(file);
                e.target.value = "";
              }}
            />
          </div>
        </div>
        <div className="preview-notice">
          <span>
            <CircleAlert size={15} />
            本地界面预览 · 点动和贴装均为模拟，刷新后恢复示例。
          </span>
          <button onClick={() => setHelp(true)}>
            操作说明 <ChevronRight size={13} />
          </button>
        </div>
        {notice && (
          <div className="error-notice" role="alert">
            {notice}
            <Button variant="ghost" size="sm" onClick={() => setNotice("")}>
              关闭
            </Button>
          </div>
        )}
        <main className="workspace">
          <aside className="left-column">
            <CameraPanel log={addLog} />
            <MachinePanel
              machine={machine}
              busy={busy}
              stop={stop}
              log={addLog}
            />
          </aside>
          <section className="panel workspace-panel">
            <Tabs
              value={page}
              onValueChange={setPage}
              className="workspace-tabs"
            >
              <div className="workspace-tab-scroll">
                <TabsList className="workspace-tab-list">
                  {pages.map(([id, label]) => (
                    <TabsTrigger key={id} value={id}>
                      {label}
                      {id === "issues" && <span className="issue-dot" />}
                    </TabsTrigger>
                  ))}
                </TabsList>
              </div>
              <TabsContent value="job" className="job-content">
                <div className="job-overview">
                  <div className="job-summary">
                    <p className="eyebrow">PLACEMENT JOB</p>
                    <div className="job-title">
                      <h1>{name}</h1>
                      <Badge
                        variant={job === "running" ? "default" : "secondary"}
                      >
                        {states[job]}
                      </Badge>
                    </div>
                    <div className="job-meta">
                      <span>
                        <Layers3 size={14} />1 块单板
                      </span>
                      <span>
                        <Box size={14} />
                        {enabled} 个启用元件
                      </span>
                      <span>H1 / N1</span>
                    </div>
                    <div className="job-progress">
                      <Progress value={progress} className="h-1.5" />
                      <span className="mono">
                        {completed.length} / {enabled}
                      </span>
                    </div>
                  </div>
                  <div className="board-thumbnail">
                    <BoardPreview
                      rows={rows}
                      selected={selected}
                      select={setSelected}
                      completed={completed}
                    />
                  </div>
                </div>
                <div className="job-actions">
                  <div className="flex gap-2">
                    {job === "running" ? (
                      <Button
                        size="sm"
                        onClick={() => {
                          setJob("paused");
                          addLog("演示任务已暂停");
                        }}
                      >
                        <Pause />
                        暂停演示
                      </Button>
                    ) : (
                      <Tooltip>
                        <TooltipTrigger asChild>
                          <span tabIndex={!canStart ? 0 : undefined}>
                            <Button
                              size="sm"
                              disabled={!canStart}
                              onClick={start}
                            >
                              <Play />
                              {job === "paused" ? "继续演示" : "运行演示"}
                            </Button>
                          </span>
                        </TooltipTrigger>
                        <TooltipContent>
                          先在左侧使能模拟并完成模拟回零
                        </TooltipContent>
                      </Tooltip>
                    )}
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={!busy}
                      onClick={stop}
                    >
                      <Square />
                      停止
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      disabled={busy || !completed.length}
                      onClick={() => {
                        resetProgress();
                        addLog("演示进度已重置");
                      }}
                    >
                      <RotateCcw />
                      重置
                    </Button>
                  </div>
                  <span className="job-hint">
                    {!machine.powered
                      ? "请先使能模拟设备"
                      : !machine.homed
                        ? "请先模拟回零"
                        : job === "running"
                          ? "正在演示，无硬件动作"
                          : "模拟设备就绪"}
                  </span>
                </div>
                <PlacementTable
                  rows={rows}
                  completed={completed}
                  selected={selected}
                  select={setSelected}
                  update={update}
                  remove={(id) => {
                    if (busy) return;
                    setRows((old) => old.filter((p) => p.id !== id));
                    if (selected === id) setSelected(null);
                    resetProgress();
                    addLog(`已从任务移除 ${id}`);
                  }}
                  locked={busy}
                />
                <div className="selection-bar">
                  <span>
                    <CheckCircle2 size={15} />
                    {active
                      ? `已选择 ${active.id} · ${active.part}`
                      : "未选择元件"}
                  </span>
                  <span>
                    {active
                      ? `${active.package} · ${active.side === "Top" ? "顶层" : "底层"}`
                      : "点击表格或单板示意图选择"}
                  </span>
                </div>
              </TabsContent>
              {pages
                .filter(([id]) => !["job", "logs"].includes(id))
                .map(([id]) => (
                  <TabsContent key={id} value={id} className="m-0 flex-1">
                    <ConfigurationPanel
                      page={id}
                      rows={rows}
                      selected={selected}
                      select={setSelected}
                      log={addLog}
                    />
                  </TabsContent>
                ))}
              <TabsContent value="logs" className="m-0 flex-1">
                <LogPanel logs={logs} clear={clearLogs} />
              </TabsContent>
            </Tabs>
          </section>
        </main>
        <footer className="status-bar">
          <span>
            <Cpu size={14} />
            STM32F407 · 未连接
          </span>
          <span className="status-event">
            {logs.at(-1)?.message ?? "暂无事件"}
          </span>
          <span className="mono">WEB PREVIEW / 0.1</span>
        </footer>
        <Dialog
          open={!!confirm}
          onOpenChange={(open) => {
            if (!open) setConfirm(null);
          }}
        >
          <DialogContent>
            <DialogHeader>
              <DialogTitle>
                {confirm === "new" ? "新建任务" : "重新载入示例"}
              </DialogTitle>
              <DialogDescription>
                这会替换当前页面中的任务和修改。需要保留坐标时，请先导出 CSV。
              </DialogDescription>
            </DialogHeader>
            <DialogFooter>
              <Button variant="outline" onClick={() => setConfirm(null)}>
                取消
              </Button>
              <Button
                onClick={() => {
                  const sample = confirm === "sample";
                  setRows(
                    sample ? samplePlacements.map((p) => ({ ...p })) : [],
                  );
                  setName(sample ? "演示控制板" : "未命名任务");
                  setSelected(sample ? "R1" : null);
                  resetProgress();
                  setNotice("");
                  setPage("job");
                  setConfirm(null);
                  addLog(sample ? "已恢复示例任务" : "已新建空任务");
                }}
              >
                确认替换
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
        <Dialog open={help} onOpenChange={setHelp}>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>试用新的操作界面</DialogTitle>
              <DialogDescription>
                React 界面原型，演示动作不会发送到目标机。
              </DialogDescription>
            </DialogHeader>
            <ol className="help-steps">
              <li>在左下角点击“使能模拟”，再点击“模拟回零”。</li>
              <li>用点动按钮查看坐标变化，或运行右侧任务演示。</li>
              <li>搜索、编辑或禁用元件；在其他页查看料位和配置草稿。</li>
              <li>相机“视频源”可以接现有预览服务，仅用于查看画面。</li>
            </ol>
            <div className="csv-example">
              <strong>CSV 表头</strong>
              <code>id,part,package,x,y,rotation,side</code>
              <code>R1,10k,0603,12.4,18.2,0,Top</code>
            </div>
            <p className="text-sm text-muted-foreground">
              刷新会恢复示例。导出坐标只保存 CSV
              字段，不包含启用状态、标定或演示进度。
            </p>
          </DialogContent>
        </Dialog>
      </div>
    </TooltipProvider>
  );
}
