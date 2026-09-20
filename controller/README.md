# 无界面控制服务

纯 Python 3.11+，使用 asyncio 组织后台控制流程，不依赖 PySide6 或网页框架。
按功能组织业务，通过 Protocol 接口使用外部能力，由 app 组装具体适配器。

## 当前实现

可运行的**模拟骨架**：初始化模拟设备、模拟回零、移动到给定坐标、获取合成图像、返回固定模拟定位结果。
`--demo` 演示完成后退出，不启动网络服务、不访问真实硬件，不包含真实识别或完整贴装。
相机帧和识别结果均显式标记 simulated。

另有独立 `--serve` 模式：mDNS 局域网发现、TCP 长连接、握手、心跳及只读状态查询。
它持久保存设备 ID，不初始化模拟或真实机器，状态明确为 `unconfigured/disconnected`。

独立 `smt-preview` 入口已接入真实 USB 相机，提供灰度、二值化和轮廓图的 HTTP MJPEG 预览，
默认端口 8766。用法与 Python 接收示例见 [预览协议](../protocols/preview-http-v1.md)。
预览不执行运动，也不输出正式 Mark/元件定位结论。

已组装两只模拟相机：`down_looking`（Mark/料位）、`up_looking`（吸取后的元件）。
任务按角色选择相机，结果同时核对相机角色与帧编号。默认演示只执行一次下视模拟定位。
第一版使用固定 PCB，不加入全局相机或 YOLO，详见 [视觉方案](../docs/vision-plan.md)。

## 目录

```text
controller/
├── pyproject.toml
├── src/smt_controller/
│   ├── app/           # 程序入口、依赖组装与资源清理
│   ├── api/           # 不绑定传输框架的业务接口
│   ├── connectivity/  # 设备 ID、mDNS、TCP 协议、服务端与客户端
│   ├── preview/       # 独立相机预览：采集、OpenCV 处理、最新帧缓存、HTTP 和客户端
│   ├── jobs/          # 任务模型与最小定位流程
│   ├── machine/       # 全机状态、控制权与运动会话
│   ├── vision/        # 识别结果与流水线扩展位置
│   ├── calibration/   # 标定边界，尚未实现变换
│   ├── ports/         # 相机、运动、视觉能力契约
│   ├── adapters/      # 当前为模拟适配器
│   ├── firmware_update/ # 本机 Go 升级服务客户端与维护准入边界
│   └── common/        # 单位明确的值类型与错误
└── tests/             # 流程、并发拒绝、失败与取消测试
```

## 本地运行

从仓库根目录，无需安装第三方运行依赖：

```bash
PYTHONPATH=controller/src python3 -m smt_controller --demo
PYTHONPATH=controller/src python3 -m unittest discover -s controller/tests -v
```

也可以在虚拟环境中安装包：

```bash
python3 -m venv .venv
.venv/bin/python -m pip install -e ./controller
.venv/bin/smt-controller --demo
```

不选择运行模式时只显示帮助。真实硬件组装入口尚未提供。
Python 环境在目标设备上安装，不复制笔记本虚拟环境到 ARM 板。

## 局域网发现和连接

网络功能的额外依赖通过可选依赖组安装，模拟演示仍可只用标准库：

```bash
.venv/bin/python -m pip install -e './controller[network]'
.venv/bin/smt-controller --serve --name SMT-Controller --interface eth0
```

在控制笔记本同样安装该依赖组，然后运行：

```bash
smt-controller --discover
smt-controller --connect-id <发现结果中的设备UUID>
```

客户端保持连接，每 5 秒查询状态并发送心跳，Ctrl+C 退出。
也可手动指定 IP，或查询一次后退出：

```bash
smt-controller --connect 192.168.2.7 --once
```

默认 TCP 端口为 8765；mDNS 使用 UDP 5353。`--interface` 可限定发现使用的网卡，
`--timeout` 设置发现等待时间，`--state-dir` 指定设备 ID 保存目录。
TCP 服务监听所有 IPv4 接口，`--interface` 限制的是发现公告与扫描接口。
普通浏览器不能直接使用此 TCP 协议。
协议报文和局限见 [局域网连接 v1](../protocols/lan-connection-v1.md)。

板端服务模板为 [smt-controller.service](../deploy/smt-controller.service)，安装方法见
[目标板环境](../docs/target-environment.md)。服务进程使用独立系统用户运行。

当前海思 ARMv7 目标板的独立环境、构建方式及验证命令见
[目标板环境](../docs/target-environment.md)。视觉库在板端原生构建，系统 Python 保持不变。

## 模块约束

- api 只调用业务入口，不读写串口，不持有相机。
- jobs 编排步骤，vision 只测量，calibration 负责坐标含义。
- machine 在一个控制事件循环中拥有机器状态；忙碌期间新操作直接拒绝。
- operation 内的步骤顺序 await，不得把同一个运动会话共享给并行任务。
- 当前故障保持锁定；取消等待不等于下位机已经停止。
- 控制事件循环不执行阻塞采集和重计算；真实适配器使用采集线程和有界视觉 worker。
- app 是组合根，只有这里选择真实或模拟实现。业务模块不得导入具体 adapters。

## 后续接入顺序

1. 在 protocols 确认 console 面板提交的任务格式与下位机协议。
2. 实现 USB Camera 适配器，验证缓冲与停稳后有效帧。
3. 实现 OpenCV 流水线及 worker，在目标硬件测耗时与定位误差。
4. 加入标定、单位与工作范围检查，再实现真实运动适配器。
5. 扩展取料/贴装状态机、IO/飞达接口、停止与故障恢复、任务持久化。
6. 在现有连接层上接入网络任务生命周期、控制权、去重与重连恢复。

真实启动时只连接并核对状态，回零由明确操作触发。当前自动回零仅属于模拟演示。
网络客户端断开不能直接取消物理任务；关闭连接不代表停机。

## 下位机升级联动

`--serve` 模式支持 `check_mcu_update` 和 `get_mcu_update_status`，通过
`--mcu-updater-socket` 指定的本机 Unix socket 调用 Go 服务。默认路径为
`/run/smt-mcu-updater/service.sock`，见 [MCU 更新协议](../protocols/mcu-update-v1.md)。

`firmware_update` 是升级应用边界，`connectivity` 只分派请求，不直接写许可或调用 OpenOCD。
当前 `start_mcu_update` 固定拒绝：真实下位机的停机、输出安全确认和运行版本读取尚未接入。
断开或模拟空闲状态都不能作为维护许可；不会根据客户端传入的 `outputs_safe` 等字段放行。
