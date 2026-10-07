import { describe, it, expect, vi } from "vitest";
import { render, screen, fireEvent, act } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import App from "./app";

describe("SMT interface simulation", () => {
  it("requires enable and homing, locks manual control during a task and supports pause/stop", async () => {
    const user = userEvent.setup();
    render(<App />);
    expect(
      (screen.getByRole("button", { name: "运行演示" }) as HTMLButtonElement)
        .disabled,
    ).toBe(true);
    await user.click(screen.getByRole("button", { name: "使能模拟" }));
    expect(
      (screen.getByRole("button", { name: "运行演示" }) as HTMLButtonElement)
        .disabled,
    ).toBe(true);
    await user.click(screen.getByRole("button", { name: "模拟回零" }));
    const jog = screen.getByRole("button", {
      name: "模拟 X 正向点动",
    }) as HTMLButtonElement;
    expect(jog.disabled).toBe(false);
    await user.click(jog);
    expect(screen.getByText("1.000")).toBeTruthy();
    await user.click(screen.getByRole("button", { name: "运行演示" }));
    expect(jog.disabled).toBe(true);
    expect(
      (screen.getByRole("button", { name: "编辑 R1" }) as HTMLButtonElement)
        .disabled,
    ).toBe(true);
    await user.click(screen.getByRole("button", { name: "暂停演示" }));
    expect(screen.getByRole("button", { name: "继续演示" })).toBeTruthy();
    expect(jog.disabled).toBe(true);
    await user.click(screen.getByRole("button", { name: "停止演示" }));
    expect(jog.disabled).toBe(false);
    expect(screen.getByText("已停止")).toBeTruthy();
  });

  it("retains the current job when CSV is invalid and accepts a valid replacement", async () => {
    render(<App />);
    const input = screen.getByLabelText("导入贴片坐标 CSV");
    const invalid = new File(["bad"], "bad.csv", { type: "text/csv" });
    Object.defineProperty(invalid, "text", { value: async () => "bad" });
    await act(async () =>
      fireEvent.change(input, { target: { files: [invalid] } }),
    );
    expect(screen.getByRole("alert").textContent).toContain("表头");
    expect(screen.getByRole("button", { name: "编辑 R1" })).toBeTruthy();
    const valid = new File([""], "new-board.csv", { type: "text/csv" });
    Object.defineProperty(valid, "text", {
      value: async () =>
        "id,part,package,x,y,rotation,side\nTEST1,10k,0603,3,4,90,Top",
    });
    await act(async () =>
      fireEvent.change(input, { target: { files: [valid] } }),
    );
    expect(screen.getByRole("button", { name: "编辑 TEST1" })).toBeTruthy();
    expect(screen.queryByRole("button", { name: "编辑 R1" })).toBeNull();
    expect(screen.queryByRole("alert")).toBeNull();
  });

  it("registers an optional read-only preview tool and rejects invalid arguments", async () => {
    const registerTool = vi.fn();
    Object.defineProperty(document, "modelContext", {
      configurable: true,
      value: { registerTool },
    });
    const view = render(<App />);
    const [tool, options] = registerTool.mock.calls[0];
    expect(tool.name).toBe("get_smt_preview_state");
    expect(tool.annotations.readOnlyHint).toBe(true);
    expect(tool.execute({})).toMatchObject({
      mode: "simulation",
      total: 12,
      completed: 0,
    });
    expect(() => tool.execute({ move: true })).toThrow();
    view.unmount();
    expect(options.signal.aborted).toBe(true);
    Reflect.deleteProperty(document, "modelContext");
  });
});
