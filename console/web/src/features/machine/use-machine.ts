import { useState } from "react";
export type Axis = "x" | "y" | "z" | "c";
export function useMachine(
  log: (message: string, level?: "info" | "warning") => void,
) {
  const [powered, setPowered] = useState(false);
  const [homed, setHomed] = useState(false);
  const [position, setPosition] = useState({ x: 0, y: 0, z: 0, c: 0 });
  const [vacuum, setVacuum] = useState(false);
  const [step, setStep] = useState("1");
  const [speed, setSpeed] = useState(50);
  function power() {
    setPowered((value) => !value);
    setHomed(false);
    setVacuum(false);
    log(powered ? "演示电源已关闭" : "演示电源已开启，请先模拟回零");
  }
  function home() {
    if (!powered) return;
    setPosition({ x: 0, y: 0, z: 0, c: 0 });
    setHomed(true);
    log("模拟回零完成，坐标已归零");
  }
  function jog(axis: Axis, direction: number) {
    if (!powered || !homed) return;
    setPosition((p) => ({
      ...p,
      [axis]: Number((p[axis] + direction * Number(step)).toFixed(3)),
    }));
    log(
      `模拟点动 ${axis.toUpperCase()} ${direction > 0 ? "+" : "−"}${step}${axis === "c" ? "°" : " mm"}`,
    );
  }
  return {
    powered,
    homed,
    position,
    setPosition,
    vacuum,
    setVacuum,
    step,
    setStep,
    speed,
    setSpeed,
    power,
    home,
    jog,
  };
}
export type Machine = ReturnType<typeof useMachine>;
