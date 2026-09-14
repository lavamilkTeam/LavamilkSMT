# OpenPnP 控制界面

笔记本负责制板文件处理、准备贴装任务，通过以太网与板端通信。
桌面端已导入 OpenPnP（Java Swing、GPL-3.0），源码位于 `frontend/`。
`backend/` 预留给后续自行开发的 Go 后端，运行在控制笔记本上。
目前前端保留上游应用功能并加入简体中文本土化，Go 后端尚未实现，两者还没有完成运行时解耦。
开发板上的 `controller/` 仍是独立的 Python 视觉与设备控制程序。
当前中文版也部署到上位机，由板端运行 Swing 会话、浏览器远程访问。
部署入口、服务管理和能力边界见 [目标板部署](../docs/console-deployment.md)。

## OpenPnP 桌面端

```text
console/
├── frontend/               # Java Swing 桌面端，暂保留上游依赖的业务模块
├── backend/                # Go 本地后端预留，职责约定见其 README
├── openpnp-upstream.json    # 上游提交及逐文件校验值
├── UPSTREAM.md             # 来源、许可证、源码入口、适配边界
├── build-frontend.sh        # 构建并执行上游测试
├── run-frontend.sh          # 启动，使用本仓库独立配置目录
├── dev-frontend.sh          # 在控制端监听 0.0.0.0:6080，通过浏览器操作 Swing 界面
├── setup-dev-frontend.sh    # Debian/Ubuntu 开发显示依赖准备
├── tools/                  # 开发工具，不属于 Go 业务后端
└── examples/               # 本项目 Python 通信示例
```

Linux/macOS 安装 JDK 17 和 Maven 3.9 后，从仓库根目录执行：

```bash
./console/build-frontend.sh
./console/run-frontend.sh
```

构建首次运行需联网下载 Maven 依赖。脚本也可使用本机已准备的
`toolchains/openpnp/jdk` 和 `toolchains/openpnp/maven`，这些工具未纳入 Git。
运行需要图形桌面；配置保存到已忽略的 `local/openpnp/`，避免覆盖其他 OpenPnP 安装的配置。
首次使用上游默认模拟配置，真实硬件和本项目后端连接需要另行配置/适配。

Windows 可以将 `frontend/pom.xml` 作为 Maven 工程导入 IDE，或在 `console/frontend/`
执行 `mvn package`，再使用上游 `openpnp.bat` 启动；上游启动器使用默认用户配置目录。
来源及详细边界见 [UPSTREAM.md](UPSTREAM.md)。

新用户默认使用简体中文；已保存的语言偏好优先。可在语言菜单中切换，重启后生效。
菜单、配置向导、视觉流程参数、诊断提示及运行状态采用贴片机行业术语。
代码类型名、G 代码、协议字段、文件格式字段和用户自定义设备名称保留原值。
术语约定与维护方法见 [LOCALIZATION.md](LOCALIZATION.md)。

导入验证（2026-09-14）：Linux x86_64、JDK 17.0.20.1、Maven 3.9.9，
`package` 构建成功，253 项上游测试全部通过。已在虚拟显示器中通过本项目启动脚本
打开欢迎窗口和主界面，使用默认模拟配置；未连接真实机器。
构建日志与界面截图保存在已忽略的 `artifacts/openpnp-import/`。
迁移至 `frontend/` 后源码校验值保持一致，已重新编译打包并验证启动脚本的版本输出；
此次仅调整目录，未重复执行上述 253 项测试。

## 浏览器开发模式（运行在自己的控制端）

这一节介绍笔记本开发模式；上位机部署使用独立的 `smt-console.service`。
OpenPnP 是 Swing 应用，没有原生 Web 开发服务器。此模式使用独立 Xvfb 显示器、
x11vnc、websockify 和 noVNC，让浏览器远程显示并操作现有界面；不会共享当前桌面。
它是开发显示入口，尚未实现前端与 Go 后端的业务 API。

Debian/Ubuntu 控制端首次准备（已有 Java/Maven 和可运行的前端构建）：

```bash
./console/setup-dev-frontend.sh
./console/dev-frontend.sh
```

依赖从本机已配置的 APT 源下载，仅解包到 `toolchains/console-dev/`，无需 sudo，
不会把程序装到开发板。依赖记录保存在 `artifacts/console-dev-setup/`。
也支持系统已安装的 Xvfb、x11vnc、noVNC 和 Python websockify；开发模式脚本目前面向 Linux。

默认监听 **`0.0.0.0:6080`**，浏览器打开：

```text
http://127.0.0.1:6080/vnc.html?autoconnect=true&resize=scale
http://<控制端局域网IP>:6080/vnc.html?autoconnect=true&resize=scale
```

本次控制端地址是 `192.168.2.15`，不是开发板的 `192.168.2.7`。
首次自动生成访问密码，查看 `local/console-dev/password` 后在 noVNC 登录框输入。
密码文件权限为 0600；原始 VNC 端口仅监听回环地址，HTTP/WebSocket 入口用于可信局域网开发，
不提供 TLS。多个浏览器共享同一个应用会话和操作状态。
noVNC 1.6 在局域网 HTTP 地址下会提示非 HTTPS；需要浏览器安全上下文的功能应通过受信任的 HTTPS 代理访问。

`Ctrl+C` 停止应用、虚拟显示器和代理；关闭浏览器只断开该客户端。
也可以从仓库根目录执行 `kill "$(cat local/console-dev/session.pid)"` 停止当前开发实例。
开发配置使用 `local/console-dev/config/`，日志放 `local/console-dev/logs/`。
更改 Java 源码后重新运行 `build-frontend.sh`，再重启开发模式；不支持浏览器热更新。

可选参数：`--host 127.0.0.1` 限定本机、`--port 6081` 更换端口、`--display 99` 更换虚拟显示器编号。
同一工作区只允许一个开发实例；已有本地显示器不会被接管。

组件文档：[noVNC](https://github.com/novnc/noVNC)、[websockify](https://github.com/novnc/websockify)、
[x11vnc](https://github.com/LibVNC/x11vnc)。这些外部工具保留各自许可证，未复制进业务源码。

本次开发入口验证：局域网地址的 HTML/JS 返回 200，WebSocket/RFB 握手及密码认证通过，
错误密码被拒绝；停止释放子进程和显示器，端口占用与重复实例会被拒绝，重启后 HTTP 恢复。
当前没有可用浏览器自动化环境，未验证 noVNC 页面的实际交互。
记录保存在 `artifacts/console-dev-setup/`。

## 与板端的职责约定

计划中的调用关系：`frontend → backend（Go，笔记本）→ controller（Python，开发板）→ MCU`。
前端负责界面、用户输入与画面展示；Go 后端负责文件解析、工程存储、任务准备、
设备会话和板端协议适配；板端负责真实视觉定位、标定变换与任务执行；MCU 负责实时运动和 IO。
本地 Go API 尚未定义，不能把当前目录划分视为已完成前后端通信。
Go 后端的详细边界见 [backend/README.md](backend/README.md)。

操作请求经公开协议进入板端应用服务，不直接绕过机器协调器访问下位机。
板端负责实际机器状态与执行进度，笔记本重连不能导致重复贴装。
任务格式与网络协议统一维护在 protocols/。

## 已提供的连接能力

安装 `controller[network]` 后，用 `smt-controller --discover` 扫描，
再用 `smt-controller --connect-id <设备UUID>` 连接。设备 IP 变化后重新扫描即可，
设备 UUID 保持不变。CLI 保持心跳并轮询状态，`--once` 可查询后退出。

Python 桌面程序可以复用 `smt_controller.connectivity.discovery.discover` 和
`smt_controller.connectivity.client.ControllerClient`：

```python
import asyncio
from smt_controller.connectivity.discovery import discover
from smt_controller.connectivity.client import ControllerClient

async def main():
    devices = await discover(timeout=5)
    for device in devices:
        print(device.device_id, device.name, device.addresses, device.port)
    # UI 让用户选设备；连接时传 expected_device_id，并尝试它的可达地址。
    # client = await ControllerClient.connect(address, device.port,
    #                                         expected_device_id=device.device_id)
    # async with client:
    #     print(await client.request("get_status"))
    # 长连接期间每 5 秒调用 client.request("ping")；断线后重新发现并握手。

asyncio.run(main())
```

当前开放发现、握手、心跳、只读状态；完整协议见
[局域网连接 v1](../protocols/lan-connection-v1.md)。

## OpenCV 视频预览

板端端口 8766 提供灰度、二值化和轮廓叠加三种 MJPEG 流。
通过设备发现取得可达 IP 后，桌面程序可以使用 `smt_controller.preview.client.iter_preview` 接收 JPEG。
示例无需 OpenCV 或 GUI：

```bash
python examples/receive_preview.py --host 192.168.2.7 --mode contours --frames 30
```

以上命令在 `console/` 目录执行，需先安装基础 `controller` 包。
接口、断线处理及嵌入桌面程序的方法见 [预览协议](../protocols/preview-http-v1.md)。
