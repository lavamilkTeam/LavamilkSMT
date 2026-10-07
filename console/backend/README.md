# Console Go 面板服务

此目录用于上位机控制面板的 Go 后端，部署在上位机，为面板界面提供业务接口。
现有界面为 `../frontend/` Java 应用，`../web/` 为 React 原型；笔记本、手机是访问终端。
目前 `cmd/smt-panel` 已提供静态 Web 服务和 `/healthz`，与桌面壳共用 `console/go.mod`。
开发板的 Python `controller/` 独立维护；以下工程管理和机器业务 API 仍未实现。

## 当前入口

仓库根目录执行 `./console/build-panel.sh web`，随后运行
`./console/build/bin/smt-panel --listen 0.0.0.0:8080`。
默认监听 `127.0.0.1:8080`；部署模板显式监听所有 IPv4 接口，供局域网浏览器访问。
该进程不需要图形环境或 Node.js，也不引用 Wails。
`GET /healthz` 只表示页面服务状态，固定报告 `mode=simulation`、`machine_connected=false`。
静态入口仅接受 GET/HEAD，未知资源/API 返回 404，不把 HTML 当成成功业务响应。
当前无业务写入接口或用户认证；后续加入远程控制时必须在协议层定义访问权限与控制权。
部署方式和验证状态见 [桌面与 kiosk 接入](../desktop/README.md)。

## 后续业务职责

- 解析 BOM、贴装坐标等输入文件，校验并转换为统一工程模型。
- 管理本地工程、配置、物料和任务草稿的保存与加载。
- 将准备好的任务形成固定版本，通过控制协议提交给同机 Python controller。
- 管理与 controller 的连接、心跳、重连及状态同步；面板保存的状态是观察副本。
- 向前端提供业务接口、状态事件和预览访问能力；视频的直连或转发方式在 API 设计时确定。
- 经 Python controller 协调维护准入，再由独立 updater 执行升级；不在面板后端内执行系统安装或 ST-LINK 烧录。

## 边界

- 前端负责交互和展示，后端处理业务及通信，具体本地传输协议后续确定。
- controller 拥有机器状态和实际任务执行权；即使在同一台上位机运行，面板也须通过接口查询和请求操作。
- 点动、回零、开始任务等均经板端机器协调器仲裁，Go 服务不直接操作下位机或发送 STEP/DIR。
- 视觉测量和运动时序继续由板端处理；视频预览不能作为定位完成或停稳后有效帧的证明。
- 界面、面板后端、controller 与 updater 的契约统一放仓库根目录 `protocols/`。
- [host-updater](../../host-updater/README.md) 更新上位机自身，[mcu-updater](../../mcu-updater/README.md) 更新 STM32；两者独立进程运行，面板断开不代表更新已取消。

## 迁移顺序

先定义前端需要的最小 API，再实现设备会话、状态和预览接入；随后接入工程导入与任务准备。
`frontend/` 当前保留 OpenPnP 原有业务、视觉和硬件模块以保证可构建。
后续按功能把相关调用切换到 Go 接口，再移除不再使用的 Java 实现，避免两套逻辑同时执行任务。
