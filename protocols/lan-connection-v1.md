# 局域网发现与连接协议 v1

用途：桌面程序或 Python 客户端发现 SMT 上位机、建立长连接并读取状态。
目前没有远程运动、回零、真空控制、文件上传或任务提交接口。

## 发现

使用 IPv4 mDNS/DNS-SD，服务类型为 `_smt-controller._tcp.local.`，UDP 5353。
客户端查询 PTR，再解析 SRV/TXT/A；不能只查某个预设主机名。

- 实例：`smt-<无连字符 UUID>._smt-controller._tcp.local.`。
- SRV：目标主机与实际 TCP 端口，默认端口 8765。
- A：板上当前的 IPv4 地址，可能有多个，客户端需尝试可达地址。
- TXT：`device_id`（带连字符 UUID）、`name`（UTF-8 名称）、`protocol=1`、`transport=tcp-jsonl`。

设备 ID 保存在服务端状态目录的 `device-id` 文件。重启、更改显示名称及 IP 不改变 ID；
复制系统到另一台机器时，应使用独立的状态目录，不能复制设备 ID。
设备 ID 用于识别设备，不是认证凭据。

服务每 3 秒检查指定网卡的地址变化并更新公告，TTL 为 30 秒；正常关闭时发送撤销公告。
启动时无地址则等待网络就绪。客户端使用 ID 去重，重新连接前应重新发现，不能长期缓存旧 IP。
断电时撤销公告无法发送，客户端仍须通过 TCP 连接/心跳判断在线状态。

mDNS 面向同一局域网广播域。AP 隔离、组播过滤、VLAN 或防火墙可能阻止发现；
此时可手动指定 IP 和端口连接。TCP 连接本身不依赖 mDNS。

## TCP 报文

UTF-8 JSON 对象，每帧以一个 LF（`\n`）结束。最大 8192 字节，包含 LF。
TCP 没有消息边界，接收端必须处理拆包和多个报文连续到达。
禁止 NaN/Infinity。客户端请求必须携带 1–64 字符的字符串 `request_id`。
响应携带相同 ID；当前连接内按顺序应答，不主动推送事件。

首次连接后 5 秒内发送握手：

```json
{"type":"hello","request_id":"1","protocol_version":1,"client_name":"my-console","expected_device_id":"设备 UUID"}
```

`client_name` 可省略，提供时为 1–128 字符。`expected_device_id` 可省略或为 null，
但从发现结果连接时应传入，并核对回复，避免连到另一台设备。

成功回复：

```json
{
  "type": "hello",
  "request_id": "1",
  "protocol_version": 1,
  "session_id": "每次连接的新 UUID",
  "device": {
    "device_id": "持久 UUID",
    "name": "SMT-Controller",
    "product": "smt-controller",
    "protocol_version": 1,
    "capabilities": ["status", "ping"]
  },
  "heartbeat_interval_s": 5,
  "idle_timeout_s": 20
}
```

## 保活与状态

建议每 5 秒发送一次 ping；服务端 20 秒没有收到完整请求则关闭该连接。
长连接不代表拥有机器控制权，多个客户端可同时查询；默认最多 16 个连接。

```json
{"type":"ping","request_id":"2"}
{"type":"pong","request_id":"2"}
{"type":"get_status","request_id":"3"}
{"type":"status","request_id":"3","status":{"mode":"unconfigured","machine":"disconnected","hardware_connected":false,"motion_enabled":false,"job_id":null,"job_state":null}}
```

上述状态明确表示：网络连接正常，真实硬件驱动尚未接入。
当前客户端使用轮询获取状态。失去连接时应显示断线，重新发现并握手；不自动重发操作请求。

## 错误

```json
{"type":"error","request_id":"4","error":{"code":"UNSUPPORTED_REQUEST","message":"当前仅支持 ping/get_status"}}
```

| 错误码 | 含义 | 服务端行为 |
|---|---|---|
| HELLO_REQUIRED | 未先握手 | 回复后关闭连接 |
| PROTOCOL_MISMATCH | 协议版本不匹配 | 回复后关闭连接 |
| DEVICE_MISMATCH | 设备 ID 不匹配 | 回复后关闭连接 |
| INVALID_MESSAGE | 格式、字段或长度无效 | 尽可能回复后关闭；无法识别 ID 时为 null |
| UNSUPPORTED_REQUEST | 不支持的请求，包括所有运动命令 | 回复后保留连接 |

连接数达到上限、对端断开或读取/写入超时，可直接关闭，不保证发送错误帧。
Python 客户端 SDK 遇到协议错误或超时会关闭自己的连接，避免后续请求串错回复。

当前为可信局域网内的明文只读服务，未实现身份认证或加密。
将来开放运动接口前，需单独定义认证、唯一控制权、请求去重、任务生命周期和故障处理；
网络握手成功不能直接使机器进入可运动状态。

参考：[python-zeroconf API](https://python-zeroconf.readthedocs.io/en/latest/api.html)。
