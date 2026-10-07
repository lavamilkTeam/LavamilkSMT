import { useState } from "react";
import {
  Camera,
  Crosshair,
  Expand,
  Link2,
  Minus,
  Plus,
  Unplug,
  Image as ImageIcon,
} from "lucide-react";
import { Button } from "@/shared/ui/button";
import { Badge } from "@/shared/ui/badge";
import { Input } from "@/shared/ui/input";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
  DialogTrigger,
} from "@/shared/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/ui/select";

export function CameraPanel({ log }: { log: (message: string) => void }) {
  const [source, setSource] = useState("");
  const [draft, setDraft] = useState("http://192.168.1.100:8766");
  const [mode, setMode] = useState("gray");
  const [error, setError] = useState("");
  const [dialog, setDialog] = useState(false);
  const [crosshair, setCrosshair] = useState(true);
  const [zoom, setZoom] = useState(1);
  const [expanded, setExpanded] = useState(false);
  const [formError, setFormError] = useState("");
  function connect() {
    try {
      const url = new URL(draft);
      if (
        !["http:", "https:"].includes(url.protocol) ||
        url.username ||
        url.password
      )
        throw new Error();
      if (window.location.protocol === "https:" && url.protocol === "http:") {
        setFormError("HTTPS 页面无法加载 HTTP 相机，请使用本地 HTTP 预览。");
        return;
      }
      setSource(url.origin);
      setError("");
      setZoom(1);
      setDialog(false);
      log(`打开只读相机预览：${url.origin}`);
    } catch {
      setFormError("请输入有效的 HTTP 或 HTTPS 服务地址。");
    }
  }
  return (
    <section
      className={`panel camera-panel ${expanded ? "camera-expanded" : ""}`}
      aria-label="相机画面"
    >
      <div className="panel-heading">
        <div className="heading-label">
          <Camera size={17} />
          <h2>相机视图</h2>
          <Badge variant="secondary">H1 · 下视</Badge>
        </div>
        <Dialog open={dialog} onOpenChange={setDialog}>
          <DialogTrigger asChild>
            <Button variant="ghost" size="sm">
              <Link2 />
              视频源
            </Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>连接相机预览</DialogTitle>
              <DialogDescription>
                读取现有 Python 预览服务的画面。仅查看视频，不建立运动控制连接。
              </DialogDescription>
            </DialogHeader>
            <label className="field-label" htmlFor="camera-source">
              预览服务地址
            </label>
            <Input
              id="camera-source"
              value={draft}
              onChange={(e) => {
                setDraft(e.target.value);
                setFormError("");
              }}
              placeholder="http://设备IP:8766"
            />
            <p className="text-sm text-muted-foreground">
              支持 /stream/gray.mjpg、threshold.mjpg 和 contours.mjpg。
            </p>
            {formError && (
              <p role="alert" className="text-destructive">
                {formError}
              </p>
            )}
            <DialogFooter>
              <Button variant="outline" onClick={() => setDialog(false)}>
                取消
              </Button>
              <Button onClick={connect}>打开只读预览</Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </div>
      <div className="camera-toolbar">
        <span className="camera-source">
          <span className={`status-dot ${source ? "amber" : ""}`} />
          {source ? "USB 相机 · 只读" : "OpenPnP 示例图"}
        </span>
        <Select
          value={mode}
          onValueChange={(value) => {
            setMode(value);
            setError("");
          }}
          disabled={!source}
        >
          <SelectTrigger className="h-8 w-28 text-sm" aria-label="相机处理模式">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="gray">灰度</SelectItem>
            <SelectItem value="threshold">二值化</SelectItem>
            <SelectItem value="contours">轮廓叠加</SelectItem>
          </SelectContent>
        </Select>
      </div>
      <div className="camera-viewport">
        {!error ? (
          <img
            key={`${source}-${mode}`}
            className={source ? "camera-image live" : "camera-image sample"}
            style={{ transform: `scale(${zoom})` }}
            src={source ? `${source}/stream/${mode}.mjpg` : "/openpnp-test.png"}
            alt={source ? "相机实时预览" : "OpenPnP 官方测试板示例，非实时图像"}
            onError={() => setError("无法读取画面，请检查预览服务地址和网络。")}
          />
        ) : (
          <div className="camera-error" role="alert">
            <Unplug size={32} />
            <p>{error}</p>
            <Button
              variant="secondary"
              onClick={() => {
                setSource("");
                setError("");
              }}
            >
              返回示例图
            </Button>
          </div>
        )}
        {crosshair && !error && (
          <div className="crosshair" aria-hidden="true">
            <div />
            <span />
          </div>
        )}
        <div className="viewport-tag">
          <span className="status-dot amber" />
          {source ? "预览画面 · 未标定" : "REFERENCE · 非实时"}
        </div>
        <div className="viewport-footer mono">
          <span>{source ? new URL(source).host : "OPENPNP / PNP-TEST"}</span>
          <span>{zoom.toFixed(1)}×</span>
        </div>
      </div>
      <div className="camera-actions">
        <div className="flex gap-1">
          <Button
            variant={crosshair ? "secondary" : "ghost"}
            size="icon"
            aria-label="切换十字线"
            aria-pressed={crosshair}
            onClick={() => setCrosshair(!crosshair)}
          >
            <Crosshair />
          </Button>
          <Button
            variant="ghost"
            size="icon"
            aria-label="缩小画面"
            disabled={zoom <= 1}
            onClick={() => setZoom(Math.max(1, zoom - 0.5))}
          >
            <Minus />
          </Button>
          <Button
            variant="ghost"
            size="icon"
            aria-label="放大画面"
            disabled={zoom >= 4}
            onClick={() => setZoom(Math.min(4, zoom + 0.5))}
          >
            <Plus />
          </Button>
          <Button
            variant="ghost"
            size="icon"
            aria-label={expanded ? "还原相机视图" : "扩大相机视图"}
            onClick={() => setExpanded(!expanded)}
          >
            <Expand />
          </Button>
        </div>
        {source ? (
          <Button
            variant="ghost"
            size="sm"
            onClick={() => {
              setSource("");
              setError("");
              log("已关闭只读视频连接");
            }}
          >
            <Unplug />
            断开视频
          </Button>
        ) : (
          <span className="text-xs text-muted-foreground flex items-center gap-1">
            <ImageIcon size={13} />
            测试图，不用于定位
          </span>
        )}
      </div>
    </section>
  );
}
