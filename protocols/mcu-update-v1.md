# MCU 更新协议 v1

调用链：Swing → Python controller → Go mcu-updater。所有组件运行在上位机；浏览器只是显示终端。

## 当前可用能力

Python 的现有 TCP 8765 JSON Lines 协议增加下列消息，仍要求先完成 `hello` 握手：

| type | 行为 |
| --- | --- |
| `check_mcu_update` | 经 Go 检查配置及签名发布清单，返回更新条件 |
| `get_mcu_update_status` | 经 Go 查询最近的持久化任务 |
| `start_mcu_update` | 当前固定拒绝，返回 `MAINTENANCE_UNAVAILABLE`；不会向 Go 提交烧录或创建许可 |

响应为 `{"type":"mcu_update","request_id":"原请求 ID","update":{...}}`。
`update` 包含 `state`、`can_update`、`code`、`blockers`；Go 可用时检查结果还包含 `task`，
成功获得发布清单时包含 `release`。`task` 为空表示没有历史任务，不能据此推断 MCU 已连接或安全。
Go 不可达或响应无效时返回 `UPDATER_UNAVAILABLE` / `UPDATER_INVALID_RESPONSE`，普通状态查询仍可使用。
当前 Python 对所有结果强制 `can_update=false`，不从 Go 的配置就绪推导机器可升级。

这仍是只读能力扩展。当前 TCP 未认证，不允许未来仅通过开放一个消息类型就启用真实烧录；
真实升级前必须补齐控制端授权、硬件维护适配、持久维护状态和重启恢复检查。
Swing 默认连接同机 Python，在后台工作线程执行检查，未添加可绕过维护准入的烧录按钮。

## Python 与 Go 的本机接口

Unix socket，默认部署地址 `/run/smt-mcu-updater/service.sock`，权限 0600，仅同一服务用户可访问。
每个连接一条请求、一条响应，随后关闭；UTF-8 JSON Lines，最大请求 8192 字节，最多 8 个连接。
socket 权限是本机信任边界；不要把它转发到未认证的网络接口。

请求示例：

```json
{"protocol_version":1,"request_id":"a1","type":"check"}
```

响应包含相同的 `request_id` 和 `protocol_version`，以及 `result` 或 `error`：

```json
{"protocol_version":1,"request_id":"a1","result":{"can_update":false,"blockers":["controller maintenance authorization required"]}}
```

| type | 参数与返回 |
| --- | --- |
| `check` | 返回发布清单、最近任务、阻塞原因；不会给出机器维护许可 |
| `status` | 返回 `{"task":...}`，无任务时为 null |
| `start` | `task_id`（32 位小写十六进制）、`version`、`sha256`；消费维护许可后创建异步任务 |
| `confirm` | `task_id`、`version`（从真实 MCU 读取）；匹配等待确认的任务后结束更新事务 |

错误码：`INVALID_MESSAGE`、`PROTOCOL_MISMATCH`、`UNSUPPORTED_REQUEST`、`UPDATE_REJECTED`。
request_id 用于单次请求关联，task_id 用于更新事务与重复提交识别；两者不能混为一谈。
任务 ID 对应已保留的历史目录时不再次执行烧录，当前任务重复提交返回原状态。
请求连接断开不取消已接受的任务；超时后先查询状态，不自动换一个 task_id 重试。

## 维护许可契约（Go 已实现消费，Python 尚未签发）

真实 controller 必须先停止任务、确认输出安全、暂停下位机日常通信并持久记录维护状态，
然后由其本机适配器原子写入权限 0600 的许可文件。路径由管理员配置，不能由界面传入。

许可字段：`task_id`、`board`、`version`、`sha256`、`outputs_safe=true`、`expires_at`（RFC3339，未来最多 15 分钟）。
Go 校验许可与签名清单完全匹配，然后以 O_EXCL 创建共享更新锁，内容为 `{"target":"mcu","task_id":"..."}`。
锁落盘后删除并落盘消费许可文件，避免旧许可在设备恢复运行后被新进程重放。
锁写入或任务持久化失败时保持阻塞。Go 不从 API 接受 `outputs_safe` 代替许可。
同一专用账户与受限目录构成本机权限边界，许可文件不是对不受信任本机进程的密码学认证。

controller 重启需先读取其维护记录和共享锁；只有真实 MCU 运行版本、协议和状态核对后才调用 `confirm`。
共享锁解除不等于允许自动运动：controller 仍需明确恢复设备状态和回零，任务不得自动续跑。
失败或 interrupted 状态没有自动解除接口；由后续板级恢复流程处理。

## 发布与兼容性

发布格式、签名、公钥与 Flash 区域配置见 [mcu-updater](../mcu-updater/README.md)。
当前仅支持 STM32F407ZGT6 的 BIN 镜像，服务不接受面板提供的文件路径、下载地址、烧录地址或 OpenOCD 命令。
代码协议、线程测试与交叉编译均不能替代实际 ST-LINK、Flash 保留区和运动保护验收。
