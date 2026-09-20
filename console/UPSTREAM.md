# OpenPnP 源码来源与集成边界

- 上游：https://github.com/openpnp/openpnp
- 导入提交：`5bd404cfc70f34103a3ca0fbb6b50c2b465f407c`
- 导入日期：2026-09-14。
- 位置：`console/frontend/`，直接纳入本仓库，不是 Git 子模块。
- 许可证：[上游 GPL-3.0 许可证](frontend/LICENSE.txt)；保留源码中的原作者版权和许可证声明。
- 校验记录：[openpnp-upstream.json](openpnp-upstream.json)，记录导入时每个文件的 SHA-256。

导入完整上游桌面应用源码、资源、测试、示例与构建定义。没有导入上游 `.git` 历史，
并排除不参与本地构建的 `.github/` 工作流、赞助配置与发布签名材料。
Java、Maven、依赖 JAR 和构建结果不纳入版本控制。上游内嵌组件及依赖保留各自许可约定。
校验记录保存的是**导入时的上游基线**，不代表本地修改后文件的校验值。
本仓库的构建和启动包装脚本位于 `frontend/` 外层。

本地修改包括简体中文资源补全与术语校订、写死界面文案的资源化、
视觉步骤及参数的中文显示、枚举显示适配、诊断消息与问题标识分离，以及默认中文启动。
源码原有版权和许可证声明保留。范围和维护方法见 [LOCALIZATION.md](LOCALIZATION.md)。

## 为什么包含机器与视觉模块

OpenPnP 的 Swing 界面直接引用 `model`、`spi`、`machine`、`vision` 等模块。
只复制 `gui/` 无法构建可运行应用，因此保留其依赖的完整 Java 应用。

当前这是可独立构建、已加入中文本土化的 OpenPnP 界面，机器控制业务尚未适配 Python controller；下位机更新查询已独立接入：

目录现按 `frontend/` 与 `backend/` 划分；`backend/` 预留给上位机控制面板的 Go 后端。
笔记本或手机是访问终端；本地开发预览不改变 console 属于上位机软件的定位。
此调整仅建立目录和职责边界，Java 业务模块尚未迁移，前后端 API 尚未实现。

- Java 更新客户端已使用 TCP 8765 握手与更新检查；mDNS 发现及常规机器状态界面尚未适配。
- TCP 8766 的预览图像尚未嵌入 OpenPnP 界面。
- OpenPnP 自身具有任务执行、视觉和硬件驱动功能，不能把这些职责直接视为已委托给 Python controller。
- 后续需将任务执行适配到 controller，避免 OpenPnP 和 controller 各自执行同一贴装流程。
- 计划由界面调用上位机 Go 面板后端，再由 Go 适配 controller 协议；真实任务执行权归 controller。
- 新增「机器 → 下位机固件更新…」独立入口，`McuUpdateClient` 使用现有 TCP 握手经 Python 查询 Go 升级服务；当前只检查更新条件，不执行烧录，也未接通其他机器业务接口。

## 主要代码入口

| 路径（相对 `frontend/src/main/java/org/openpnp/`） | 职责 |
| --- | --- |
| `Main.java` | 应用启动 |
| `gui/MainFrame.java` | 主窗口 |
| `gui/JobPanel.java` | 任务界面 |
| `gui/MachineControlsPanel.java` | 机器操作界面 |
| `gui/components/CameraPanel.java` | 相机画面组件 |
| `gui/importer/` | 板文件导入 |
| `model/` | 工程、元件和配置模型 |
| `spi/` | 机器、相机、驱动等接口 |
| `machine/`、`vision/` | 现有执行与视觉实现 |

## 后续维护

更新上游时以记录的提交为基线比较变化，同时更新校验记录和本文件。
同步更新 `build-frontend.sh` 中的 `openpnp.version`，它让应用内源码链接指向导入基线。
对上游文件做本地改动时在这里记录改动范围，保留原版权声明。
`frontend/README.md` 为上游原文；本项目使用说明以 [console/README.md](README.md) 为准。
