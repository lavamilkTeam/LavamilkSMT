import {
  ArrowDown,
  ArrowLeft,
  ArrowRight,
  ArrowUp,
  CircleStop,
  Crosshair,
  Home,
  Power,
  RotateCcw,
  RotateCw,
} from "lucide-react";
import { Button } from "@/shared/ui/button";
import { Badge } from "@/shared/ui/badge";
import { Slider } from "@/shared/ui/slider";
import { Switch } from "@/shared/ui/switch";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/shared/ui/tabs";
import type { Machine } from "./use-machine";

export function MachinePanel({
  machine: m,
  busy,
  stop,
  log,
}: {
  machine: Machine;
  busy: boolean;
  stop: () => void;
  log: (message: string) => void;
}) {
  const locked = !m.powered || !m.homed || busy;
  return (
    <section className="panel machine-panel" aria-label="机器控制">
      <div className="panel-heading">
        <div className="heading-label">
          <Crosshair size={17} />
          <h2>机器控制</h2>
        </div>
        <Badge variant="outline">模拟设备</Badge>
      </div>
      <div className="machine-top">
        <span>
          吸嘴 <strong className="mono">N1</strong>
          <span className="text-muted-foreground"> / NT1 · H1</span>
        </span>
        <span className="text-xs text-muted-foreground">
          {!m.powered ? "未使能" : m.homed ? "已模拟回零" : "等待模拟回零"}
        </span>
      </div>
      <Tabs defaultValue="jog">
        <TabsList className="machine-tabs">
          <TabsTrigger value="jog">点动控制</TabsTrigger>
          <TabsTrigger value="actuator">执行器</TabsTrigger>
        </TabsList>
        <TabsContent value="jog" className="m-0">
          <div className="jog-content">
            <div className="jog-column">
              <span className="control-caption">X / Y</span>
              <div className="jog-pad">
                <Button
                  className="col-start-2"
                  variant="outline"
                  size="icon"
                  disabled={locked}
                  aria-label="模拟 Y 正向点动"
                  onClick={() => m.jog("y", 1)}
                >
                  <ArrowUp />
                </Button>
                <Button
                  className="col-start-1"
                  variant="outline"
                  size="icon"
                  disabled={locked}
                  aria-label="模拟 X 负向点动"
                  onClick={() => m.jog("x", -1)}
                >
                  <ArrowLeft />
                </Button>
                <div className="jog-center mono">XY</div>
                <Button
                  variant="outline"
                  size="icon"
                  disabled={locked}
                  aria-label="模拟 X 正向点动"
                  onClick={() => m.jog("x", 1)}
                >
                  <ArrowRight />
                </Button>
                <Button
                  className="col-start-2"
                  variant="outline"
                  size="icon"
                  disabled={locked}
                  aria-label="模拟 Y 负向点动"
                  onClick={() => m.jog("y", -1)}
                >
                  <ArrowDown />
                </Button>
              </div>
            </div>
            <div className="jog-column">
              <span className="control-caption">Z</span>
              <Button
                variant="outline"
                size="icon"
                disabled={locked}
                aria-label="模拟 Z 抬升"
                onClick={() => m.jog("z", 1)}
              >
                <ArrowUp />
              </Button>
              <span className="control-caption">高度</span>
              <Button
                variant="outline"
                size="icon"
                disabled={locked}
                aria-label="模拟 Z 下降"
                onClick={() => m.jog("z", -1)}
              >
                <ArrowDown />
              </Button>
            </div>
            <div className="jog-column">
              <span className="control-caption">C</span>
              <Button
                variant="outline"
                size="icon"
                disabled={locked}
                aria-label="模拟逆时针旋转"
                onClick={() => m.jog("c", -1)}
              >
                <RotateCcw />
              </Button>
              <span className="control-caption">旋转</span>
              <Button
                variant="outline"
                size="icon"
                disabled={locked}
                aria-label="模拟顺时针旋转"
                onClick={() => m.jog("c", 1)}
              >
                <RotateCw />
              </Button>
            </div>
            <div className="jog-setup">
              <div className="flex justify-between gap-2">
                <span className="control-caption">步长</span>
                <span className="text-xs text-muted-foreground">mm / °</span>
              </div>
              <div className="step-options">
                {["0.01", "0.1", "1", "10"].map((step) => (
                  <button
                    key={step}
                    aria-pressed={m.step === step}
                    onClick={() => m.setStep(step)}
                    className={m.step === step ? "active" : ""}
                  >
                    {step}
                  </button>
                ))}
              </div>
              <div className="flex justify-between text-xs mt-4 mb-3">
                <span>速度</span>
                <strong className="mono">{m.speed}%</strong>
              </div>
              <Slider
                aria-label="模拟速度百分比"
                value={[m.speed]}
                onValueChange={(v) => m.setSpeed(v[0])}
                min={10}
                max={100}
                step={10}
              />
            </div>
          </div>
        </TabsContent>
        <TabsContent value="actuator" className="actuator-content">
          <div>
            <div>
              <strong>真空吸附</strong>
              <p className="text-sm text-muted-foreground">
                N1 吸嘴 · 仅改变演示状态
              </p>
            </div>
            <Switch
              aria-label="模拟真空吸附"
              checked={m.vacuum}
              disabled={locked}
              onCheckedChange={(value) => {
                m.setVacuum(value);
                log(`模拟真空${value ? "开启" : "关闭"}`);
              }}
            />
          </div>
          <p className="text-sm text-muted-foreground">压力传感器：未接入</p>
        </TabsContent>
      </Tabs>
      <div className="machine-bottom">
        <Button
          variant={m.powered ? "secondary" : "default"}
          size="sm"
          disabled={busy}
          onClick={m.power}
        >
          <Power />
          {m.powered ? "关闭模拟" : "使能模拟"}
        </Button>
        <Button
          variant="outline"
          size="sm"
          disabled={!m.powered || busy}
          onClick={m.home}
        >
          <Home />
          模拟回零
        </Button>
        <Button
          className="ml-auto text-destructive"
          variant="ghost"
          size="sm"
          onClick={stop}
        >
          <CircleStop />
          停止演示
        </Button>
      </div>
      <div className="axis-readout">
        {(["x", "y", "z", "c"] as const).map((axis) => (
          <div key={axis}>
            <span>{axis.toUpperCase()}</span>
            <strong className="mono">{m.position[axis].toFixed(3)}</strong>
            <small>{axis === "c" ? "°" : "mm"}</small>
          </div>
        ))}
      </div>
    </section>
  );
}
