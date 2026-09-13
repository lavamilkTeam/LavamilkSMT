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
真实运动接口与网页尚未提供。客户端可按设备 ID 发现，不必固定记住 DHCP 地址。

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

开机启动只读网络连接服务，不初始化机器或自动回零。
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
