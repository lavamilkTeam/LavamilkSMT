import math
from dataclasses import dataclass
from urllib.request import urlopen


@dataclass(frozen=True)
class ReceivedPreview:
    sequence: int
    received_at: float
    jpeg: bytes


def iter_preview(url: str, *, timeout: float = 5):
    """接收本项目 MJPEG 流；在客户端工作线程运行，避免阻塞 UI。"""
    with urlopen(url, timeout=timeout) as response:
        if response.headers.get_content_type() != "multipart/x-mixed-replace":
            raise ValueError("不是 MJPEG 预览流")
        boundary = response.headers.get_param("boundary")
        if not isinstance(boundary, str) or not 1 <= len(boundary) <= 100:
            raise ValueError("缺少有效的 multipart boundary")
        boundary = b"--" + boundary.encode("ascii")
        previous = 0
        while True:
            line = response.readline(256).rstrip(b"\r\n")
            if not line or line == boundary + b"--":
                return
            if line != boundary:
                raise ValueError("无效的帧分界")
            headers = {}
            for _ in range(16):
                line = response.readline(1024)
                if line == b"\r\n":
                    break
                if not line.endswith(b"\r\n") or b":" not in line:
                    raise ValueError("无效的帧头")
                key, value = line.decode("ascii").split(":", 1)
                headers[key.lower()] = value.strip()
            else:
                raise ValueError("帧头过长")
            length = int(headers.get("content-length", "0"))
            sequence = int(headers.get("x-frame-sequence", "0"))
            received_at = float(headers.get("x-received-at", "nan"))
            if (headers.get("content-type") != "image/jpeg" or not 1 <= length <= 4 * 1024 * 1024
                    or sequence <= previous or not math.isfinite(received_at)):
                raise ValueError("无效的图像长度、序号或时间戳")
            jpeg = response.read(length)
            if len(jpeg) != length or response.read(2) != b"\r\n":
                raise EOFError("预览帧传输中断")
            previous = sequence
            yield ReceivedPreview(sequence, received_at, jpeg)
