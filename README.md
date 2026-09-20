# SMT 桌面贴片机

软件、通信协议和机械设计放在同一个 Git 仓库中，共同维护版本。当前包含纯 Python 无界面控制骨架、模拟定位流程、局域网发现与状态连接，以及真实 USB 相机的 OpenCV 预览；真实运动控制和定位尚未接入。

## 项目结构

```text
SMT/
├── console/              # 上位机控制面板：界面与 Go 后端预留
├── controller/           # Linux 板：纯 Python 设备流程、视觉与通信
├── host-updater/         # Go 上位机自身更新服务，当前为职责约定
├── mcu-updater/          # Go 下位机更新服务与面板查询联动；真实维护接入前禁止烧录
├── firmware/
│   └── mainboard/        # STM32F407ZGT6 下位机固件，板级工程待建立
├── protocols/            # 局域网连接、下位机协议及版本约定
├── deploy/               # 板端 systemd 服务模板
├── hardware/
│   ├── mechanical/
│   │   ├── source/       # 可编辑的机械 CAD 源工程
│   │   ├── step/         # STEP/STP 三维交换文件
│   │   └── drawings/     # DWG/DXF 图纸及配套 PDF
│   └── electronics/      # 电路原理图、PCB、BOM
├── config/
│   └── examples/         # 可公开提交的配置示例
├── docs/                 # 架构、标定和部署文档
├── tests/
│   └── fixtures/         # 小型测试图片、协议报文等固定样例
└── tools/                # 构建、部署和开发辅助脚本
```

## 运行方式

- `console` 是上位机控制面板；文件处理、工程管理和任务准备归面板后端，Go 后端尚未实现。笔记本、手机是访问终端，也可作为开发环境。
- 板端使用适配该板的 Linux；`controller` 是独立的 Python 无界面控制与视觉服务。
- 当前中文版 OpenPnP 在上位机运行，通过浏览器访问；机器运动仍为模拟，见 [部署说明](docs/console-deployment.md)。
- [host-updater](host-updater/README.md) 预留上位机自身更新；[mcu-updater](mcu-updater/README.md) 已实现独立 Go 服务及 Swing → Python → Go 更新检查。真实维护适配尚未接入，面板暂不允许烧录。
- 下位机固件独立编译和烧录，负责实时运动执行和硬件保护。
- PCB 由夹具固定；随头下视相机找 Mark，工作台上视相机测元件偏移，详见 [双相机方案](docs/vision-plan.md)。
- STEP/DWG 等机械文件由 CAD 软件打开，不参与软件构建。

同一个仓库不代表同一个进程、编译器或部署包。各部分保留独立的依赖和构建入口，在协议层约定协作方式。

板端控制服务采用 Python + asyncio，OpenCV 已准备在目标环境中，真实视觉适配器待接入。
局域网发现与 TCP 连接已实现，使用方法见 [连接协议](protocols/lan-connection-v1.md)。
独立视频预览通过 HTTP 8766 传输灰度、二值化和轮廓图，见 [预览协议与客户端](protocols/preview-http-v1.md)。
当前运行模拟演示无需第三方依赖：

```bash
PYTHONPATH=controller/src python3 -m smt_controller --demo
PYTHONPATH=controller/src python3 -m unittest discover -s controller/tests -v
```

演示仅使用模拟运动、合成图像和固定定位结果，不执行真实识别或贴装。目录与开发说明见 [controller/README.md](controller/README.md)。

## 文件管理

- 可编辑 CAD 源工程放 `hardware/mechanical/source/`，交换模型放 `step/`，工程图放 `drawings/`。
- 图纸和软件之间存在依赖时，在同一次提交中更新相关文件与说明；发布时记录对应的硬件版本。
- 使用稳定文件名，例如 `head-assembly.step`、`camera-bracket.dwg`；历史修订交给 Git 管理。
- 板厂 SDK、系统镜像、编译产物、完整录像和日常采集图片不直接提交。SDK 下载地址、版本及校验值记录在文档中。
- 本机 SDK 放根目录 `sdk/`，工具链放 `toolchains/`，系统镜像放 `system-images/`，发布构建产物放 `artifacts/`；这些目录均已忽略。不要把它们混放在源码目录中。
- 设备实际标定和运行数据放根目录 `local/`，该目录已忽略；需要另行备份。可复现的配置样例放 `config/examples/`。
- 本机密钥或凭据放 `secrets/` 或本机 `.env`，均不提交；`.env.example` 只放占位值。正式图纸、依赖锁文件、共享编辑器配置和必要的预编译库应提交。
- 当前未启用 Git LFS。大型 CAD 文件正式导入前，建议先安装 Git LFS、确认远程仓库支持及容量，再提交对应跟踪规则。不要等历史中积累大量图纸后再迁移。

## 下一步

1. 确认控制板型号、Linux 镜像和 USB 相机接口能力。
2. 确认下位机芯片、通信接口和已有固件。
3. 在 `protocols/` 定义最小控制协议，再接入真实设备适配器。
4. 跑通实际采集、定位、标定和单次取放，再增加网络任务入口。

架构约定见 [docs/architecture.md](docs/architecture.md)。

目标板开发环境与运行命令见 [docs/target-environment.md](docs/target-environment.md)。
