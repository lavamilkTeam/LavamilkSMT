import { useState } from "react";
import { Check, CircleAlert, Save, Settings2 } from "lucide-react";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Badge } from "@/shared/ui/badge";
import { Switch } from "@/shared/ui/switch";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/shared/ui/table";
import { Slider } from "@/shared/ui/slider";
import { BoardPreview } from "@/features/job/board-preview";
import type { Placement } from "@/features/job/model";

export function ConfigurationPanel({
  page,
  rows,
  selected,
  select,
  log,
}: {
  page: string;
  rows: Placement[];
  selected: string | null;
  select: (id: string) => void;
  log: (message: string) => void;
}) {
  const [threshold, setThreshold] = useState(127);
  const [crosscheck, setCrosscheck] = useState(true);
  const [machineName, setMachineName] = useState("SMT 桌面贴片机");
  const [draftSaved, setDraftSaved] = useState(false);
  const labels: Record<string, string> = {
    panels: "拼板",
    boards: "单板",
    parts: "元件库",
    packages: "封装库",
    vision: "视觉",
    feeders: "飞达",
    machine: "机器配置",
    issues: "问题与解决方案",
  };
  const groups = [...new Set(rows.map((p) => p.package))];
  const parts = [
    ...new Map(rows.map((p) => [`${p.part}/${p.package}`, p])).values(),
  ];
  return (
    <div className="configuration-panel">
      <div className="configuration-heading">
        <div>
          <p className="eyebrow">WORKSPACE / CONFIGURATION</p>
          <h2>{labels[page]}</h2>
        </div>
        <Badge variant="outline">本地预览</Badge>
      </div>
      {["parts", "packages", "feeders"].includes(page) && (
        <>
          <p className="configuration-note">
            {page === "feeders"
              ? "以下为示例任务的建议料位，尚未配置实际送料器。"
              : "根据当前任务汇总；在贴装列表中编辑元件后，这里会同步更新。"}
          </p>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>
                  {page === "feeders" ? "建议料位" : "编号"}
                </TableHead>
                <TableHead>{page === "packages" ? "封装" : "元件"}</TableHead>
                <TableHead>
                  {page === "packages" ? "元件数量" : "封装"}
                </TableHead>
                <TableHead>状态</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {page === "packages"
                ? groups.map((pkg, i) => (
                    <TableRow key={pkg}>
                      <TableCell className="mono">
                        {String(i + 1).padStart(2, "0")}
                      </TableCell>
                      <TableCell>{pkg}</TableCell>
                      <TableCell>
                        {rows.filter((p) => p.package === pkg).length}
                      </TableCell>
                      <TableCell>
                        <Badge variant="secondary">待标定</Badge>
                      </TableCell>
                    </TableRow>
                  ))
                : parts.map((p, i) => (
                    <TableRow key={`${p.part}/${p.package}`}>
                      <TableCell className="mono">
                        {page === "feeders" ? "F" : ""}
                        {String(i + 1).padStart(2, "0")}
                      </TableCell>
                      <TableCell>{p.part}</TableCell>
                      <TableCell>{p.package}</TableCell>
                      <TableCell>
                        <Badge variant="secondary">
                          {page === "feeders" ? "未绑定硬件" : "任务引用"}
                        </Badge>
                      </TableCell>
                    </TableRow>
                  ))}
            </TableBody>
          </Table>
        </>
      )}
      {["boards", "panels"].includes(page) && (
        <>
          <p className="configuration-note">
            {page === "panels"
              ? "当前预览包含一块单板，未配置重复拼板。"
              : "当前任务的元件坐标分布。选择元件可在贴装列表中继续编辑。"}
          </p>
          <div className="large-board">
            <BoardPreview
              rows={rows}
              selected={selected}
              select={select}
              completed={[]}
            />
          </div>
          <div className="detail-grid">
            <div>
              <span>单板数量</span>
              <strong>1</strong>
            </div>
            <div>
              <span>元件数量</span>
              <strong>{rows.length}</strong>
            </div>
            <div>
              <span>坐标单位</span>
              <strong>mm</strong>
            </div>
          </div>
        </>
      )}
      {page === "vision" && (
        <div className="settings-form">
          <div className="inline-notice">
            <CircleAlert size={16} />
            参数仅在当前页面预览中保存，不影响真实视频处理。
          </div>
          <label className="field-label">
            二值化阈值 <span className="mono">{threshold}</span>
            <Slider
              className="my-4"
              aria-label="二值化阈值草稿"
              value={[threshold]}
              onValueChange={(v) => {
                setThreshold(v[0]);
                setDraftSaved(false);
              }}
              min={0}
              max={255}
            />
          </label>
          <div className="setting-line">
            <div>
              <strong>定位质量检查</strong>
              <p>记录为标定参数草稿</p>
            </div>
            <Switch
              aria-label="定位质量检查草稿"
              checked={crosscheck}
              onCheckedChange={(value) => {
                setCrosscheck(value);
                setDraftSaved(false);
              }}
            />
          </div>
          <div className="setting-line">
            <span>像素当量</span>
            <Badge variant="outline">未标定</Badge>
          </div>
          <div className="setting-line">
            <span>相机与吸嘴偏移</span>
            <Badge variant="outline">未标定</Badge>
          </div>
          <Button
            onClick={() => {
              setDraftSaved(true);
              log(
                `保存视觉草稿：阈值 ${threshold}，质量检查${crosscheck ? "开启" : "关闭"}（未下发）`,
              );
            }}
          >
            {draftSaved ? <Check /> : <Save />}
            {draftSaved ? "已记录草稿" : "记录参数草稿"}
          </Button>
        </div>
      )}
      {page === "machine" && (
        <div className="settings-form">
          <label className="field-label">
            设备名称草稿
            <Input
              value={machineName}
              maxLength={80}
              onChange={(e) => {
                setMachineName(e.target.value);
                setDraftSaved(false);
              }}
            />
          </label>
          <div className="setting-line">
            <span>运动控制板</span>
            <strong className="mono">STM32F407ZGT6</strong>
          </div>
          <div className="setting-line">
            <span>贴片头 / 吸嘴</span>
            <strong>H1 / N1</strong>
          </div>
          <div className="setting-line">
            <span>轴配置</span>
            <strong className="mono">X · Y · Z · C</strong>
          </div>
          <div className="setting-line">
            <span>硬件控制连接</span>
            <Badge variant="outline">未接入</Badge>
          </div>
          <Button
            disabled={!machineName.trim()}
            onClick={() => {
              setDraftSaved(true);
              log(`设备名称草稿：${machineName}（未下发）`);
            }}
          >
            {draftSaved ? <Check /> : <Save />}
            {draftSaved ? "已记录草稿" : "记录配置草稿"}
          </Button>
        </div>
      )}
      {page === "issues" && (
        <div className="issue-list">
          {[
            ["运动控制尚未接入", "当前按钮只改变模拟状态，不能控制实际机器。"],
            [
              "相机尚未标定",
              "真实定位前需要建立像素当量、畸变和吸嘴偏移标定。",
            ],
            [
              "工程兼容性",
              "当前支持示例任务和指定格式的 CSV；尚未迁移 OpenPnP XML 工程。",
            ],
          ].map(([title, body]) => (
            <div className="issue" key={title}>
              <Settings2 size={20} />
              <div>
                <strong>{title}</strong>
                <p>{body}</p>
              </div>
              <Badge variant="secondary">待接入</Badge>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
