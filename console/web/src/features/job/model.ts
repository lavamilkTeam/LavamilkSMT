export type Side = "Top" | "Bottom";
export type Placement = {
  id: string;
  part: string;
  package: string;
  x: number;
  y: number;
  rotation: number;
  side: Side;
  enabled: boolean;
};
export type JobState = "idle" | "running" | "paused" | "completed" | "stopped";

export const samplePlacements: Placement[] = [
  ["R1", "10kΩ", "0603", 12.4, 18.2, 0],
  ["R2", "10kΩ", "0603", 16.2, 18.2, 0],
  ["R3", "4.7kΩ", "0603", 24.5, 31.6, 90],
  ["R4", "100Ω", "0402", 28.1, 31.6, 90],
  ["C1", "100nF", "0603", 36.8, 22.4, 0],
  ["C2", "100nF", "0603", 40.2, 22.4, 0],
  ["C3", "10µF", "0805", 18.6, 42.8, 90],
  ["C4", "1µF", "0603", 43.8, 38.1, 180],
  ["D1", "LED 绿", "0805", 55.2, 14.6, 0],
  ["D2", "LED 红", "0805", 55.2, 20.6, 0],
  ["U1", "稳压器", "SOT-23-5", 31.8, 46.2, 90],
  ["U2", "接口芯片", "SOIC-8", 51.2, 46.2, 0],
].map(([id, part, pkg, x, y, rotation]) => ({
  id: String(id),
  part: String(part),
  package: String(pkg),
  x: Number(x),
  y: Number(y),
  rotation: Number(rotation),
  side: "Top",
  enabled: true,
}));

// Small RFC 4180-style reader: quoted cells, commas, CRLF and escaped quotes.
export function parsePlacementCsv(input: string): Placement[] {
  const text = input.replace(/^\uFEFF/, "");
  const rows: string[][] = [];
  let row: string[] = [],
    cell = "",
    quoted = false,
    closed = false;
  for (let i = 0; i < text.length; i++) {
    const ch = text[i];
    if (quoted) {
      if (ch === '"' && text[i + 1] === '"') {
        cell += '"';
        i++;
      } else if (ch === '"') {
        quoted = false;
        closed = true;
      } else cell += ch;
    } else if (ch === "," || ch === "\n" || ch === "\r") {
      row.push(cell.trim());
      cell = "";
      closed = false;
      if (ch !== ",") {
        if (row.some(Boolean)) rows.push(row);
        row = [];
        if (ch === "\r" && text[i + 1] === "\n") i++;
      }
    } else if (ch === '"') {
      if (cell || closed) throw new Error("CSV 引号格式不正确");
      quoted = true;
    } else {
      if (closed && ch.trim()) throw new Error("CSV 引号后存在多余字符");
      cell += ch;
    }
  }
  if (quoted) throw new Error("CSV 存在未闭合的引号");
  row.push(cell.trim());
  if (row.some(Boolean)) rows.push(row);
  const header = rows.shift()?.map((value) => value.toLowerCase()) ?? [];
  const required = ["id", "part", "package", "x", "y", "rotation", "side"];
  if (required.some((key) => !header.includes(key)))
    throw new Error("CSV 需要 id,part,package,x,y,rotation,side 表头");
  if (new Set(header).size !== header.length)
    throw new Error("CSV 表头不能重复");
  if (!rows.length || rows.length > 5000)
    throw new Error("请导入包含 1–5000 个元件的文件");
  const ids = new Set<string>();
  return rows.map((cells, index) => {
    const at = (key: string) => cells[header.indexOf(key)] ?? "";
    const fail = (reason: string): never => {
      throw new Error(`第 ${index + 2} 行：${reason}`);
    };
    if (cells.length !== header.length) fail("列数与表头不一致");
    const id = at("id"),
      part = at("part"),
      pkg = at("package");
    if (!id || !part || !pkg) fail("位号、元件、封装不能为空");
    if (ids.has(id)) fail(`位号 ${id} 重复`);
    ids.add(id);
    const number = (key: string) => {
      const value = Number(at(key));
      if (!at(key) || !Number.isFinite(value)) fail(`${key} 必须是有限数字`);
      return value;
    };
    const side = at("side").toLowerCase();
    if (!["top", "bottom"].includes(side)) fail("side 必须为 Top 或 Bottom");
    return {
      id,
      part,
      package: pkg,
      x: number("x"),
      y: number("y"),
      rotation: number("rotation"),
      side: side === "top" ? "Top" : "Bottom",
      enabled: true,
    };
  });
}

export function encodePlacementCsv(rows: Placement[]): string {
  const escape = (value: unknown) => `"${String(value).replaceAll('"', '""')}"`;
  return (
    "id,part,package,x,y,rotation,side\r\n" +
    rows
      .map((p) =>
        [p.id, p.part, p.package, p.x, p.y, p.rotation, p.side]
          .map(escape)
          .join(","),
      )
      .join("\r\n")
  );
}

export function nextPending(rows: Placement[], completed: string[]) {
  return rows.find((row) => row.enabled && !completed.includes(row.id));
}
