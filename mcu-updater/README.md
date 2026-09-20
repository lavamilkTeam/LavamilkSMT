# 下位机固件更新服务

运行在 Linux 上位机的独立 Go 服务，按职能划分。面板不直接访问烧录器：

```text
Java Swing → Python controller → Go mcu-updater → OpenOCD → USB ST-LINK → STM32
```

## 当前实现与边界

- 已实现：签名发布清单查询、HTTPS 下载、板型/协议/Flash 区域校验、SHA-256 校验、OpenOCD 子进程适配、持久化异步任务、Unix socket API。
- 已接通：Swing「机器 → 下位机固件更新…」经 Python 检查更新条件；Python 也可查询最近任务。
- **当前面板只检查，不发起烧录。Python 尚无真实下位机停机、输出安全确认和运行版本读取适配，启动更新一律返回 `MAINTENANCE_UNAVAILABLE`，不会签发维护许可。**
- 下位机板级工程与正式签名发布源尚未建立，没有可直接烧录的本项目固件。
- OpenOCD 已通过替身进程测试；未连接 ST-LINK 或实机烧录，不能视为硬件验收。
- 本服务不更新上位机系统；该职责属于 [host-updater](../host-updater/README.md)。

## 代码边界

```text
mcu-updater/
├── go.mod                         # 独立模块，仅使用 Go 标准库
├── cmd/smt-mcu-updater/main.go     # 配置、进程互斥、资源生命周期
└── internal/
    ├── api/                       # 本机 JSON Lines 协议，限制长度/并发/超时
    ├── update/                    # 更新事务、任务状态和恢复；编排外部能力
    ├── release/                   # Ed25519 清单验签、兼容性检查、镜像下载
    ├── flash/                     # OpenOCD/ST-LINK 适配，不判断机器是否安全
    ├── maintenance/               # 消费 controller 的许可、占用共享更新锁
    └── storage/                   # 状态文件原子替换与落盘
```

依赖方向：`api → update → 能力接口`。入口注入 release、flash、maintenance 实现；
业务流程不依赖 socket、HTTP handler 或界面。当前只有 MCU 升级任务，未建立通用插件框架。

## 本地运行

需要 Go 1.26+。在仓库根目录执行：

```bash
cd mcu-updater
go test -race ./...
go build -trimpath -o ../artifacts/smt-mcu-updater ./cmd/smt-mcu-updater
cd ..
./artifacts/smt-mcu-updater \
  --state-dir local/mcu-updater \
  --socket local/mcu-updater/service.sock
```

另一个终端启动已安装网络依赖的 Python controller：

```bash
PYTHONPATH=controller/src python3 -m smt_controller --serve \
  --mcu-updater-socket "$PWD/local/mcu-updater/service.sock"
```

构建并启动 Swing 后，点击「机器 → 下位机固件更新…」。默认连接同机 `127.0.0.1:8765`；
开发时可通过 Java 系统属性 `smt.controller.host` / `smt.controller.port` 调整 Python 地址。
请求在 SwingWorker 中执行，不阻塞界面线程；结果明确显示阻止升级的原因。

无 `--config` 时服务正常提供检查/状态接口，所有硬件更新保持未配置。
配置模板见 [mcu-updater.json](../config/examples/mcu-updater.json)，模板不能直接用于烧录。
Go 与 Python 的 socket 权限为 0600，部署时两者使用同一专用用户 `smt-controller`。
服务模板见 [smt-mcu-updater.service](../deploy/smt-mcu-updater.service)，默认同样不启用物理烧录。
异常退出可能留下本地开发 socket；确认没有存活进程后才能移除该 socket，再重启。不要删除任务或维护记录来绕过恢复。

## 固件发布与安装约束

常规更新下载 CI 编译后的 BIN，不在板端拉分支头编译。发布清单由受信任发布者签名：

```json
{"payload":"原始 manifest JSON 字节的 Base64","signature":"Ed25519 签名的 Base64"}
```

公钥来自本机管理员配置，不能来自下载内容。payload 字段为：

- `version`、`board`、`mcu`（当前为 `STM32F407ZGT6`）、`protocol`。
- `address`（十进制烧录地址）、`size`（BIN 字节数）、`sha256`（小写十六进制）。
- `url`（与配置的清单地址同源的 HTTPS BIN 地址）。

清单最大 64 KiB，镜像不超过配置的 Flash 区域；禁止重定向和 URL 内嵌凭据。
配置必须明确板型、协议、ST-LINK 序列号、Flash 区域和发布公钥；Flash 区域的起止必须按 F407 扇区对齐，
避免擦除跨入保留区。镜像区域内的参数需要在板级设计时单独保留。下载校验通过后才生成 `image.bin`。
当前 CI 只构建固件，未自动签名或发布上述清单；签名私钥不放在设备或源码仓库。

## 维护与恢复

协议细节见 [MCU 更新 v1](../protocols/mcu-update-v1.md)。更新任务使用固定版本、摘要和 task_id；
检查之后发布源发生变化时拒绝启动，重复提交同一当前任务只返回其状态。

```text
许可校验 → downloading → flashing → awaiting_confirmation → succeeded
                         ↘ failed / interrupted（保持维护锁）
```

Go 只消费 Python 写入的许可，不自行推断停机。许可绑定任务、板型、版本和镜像摘要，有短期有效期；
创建共享持久化更新锁后，直到真实运行版本确认前都不会释放。许可到期不会自动解除维护。
未来 host-updater 必须使用同一个锁，不能各自实现独立互斥。

下载或烧录失败、进程中断会保留维护锁；重启不会重新烧录。成功写入 Flash 只进入等待确认，
需 Python 从真实 MCU 读取运行版本并核对协议/设备状态后调用确认接口。Python 当前不会发出该确认。
没有实现自动回滚或通用的清锁 API；失败恢复需要核对实机，不能用“删除锁并重试”代替恢复流程。

浏览器断线、查询超时不会取消已接受的烧录任务。SIGTERM 停止接收新请求并等待任务完成，
整个任务上限 5 分钟，OpenOCD 上限 2 分钟；强制断电仍可能造成固件不完整。
下载、任务记录和有界 OpenOCD 日志存放在 state-dir；开发文件已由根目录 local/、artifacts/ 忽略。
历史任务保留供审计与人工恢复，目前没有自动清理或自动回退。

## 验证

单元测试覆盖许可缺失/过期、重复更新、下载失败、签名与硬件不匹配、重启中断及运行版本确认。
在构建 Go 二进制和 Java 测试类后，可从仓库根目录运行：

```bash
python3 tools/check_mcu_update_link.py
```

该检查临时启动真实 Go 和 Python 进程，再调用 Swing 使用的 Java 客户端，验证三段链路与拒绝烧录行为；不会连接真实相机、MCU 或 ST-LINK。

[MCU updater CI](../.github/workflows/mcu-updater-ci.yml) 执行 Go race/vet、Python 维护边界测试、Java 更新客户端与本土化测试、三进程联动检查，并构建 Linux amd64/ARMv7/ARM64 二进制。CI 不访问真实设备，也不部署或烧录。
