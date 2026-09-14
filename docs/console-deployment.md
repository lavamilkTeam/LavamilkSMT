# 上位机中文版 OpenPnP

当前控制界面部署在 `192.168.2.7` 上位机，浏览器访问：

```text
http://192.168.2.7:6080/vnc.html?autoconnect=true&resize=scale
```

按 [目标板网络设置](target-environment.md) 配置网线直连后，可将地址换为固定地址
`192.168.1.100`。`192.168.2.7` 是当前 DHCP 地址。手机或其他电脑需能访问同一局域网。
浏览器共享同一个 OpenPnP 会话；计算和配置保存均在板端，不依赖笔记本进程。
该入口使用 noVNC 远程显示 Java Swing，未将 OpenPnP 改写成网页应用。

## 服务与文件

- 服务：`smt-console.service`，独立 `smt-console` 用户，开机启动。
- 程序：`/opt/smt/console/releases/<部署包 SHA-256 前 16 位>/`。
- 当前版本：`/opt/smt/console/current` 符号链接。
- 用户数据：`/var/lib/smt-console/config/`；Java 偏好也保存在该用户主目录下。
- 连接密码：`/var/lib/smt-console/password`，仅服务用户可读写，不写入仓库。
- 安装记录与部署包：`/opt/smt/setup/console/`。
- 显示器：独立 Xvfb `:99`，1280×800；VNC 5901 仅回环，HTTP/WebSocket 6080 对局域网开放。

该版本 x11vnc 还会在 IPv6 回环 `::1:5900` 监听，不对外开放原始 VNC。

```bash
systemctl status smt-console
journalctl -u smt-console -n 80 --no-pager
systemctl restart smt-console
systemctl stop smt-console
```

密码文件为一行 8 位 ASCII 字符，更新后权限保持 0600、属主保持 `smt-console`。
VNC 在新连接时重新读取密码；已连接会话需断开后重连才会重新认证。
当前入口未配置 TLS，部署范围为本地局域网。

## 运行边界

OpenPnP 使用上游默认模拟机器配置，未接入真实运动或自动接管 USB 相机。
现有 `smt-controller.service`（8765）和 `smt-preview.service`（8766）独立运行。
OpenPnP 与本项目 Python 业务协议尚未适配，Go 后端也尚未实现。
本次部署不代表已能真实取料、贴装或执行相机标定。

Java 使用 `-Xms64m -Xmx512m -XX:ActiveProcessorCount=2`，服务 Nice 为 5。
板端配置中两只模拟相机的预览设为 5fps，避免默认高帧率模拟渲染持续占用处理器。
512MB 是 Java 堆上限，不包含原生 OpenCV、字体、显示服务和其他进程的内存。
真实双相机视觉、贴装吞吐和长时间运行仍需分别实测。
当前上游 capture JAR 没有 ARMv7 原生库，后续选择 `OpenPnpCaptureCamera` 前还需适配；
此限制不影响现有 Python 相机预览服务。

## 再部署

板端依赖使用原有 Ubuntu armhf 软件源安装，不复制笔记本 x86_64 的 Java 或显示库：

```bash
apt-get install --no-install-recommends \
  openjdk-17-jre xvfb x11vnc novnc python3-websockify fonts-noto-cjk
```

在笔记本运行 `console/build-frontend.sh` 构建并验证 Java 应用。
部署包根目录包含 `frontend/target/` 下的应用 JAR 与 `lib/`、`frontend/samples/`、
`frontend/VERSION.txt`、许可证及中英文欢迎说明，以及 `tools/target_frontend.py`。
将部署包命名为 `console.tar.gz`，与 [服务模板](../deploy/smt-console.service)、
[安装脚本](../tools/install_target_console.sh) 一同放入 `/opt/smt/setup/console/`。
首次安装还需在该目录准备权限 0600 的 `password` 文件。

```bash
bash /opt/smt/setup/console/install_target_console.sh <部署包的完整 SHA-256>
```

安装脚本校验部署包、使用独立版本目录并切换 `current`，不覆盖配置目录。
旧版路径保存在 `previous-release.txt`，保留的旧版本可用于手动回退。
更新后检查服务日志和浏览器认证，不能仅凭 `systemctl is-active` 判断 Java 初始化成功。

## 本次部署验证（2026-09-14）

- 部署包 SHA-256：`0e67e540c384985fe24705216f4ce1c96146b76e19b4e889f2f26fc1d32159f2`。
- 板端 Java 17.0.15（ARM）、OpenCV Java 4.5.5 原生库和高斯模糊操作可运行；中文资源和字体检查通过。
- HTTP 页面、WebSocket/RFB 握手、新密码认证和 1280×800 图像接收通过，错误密码被拒绝。
- 已查看板端实际输出的中文欢迎界面，版本显示为 `2.6_vendored.5bd404c`。
  首次进入点击“确定”即可继续；这块板启动界面约需一至两分钟。
- 服务停止、重新启动及启动后连接通过，已启用开机启动；未重启整块板子验收。
- 两只模拟相机 5fps、欢迎界面显示期间，15 秒采样中 Java CPU 约为单核的 31.7%，
  Java RSS 约 399MiB，界面服务各进程 RSS 合计约 483MiB（共享页可能重复计算）。
  这不是实际贴装或双 USB 相机负载测试。
- 原有 8765 握手和状态查询通过、运动保持禁用；8766 状态返回画面就绪。
- 最后采样约有 1.2GiB 可用内存、3.5GiB 剩余磁盘空间。

本机安装与验证记录保存于 `artifacts/console-target/`，未纳入 Git。
