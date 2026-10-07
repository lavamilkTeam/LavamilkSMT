import { useState } from "react";
import { Pencil, Search, Trash2, SlidersHorizontal } from "lucide-react";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Badge } from "@/shared/ui/badge";
import { Checkbox } from "@/shared/ui/checkbox";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/shared/ui/table";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/shared/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/ui/select";
import type { Placement } from "./model";

type Props = {
  rows: Placement[];
  completed: string[];
  selected: string | null;
  select: (id: string) => void;
  update: (row: Placement) => void;
  remove: (id: string) => void;
  locked: boolean;
};
export function PlacementTable({
  rows,
  completed,
  selected,
  select,
  update,
  remove,
  locked,
}: Props) {
  const [query, setQuery] = useState("");
  const [side, setSide] = useState("all");
  const [edit, setEdit] = useState<Placement | null>(null);
  const filtered = rows.filter(
    (p) =>
      `${p.id} ${p.part} ${p.package}`
        .toLowerCase()
        .includes(query.toLowerCase()) &&
      (side === "all" || p.side === side),
  );
  return (
    <div className="placement-section">
      <div className="placement-toolbar">
        <div className="heading-label">
          <h3>贴装列表</h3>
          <Badge variant="secondary" className="mono">
            {rows.length}
          </Badge>
        </div>
        <div className="flex items-center gap-2">
          <Select value={side} onValueChange={setSide}>
            <SelectTrigger aria-label="筛选板层" className="h-8 w-28">
              <SlidersHorizontal size={14} />
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">全部板层</SelectItem>
              <SelectItem value="Top">顶层</SelectItem>
              <SelectItem value="Bottom">底层</SelectItem>
            </SelectContent>
          </Select>
          <div className="search-field">
            <Search size={15} />
            <Input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              aria-label="搜索元件"
              placeholder="搜索位号、元件…"
              className="h-8"
            />
          </div>
        </div>
      </div>
      <div className="placement-scroll">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-10">启用</TableHead>
              <TableHead>位号</TableHead>
              <TableHead>元件 / 封装</TableHead>
              <TableHead className="text-right">
                X <small>mm</small>
              </TableHead>
              <TableHead className="text-right">
                Y <small>mm</small>
              </TableHead>
              <TableHead className="text-right">角度</TableHead>
              <TableHead>板层</TableHead>
              <TableHead>状态</TableHead>
              <TableHead className="w-20">操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {filtered.map((p) => (
              <TableRow
                key={p.id}
                data-state={selected === p.id ? "selected" : undefined}
                onClick={() => select(p.id)}
                className={!p.enabled ? "opacity-50" : ""}
              >
                <TableCell>
                  <Checkbox
                    aria-label={`启用 ${p.id}`}
                    checked={p.enabled}
                    disabled={locked}
                    onCheckedChange={(checked) =>
                      update({ ...p, enabled: checked === true })
                    }
                  />
                </TableCell>
                <TableCell>
                  <button
                    className="row-reference mono"
                    onClick={() => select(p.id)}
                  >
                    {p.id}
                  </button>
                </TableCell>
                <TableCell>
                  <span>{p.part}</span>
                  <span className="package-label mono">{p.package}</span>
                </TableCell>
                <TableCell className="text-right mono">
                  {p.x.toFixed(2)}
                </TableCell>
                <TableCell className="text-right mono">
                  {p.y.toFixed(2)}
                </TableCell>
                <TableCell className="text-right mono">{p.rotation}°</TableCell>
                <TableCell>
                  <span className="layer-label">
                    {p.side === "Top" ? "顶层" : "底层"}
                  </span>
                </TableCell>
                <TableCell>
                  <span
                    className={`placement-status ${completed.includes(p.id) ? "done" : ""}`}
                  >
                    <span />
                    {!p.enabled
                      ? "跳过"
                      : completed.includes(p.id)
                        ? "已演示"
                        : "待演示"}
                  </span>
                </TableCell>
                <TableCell>
                  <div className="flex">
                    <Button
                      variant="ghost"
                      size="icon"
                      className="size-7"
                      aria-label={`编辑 ${p.id}`}
                      disabled={locked}
                      onClick={() => setEdit({ ...p })}
                    >
                      <Pencil className="size-3.5" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon"
                      className="size-7 text-muted-foreground"
                      aria-label={`移除 ${p.id}`}
                      disabled={locked}
                      onClick={() => remove(p.id)}
                    >
                      <Trash2 className="size-3.5" />
                    </Button>
                  </div>
                </TableCell>
              </TableRow>
            ))}
            {!filtered.length && (
              <TableRow>
                <TableCell
                  colSpan={9}
                  className="py-12 text-center text-muted-foreground"
                >
                  {rows.length
                    ? "没有匹配的元件，请调整搜索或板层。"
                    : "当前任务为空。可以载入示例或导入 CSV。"}
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </div>
      <div className="table-footer">
        <span>
          显示 {filtered.length} / {rows.length} 个元件
        </span>
        <span>坐标系：PCB 局部 · 单位：mm</span>
      </div>
      <Dialog
        open={!!edit}
        onOpenChange={(open) => {
          if (!open) setEdit(null);
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>编辑元件 · {edit?.id}</DialogTitle>
            <DialogDescription>
              修改当前演示任务。保存后会清除任务演示进度。
            </DialogDescription>
          </DialogHeader>
          {edit && (
            <form
              onSubmit={(e) => {
                e.preventDefault();
                if (
                  [edit.x, edit.y, edit.rotation].every(Number.isFinite) &&
                  edit.part.trim() &&
                  edit.package.trim()
                ) {
                  update(edit);
                  setEdit(null);
                }
              }}
            >
              <div className="form-grid">
                {(["part", "package", "x", "y", "rotation"] as const).map(
                  (key) => (
                    <label key={key} className="field-label">
                      {
                        {
                          part: "元件",
                          package: "封装",
                          x: "X / mm",
                          y: "Y / mm",
                          rotation: "角度 / °",
                        }[key]
                      }
                      <Input
                        required
                        type={
                          ["x", "y", "rotation"].includes(key)
                            ? "number"
                            : "text"
                        }
                        step="any"
                        value={
                          typeof edit[key] === "number" &&
                          !Number.isFinite(edit[key])
                            ? ""
                            : edit[key]
                        }
                        onChange={(e) =>
                          setEdit({
                            ...edit,
                            [key]: ["x", "y", "rotation"].includes(key)
                              ? e.target.value === ""
                                ? NaN
                                : Number(e.target.value)
                              : e.target.value,
                          })
                        }
                      />
                    </label>
                  ),
                )}
                <label className="field-label">
                  板层
                  <Select
                    value={edit.side}
                    onValueChange={(value) =>
                      setEdit({ ...edit, side: value as Placement["side"] })
                    }
                  >
                    <SelectTrigger className="w-full">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="Top">顶层</SelectItem>
                      <SelectItem value="Bottom">底层</SelectItem>
                    </SelectContent>
                  </Select>
                </label>
              </div>
              <DialogFooter className="mt-6">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => setEdit(null)}
                >
                  取消
                </Button>
                <Button type="submit">保存修改</Button>
              </DialogFooter>
            </form>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}
