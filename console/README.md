# 笔记本侧应用预留

笔记本负责制板文件处理、准备贴装任务，通过以太网与板端通信。
界面形式与框架尚未确定；controller 是独立的纯 Python 无界面程序，不要求托管网页。

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
