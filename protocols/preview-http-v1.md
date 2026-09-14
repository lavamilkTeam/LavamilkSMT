# OpenCV 预览 HTTP v1

板端独立 `smt-preview` 进程读取 USB 相机，使用 OpenCV 处理并编码 JPEG，通过以太网发送。
默认监听 `0.0.0.0:8766`，与 TCP 8765 控制连接分开。第一版使用 640×480、
输出上限 10fps、JPEG 质量 75；实际帧率以板端测量为准。

## 接口

均为 GET，无需请求体。将地址替换成发现结果中的可达 IP；当前局域网维护地址为
`192.168.2.7`，直连静态地址为 `192.168.1.100`，使用静态地址时笔记本须在相同网段。

| 路径 | 内容 |
| --- | --- |
| `/stream/gray.mjpg` | 灰度 MJPEG 视频流 |
| `/stream/threshold.mjpg` | 高斯滤波 + Otsu 二值化 MJPEG 视频流 |
| `/stream/contours.mjpg` | 灰度底图上的绿色轮廓叠加 MJPEG 视频流 |
| `/snapshot/{mode}.jpg` | 对应模式的最新 JPEG 快照 |
| `/status.json` | 相机是否就绪、错误、帧龄、序号、分辨率和处理耗时 |
| `/` | 重定向至轮廓视频流 |

`mode` 为 `gray`、`threshold`、`contours`。支持 MJPEG 的浏览器可直接打开流地址；
桌面程序使用下面的接收器。当前设备发现只发布控制服务，客户端取得 IP 后使用预览端口 8766。
此接口用于可信局域网，无登录、TLS 或机器控制操作。

## 图像与缓冲约定

一个采集线程独占相机，每次处理同时生成三种画面；所有客户端共享编码结果。
只保存最新一组图像，慢客户端跳过旧帧，不为每个连接重复采集或处理。最多 4 个视频连接，
超额返回 503；额外限制请求线程数量，超过总请求容量会关闭新连接。

状态中的 `ready=false` 表示暂无有效画面。最后发布帧超过 2 秒视为过期；
快照/新视频连接等待最多 3 秒仍无有效帧时返回 503，现有流等待新帧超时后结束。
发送超时会关闭慢连接。相机读取失败清除缓存，每 2 秒尝试重新打开；客户端需重新连接。

图像响应含 `X-Frame-Sequence` 和 `X-Received-At`。序号在同一进程内递增，进程重启归零；
时间戳是板端完成解码后的 Unix 时间，**不是传感器曝光时间**，不能用于证明运动停稳后曝光。
JPEG 是有损格式，二值图经过传输解码后可能在边缘产生中间灰度。

轮廓视图仅展示通用阈值分割后的轮廓，不代表已识别 Mark、元件或计算出可用贴装坐标。
处理扩展位置为 `smt_controller/preview/processing.py` 的 `PreviewProcessor.process()`。

视频使用 `Content-Type: multipart/x-mixed-replace; boundary=smt-frame`，每帧格式为：

```text
--smt-frame\r\n
Content-Type: image/jpeg\r\n
Content-Length: <JPEG 字节数>\r\n
X-Frame-Sequence: <序号>\r\n
X-Received-At: <Unix 秒>\r\n
\r\n
<JPEG 二进制>\r\n
```

## Python 接收

从仓库根目录安装基础客户端包即可，接收 JPEG 不依赖 OpenCV：

```bash
python -m pip install -e ./controller
python console/examples/receive_preview.py --host 192.168.2.7 --mode contours --frames 30 --output artifacts/preview.jpg
```

在桌面程序的工作线程中消费流，再将图像提交给 UI 线程；离开页面时关闭接收器：

```python
from contextlib import closing
from smt_controller.preview.client import iter_preview

with closing(iter_preview("http://192.168.2.7:8766/stream/contours.mjpg")) as stream:
    for frame in stream:
        # frame.jpeg 可交给桌面框架解码；队列同样只保留最新画面。
        # OpenCV 客户端可用 cv2.imdecode(np.frombuffer(frame.jpeg, np.uint8), cv2.IMREAD_COLOR)。
        print(frame.sequence, len(frame.jpeg))
```

接收器不自动重连；调用端在 EOF、网络异常或 503 后退避重试，并显示离线状态，避免把旧图当实时图。
板端 OpenCV 未启用 FFmpeg/GStreamer，因此接收示例直接解析 HTTP，不依赖 `VideoCapture(URL)`。

## 板端运行

已有 NumPy/OpenCV 环境后：

```bash
/opt/smt/venv/bin/python -m smt_controller.app.preview --device /dev/video0 --width 640 --height 480 --fps 10 --quality 75
```

常驻运行使用 [服务模板](../deploy/smt-preview.service)，以 `smt-controller` 用户及 `video`
附加组访问相机，开机启动。具体部署见 [目标板环境](../docs/target-environment.md)。
服务启用时不要另起手动采集进程。锁文件仅协调使用相同 `--lock-file` 路径的实例，
不能锁住不遵守约定的相机程序。

后续接入真实定位时，应由统一相机采集层发布预览副本，预览端只编码与发送；
不能让预览进程与正式定位适配器分别打开同一只相机。定位使用的停稳后有效帧另行校验。
