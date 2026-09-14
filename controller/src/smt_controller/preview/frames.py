import threading
import time
from dataclasses import dataclass
from types import MappingProxyType
from collections.abc import Mapping

MODES = ("gray", "threshold", "contours")


@dataclass(frozen=True)
class PreviewFrame:
    sequence: int
    received_at: float
    published_at: float
    width: int
    height: int
    images: Mapping[str, bytes]
    metrics: Mapping[str, float]


class LatestFrames:
    """只保留最新一组编码图像；消费者使用序号跳过过期帧。"""

    def __init__(self, *, max_age: float = 2):
        self.max_age = max_age
        self._condition = threading.Condition()
        self._latest: PreviewFrame | None = None
        self._sequence = 0
        self._error: str | None = "camera starting"
        self._closed = False

    def publish(self, *, received_at: float, width: int, height: int,
                images: dict[str, bytes], metrics: dict[str, float]) -> None:
        if set(images) != set(MODES) or not all(isinstance(data, bytes) and data for data in images.values()):
            raise ValueError("必须提供三种已编码的预览图像")
        with self._condition:
            if self._closed:
                return
            self._sequence += 1
            self._latest = PreviewFrame(
                self._sequence, received_at, time.monotonic(), width, height,
                MappingProxyType(dict(images)), MappingProxyType(dict(metrics)),
            )
            self._error = None
            self._condition.notify_all()

    def fail(self, reason: str) -> None:
        with self._condition:
            self._latest = None
            self._error = reason
            self._condition.notify_all()

    def wait_after(self, sequence: int, timeout: float = 3) -> PreviewFrame | None:
        deadline = time.monotonic() + timeout
        with self._condition:
            while not self._closed:
                frame = self._latest
                if frame is not None and frame.sequence > sequence and time.monotonic() - frame.published_at <= self.max_age:
                    return frame
                remaining = deadline - time.monotonic()
                if remaining <= 0:
                    return None
                self._condition.wait(remaining)
        return None

    def status(self) -> dict:
        with self._condition:
            frame = self._latest
            age = time.monotonic() - frame.published_at if frame is not None else None
            ready = not self._closed and age is not None and age <= self.max_age
            return {
                "ready": ready, "sequence": self._sequence,
                "frame_age_ms": round(age * 1000, 1) if age is not None else None,
                "received_at_unix_s": frame.received_at if frame is not None else None,
                "width": frame.width if frame is not None else None,
                "height": frame.height if frame is not None else None,
                "metrics": dict(frame.metrics) if ready else {},
                "error": "stopped" if self._closed else (self._error or (None if ready else "frame stale")),
            }

    def close(self) -> None:
        with self._condition:
            self._closed = True
            self._latest = None
            self._condition.notify_all()
