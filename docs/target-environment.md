# 目标板开发环境

固定控制地址：`192.168.1.100/24`；本次维护使用的 DHCP 地址：`192.168.2.7`。
检查与准备日期：2026-09-13。
登录凭据不保存在仓库中。

## 控制笔记本连接

板端 `/etc/netplan/10-dhcp-all-interfaces.yaml` 已持久配置 `eth0` 的
`192.168.1.100/24` 静态地址，同时保留 DHCP。现场检查确认静态地址已生效，
由 systemd-networkd 管理；`192.168.2.7` 是 DHCP 分配，不能视为固定地址。

控制笔记本网线直连时，将有线网卡设置为 `192.168.1.101`，子网掩码
`255.255.255.0`，此直连接口的网关与 DNS 留空。随后运行：

```bash
ping 192.168.1.100
ssh root@192.168.1.100
```

笔记本侧尚未代为配置或完成直连验证。若接入现有局域网，需先确认该静态地址没有冲突，
且笔记本具有同网段地址；若其他接口已使用 `192.168.1.0/24`，需规划不同的直连网段。
板端已提供 mDNS 发现与 TCP 8765 连接服务，支持握手、心跳、只读状态查询。
真实运动接口尚未提供。另有 HTTP 8766 视频预览，见下文；客户端可按设备 ID 发现，不必固定记住 DHCP 地址。

## 实测平台

- 设备树 compatible：`hi3798mv310-series`，四核 Cortex-A53，最高 1.6GHz。
- 系统：Ubuntu 20.04.6，armhf / armv7l，glibc 2.31。
- 内核：厂商 Linux 4.4.35；约 2GB 内存、8GB eMMC。
- USB 2.0，存在 uvcvideo 驱动；准备时没有连接摄像头。
- 系统 `/usr/bin/python3` 为 3.8.10，不替换它。

## 安装布局

```text
/opt/smt/
├── python/       # 独立 CPython 3.11.16 ARMv7 hard-float
├── venv/         # 项目 Python 虚拟环境
├── opencv/       # 原生构建的 OpenCV 库
├── controller/   # 当前源码快照和测试
├── wheels/       # 本机生成的 NumPy wheel
├── build/        # 本机构建目录，可在不再需要增量构建时清理
└── setup/        # 源码包、构建脚本、日志与环境验证结果
```

开机启动只读网络连接服务和独立相机预览服务，不初始化运动系统或自动回零。
修改工作区源码后需显式同步目标目录；目标目录不是共享挂载。

## 局域网服务部署

- systemd 单元：`smt-controller.service`，已启用开机启动，进程使用 `smt-controller` 系统用户。
- 监听：TCP 8765；通过 eth0 的 IPv4 mDNS（UDP 5353）发布 `_smt-controller._tcp.local.`。
- 名称：`SMT-Controller`。
- 设备 ID：`6958aac7-32e6-4158-9254-5cffe405cc5a`，保存在 `/var/lib/smt-controller/device-id`。
- 网络依赖：zeroconf 0.151.3、ifaddr 0.2.0。ARMv7 板使用 zeroconf 纯 Python 实现，
  通过 `SKIP_CYTHON=1` 构建，构建辅助依赖 poetry-core 2.4.1。

控制笔记本在仓库根目录安装客户端：

```bash
python -m pip install -e './controller[network]'
smt-controller --discover
smt-controller --connect-id 6958aac7-32e6-4158-9254-5cffe405cc5a
```

查看与管理板端服务：

```bash
systemctl status smt-controller
journalctl -u smt-controller -n 50 --no-pager
systemctl restart smt-controller
```

服务模板见 [deploy/smt-controller.service](../deploy/smt-controller.service)，
板端安装步骤见 [tools/install_target_network.sh](../tools/install_target_network.sh)。
脚本假设已有 `/opt/smt/venv`，且 `setup` 目录中准备了项目快照、zeroconf 源码、
ifaddr/poetry-core wheel、服务文件和 SHA-256 清单；它不是空系统安装器。
该次准备记录保存在工作区 `artifacts/network-setup/` 和板端 `/opt/smt/setup/install-network.log`。
后续更新源码不需要重新构建 NumPy/OpenCV。

本次网络验证：笔记本 `192.168.2.15` 成功通过 mDNS 发现开发板，并通过
`192.168.2.7:8765` 完成握手和状态查询；按设备 ID 自动选择可达地址的 CLI 也通过。
服务重启前后 ID 一致。两个客户端持续连接超过 25 秒，完成 6 轮心跳/查询，
其中一个断开后另一个仍可正常查询。本地与板端 19 项测试均通过。
未通过修改实际 IP、拔网线或重启整块板子来验证网络变化；这些路径仍需现场验证。
记录见工作区 `artifacts/network-setup/lan-verification.json` 与板端
`/opt/smt/setup/network-tests.log`。

## OpenCV 预览部署与实测（2026-09-14）

已连接 UVC `FD WDR Camera`（USB `1d6c:0103`），设备为 `/dev/video0`，USB 2.0 链路。
服务使用稳定路径 `/dev/v4l/by-id/usb-FD_WDR_Camera_FD_WDR_Camera_20211101001-video-index0`。
原生 OpenCV 的 V4L2 后端可以采集并解码相机 MJPG 输出。

独立 `smt-preview.service` 已启用开机启动，使用 `smt-controller` 用户及 `video` 附加组。
默认 640×480、处理输出上限 10fps、JPEG 质量 75，同时生成灰度、二值化和轮廓画面。
当前局域网内可直接打开：

- 灰度：`http://192.168.2.7:8766/stream/gray.mjpg`
- 二值化：`http://192.168.2.7:8766/stream/threshold.mjpg`
- 轮廓：`http://192.168.2.7:8766/stream/contours.mjpg`
- 状态：`http://192.168.2.7:8766/status.json`

直连时按前文配置笔记本网卡，并把 URL 地址换为 `192.168.1.100`。
服务配置和客户端说明见 [预览 HTTP v1](../protocols/preview-http-v1.md)。

此次真实网线验证从 `192.168.2.15` 同时接收三路，每路 40 帧，JPEG 全部可解码为
640×480，序号严格递增。实际每路 **5.36–5.37fps**，三路 JPEG 负载合计约
**758KB/s（6.1Mbps）**。一次状态采样中，解码约 36ms、处理约 37ms、三图编码约 114ms；
10fps 是设置上限，当前板端没有达到。此场景主要耗时在 JPEG 编码，画面内容变化会影响耗时和带宽。
预览进程观察到约 40MB RSS，CPU 约占一个核心。未测两只相机同时工作。

一个视频客户端断开后另一个继续收到 10 帧，TCP 8765 仍能握手/查询且运动保持禁用。
预览服务重启后已重新取得相机，笔记本接收示例连续收到 10 帧；未重启整块开发板或拔插相机。
本地与板端 27 项测试均通过；三种图像已人工检查，尚未验证 PCB Mark 或元件定位精度。
记录在工作区 `artifacts/preview-setup/`，板端安装日志为 `/opt/smt/setup/preview/install-preview.log`。

管理命令：

```bash
systemctl status smt-preview
journalctl -u smt-preview -n 50 --no-pager
systemctl restart smt-preview
systemctl stop smt-preview  # 其他采集程序使用同一相机前先停止预览
```

重新部署时，把 `controller/` 打包为 `controller.tar.gz`（排除 `__pycache__` 和 `*.egg-info`），
连同 [服务模板](../deploy/smt-preview.service)、[安装脚本](../tools/install_target_preview.sh)
放进 `/opt/smt/setup/preview/`。对这三个文件生成 `preview-source.sha256`，随后执行：

```bash
bash /opt/smt/setup/preview/install_target_preview.sh
```

脚本要求已有独立 Python、NumPy/OpenCV、项目构建依赖和 `smt-controller` 用户；
它备份项目目录，安装源码，执行检查后启动预览，不重新构建视觉库。
实际相机型号不同需先修改服务的 `--device`。服务运行时不能再手动启动第二个相机采集器。

## 软件来源与版本

- Python：Astral python-build-standalone 的 `20260901` 发行版，
  `cpython-3.11.16+20260901-armv7-unknown-linux-gnueabihf-install_only_stripped.tar.gz`。
  SHA-256：`98d7f8cae7ac4deb355420c09334f483cfdfc9c6639d30b8afc526176b94e383`，
  与 GitHub release asset digest 核对后安装。
- NumPy 1.26.4：PyPI 源码包，按发布元数据校验，在板端生成 wheel。
- OpenCV 4.13.0：官方仓库 tag 源码，在板端构建。
- pyserial 3.5：Python 串口通信库，尚未接入下位机协议。
- 构建工具：pip 25.3、setuptools 79.0.1、wheel 0.45.1、packaging 25.0、
  Meson 1.2.3、meson-python 0.15.0、pyproject-metadata 0.9.1、Cython 3.0.12。

来源：
[Python 构建发行版](https://github.com/astral-sh/python-build-standalone/releases/tag/20260901)、
[NumPy 源码](https://pypi.org/project/numpy/1.26.4/)、
[OpenCV 源码](https://github.com/opencv/opencv/tree/4.13.0)。

## 构建约定

系统依赖从板上已有 Ubuntu 软件源安装，不添加跨发行版软件源：

```bash
apt-get install --no-install-recommends \
  build-essential cmake ninja-build pkg-config \
  libjpeg-dev libpng-dev libopenblas-dev libv4l-dev v4l-utils
```

Python 安装在独立前缀，再使用其 venv。NumPy/OpenCV 构建细节见
[构建脚本](../tools/build_target_vision.sh)；脚本要求独立 Python、构建依赖及源码包已准备好，
不是从空系统开始的全自动安装器。源码包的校验值已写入脚本。

在空的目标安装目录中，将校验后的 Python 包保存为 `/opt/smt/setup/python.tar.gz`，
NumPy/OpenCV 源码以脚本中的文件名放在同一目录，然后执行：

```bash
tar -xzf /opt/smt/setup/python.tar.gz -C /opt/smt
/opt/smt/python/bin/python3.11 -m venv /opt/smt/venv
CYTHON_COMPILE_IN_CYTHON=0 /opt/smt/venv/bin/python -m pip install \
  pip==25.3 setuptools==79.0.1 wheel==0.45.1 packaging==25.0 \
  meson==1.2.3 meson-python==0.15.0 pyproject-metadata==0.9.1 \
  Cython==3.0.12 pyserial==3.5
bash /opt/smt/setup/build_target_vision.sh
/opt/smt/venv/bin/python -m pip install --no-build-isolation --no-deps -e /opt/smt/controller
```

这些命令假设已复制项目源码和构建脚本；不要直接覆盖已有的 Python 安装目录。
NumPy 使用两个、OpenCV 使用三个并行构建任务。构建临时文件位于磁盘上的
`/opt/smt/build/tmp`，避免占满内存型 `/tmp`。

本次 OpenCV 编译使用临时 distcc 助手加速：笔记本上的 ARM hard-float GCC 9.4
与板端 GCC 主次版本一致，板端负责预处理、链接和最终运行验证。
工具来自 Ubuntu/Debian 软件包，下载与校验记录保存在工作区
`artifacts/target-setup/cross-tools/`。该助手通过 SSH 转发连接，只在编译期间启用。
普通重建不需要它；构建脚本默认在板端编译。可选环境变量
`SMT_COMPILER_LAUNCHER` 和 `SMT_BUILD_JOBS` 用于显式指定编译助手与并行数。

OpenCV 选择 core、imgproc、imgcodecs、videoio、calib3d、python3 及其必要依赖；
启用 NEON、V4L2、JPEG/PNG；关闭 GUI、OpenCL、FFmpeg、GStreamer 和测试程序。
USB 摄像头通过 V4L2 接入。此构建不提供通用 FFmpeg 视频文件解码。
OpenCV 直接安装到虚拟环境及独立库目录，版本以 `cv2.__version__` 与构建信息为准，
不能仅靠 `pip freeze` 还原它。

## 验证方式

在目标板运行：

```bash
/opt/smt/venv/bin/python --version
/opt/smt/venv/bin/smt-controller --demo
/opt/smt/venv/bin/python -m unittest discover -s /opt/smt/controller/tests -v
/opt/smt/venv/bin/python /opt/smt/setup/verify_target_environment.py
v4l2-ctl --list-devices
```

环境检查脚本验证 NumPy、OpenCV、pyserial、项目包、V4L2 后端、PNG 编解码和二维变换；
对 1280×720 合成图像做简单轮廓处理计时。该计时不包括真实采集、曝光、运动停稳或复杂匹配，
不代表贴装节拍或实际定位精度。

摄像头接入后仍需验证设备身份绑定、两路 USB 带宽、曝光设置和有效新帧时序。

## 本次验证结果

- Python 3.11.16、NumPy 1.26.4、OpenCV 4.13.0、pyserial 3.5 均可正常导入。
- `smt-controller 0.1.0` 已以 editable 模式安装，演示程序完成，7 项单元测试全部通过。
- `pip check` 通过；OpenCV 的 V4L2 后端可用，PNG 往返编解码及二维变换检查通过。
- 1280×720 单通道合成图像，OpenCV 两线程，预热 3 次后测量 30 次：
  高斯模糊、阈值分割、轮廓提取及最小包围圆计算合计中位数 **26.162ms**，
  最大值 **28.571ms**。这是环境冒烟检查，不是实际贴片性能验收。
- 未检测到 `/dev/video*`，尚未验证真实 USB 摄像头或下位机运动通信。
- 系统 Python 仍为 3.8.10，CPU 调速策略恢复为原来的 `interactive`。

完整记录位于板端 `/opt/smt/setup/verification.json`、`controller-tests.log`、
`opencv-build-information.txt`、`python-packages.txt` 和 `system-packages.txt`。
工作区 `artifacts/target-setup/` 保存这些记录及本次生成的 NumPy wheel、OpenCV 运行库备份。
OpenCV 备份用于同类 ABI 环境，目录前缀为 `/opt/smt`，不属于通用 pip wheel。
