# React 操作界面预览

以当前 OpenPnP 中文 Swing 主界面为功能布局参考，使用 **React + TypeScript + shadcn/ui + Tailwind CSS + Vite** 实现的本地前端原型。保留左侧相机和机器控制、右侧任务及配置页的操作顺序。

这是新的界面试用入口；原有 `console/frontend/` Java 应用、目标板服务和 Python controller 未被替换。
已增加 Wails 桌面壳与 Go 静态页面服务，尚未接机器业务 API 或运动控制。

## 开发

需要 Node.js 22.12+（本次使用 22.23.2）和 npm。从仓库根目录执行：

```bash
cd console/web
npm ci
npm run dev
```

打开终端打印的 `http://127.0.0.1:5173/`。可使用 VS Code 的集成浏览器。开发服务默认仅监听本机，端口占用时明确失败，不自动跳到其他端口。

Linux/macOS 也可在仓库根目录执行 `./console/dev-web.sh`。该脚本优先使用系统 Node.js；未安装时，可使用本机已经准备的 `toolchains/node/bin`。此工具链目录不提交 Git。

```bash
npm test           # CSV 解析、演示状态与导入交互测试
npm run lint:boundaries # 模块入口、依赖方向与循环检查
npm run check     # 边界检查、现有测试、TypeScript 与构建
npm run build     # TypeScript 检查和生产构建
npm run preview   # 在 4173 端口查看 dist/ 的生产构建
```

`dist/` 仅包含静态文件。`../build-panel.sh` 将它复制到 Go 模块的生成目录，
由独立 HTTP 服务或 Wails 内嵌资源服务提供；目标板运行时不需要 Node.js。
桌面、纯 Web 和开机 kiosk 的入口见 [desktop/README.md](../desktop/README.md)。
本次仍未修改目标板部署；这些新入口按用户要求暂未构建测试。

## 可以试用的功能

- 默认载入 12 个示例元件。支持新建、恢复示例、按位号/元件/封装搜索及按板层筛选。
- 支持修改元件参数、禁用或移除元件；元件库、封装库和建议料位随任务更新。
- PCB 坐标示意图可以选择元件，示意图不是实际 PCB 外形或相机标定结果。
- 点击“使能模拟”再“模拟回零”，可以点动 XYZ/C；演示坐标没有实际机器行程约束，不可当作运动实现。
- 运行、暂停、继续、停止任务演示。运行及暂停期间禁止点动和任务修改；任务时序仅用于界面演示，未发送硬件指令。
- 视觉/机器配置页提供本次页面会话的草稿；未调用后端、未影响实际设备。切换页签不保证保留未提交的草稿，刷新恢复初始数据。
- 运行日志记录最近 200 个事件，支持导出和清空。
- 相机区域默认使用已纳入仓库的 OpenPnP 测试图，明确标为非实时。支持缩放、十字线与放大视图。
- “视频源”可手动指定现有 Python 预览服务，例如 `http://192.168.1.100:8766`，读取灰度、二值化或轮廓 MJPEG。此连接只读取视频，不代表运动连接成功，也不提供有效定位帧保证。预览机必须能访问该地址，HTTPS 页面不能直接加载 HTTP 流。

所有任务数据当前位于浏览器内存，刷新会丢失修改。不存在自动恢复任务、真实回零、真实贴装或远程急停功能。

## CSV

导入文件限制为 2 MiB、1–5000 行元件数据，表头如下：

```csv
id,part,package,x,y,rotation,side
R1,10k,0603,12.4,18.2,0,Top
C1,100nF,0603,36.8,22.4,90,Bottom
```

位号不重复，坐标单位 mm、角度单位度，板层是 `Top` 或 `Bottom`。支持 UTF-8 BOM、CRLF、带引号的单元格、逗号和转义引号。整个文件校验成功后才替换当前任务，失败时保留原任务。

导出只保存上述坐标字段，不包含启用状态、演示进度、标定和机器配置。暂不支持 OpenPnP `.job.xml` / `.board.xml`。

## Feature First

```text
src/
├── app/                   # 工作台布局、跨功能组合、演示任务协调
├── features/
│   ├── camera/            # 视频源、画面和视图控制
│   ├── machine/           # 模拟点动和执行器
│   ├── job/               # 元件模型、CSV、表格与坐标示意图
│   ├── configuration/     # 元件/封装/飞达汇总及配置草稿
│   └── logs/              # 操作日志
└── shared/
    ├── ui/                # shadcn/ui 组件
    └── lib/               # 通用函数和可选只读预览工具
```

功能内部管理自己的组件、状态和类型。`app/` 负责组合，跨功能引用保持显式。这里的模拟机器状态是前端原型代码，接入后端时需使用真实状态订阅替换，不能直接变成真实控制协议。

模块根文件作为聚焦的公开入口，嵌套目录为私有实现；跨模块只引用根文件。
依赖方向为 app → features/shared、features → shared，允许无环的显式功能间公开引用。
Web 不依赖 Wails bindings。规则见 [AGENTS.md](AGENTS.md) 和 `.dependency-cruiser.cjs`；
新增规则尚待执行正反例验证。

界面包含可选的只读 WebMCP `get_smt_preview_state`，仅在浏览器提供对应 API 时注册，返回演示任务摘要，不暴露硬件操作。已用测试替身检查参数和生命周期；当前环境没有可用的 WebMCP 浏览器，未完成真实浏览器端验证。

本次验证包含生产构建、数据测试、jsdom 组件交互测试及本地 HTTP 冒烟检查。当前会话无法打开内嵌浏览器，尚未做真实浏览器视觉验收或真实相机连接验收。

素材来源与许可证见 [NOTICE.md](NOTICE.md)。
