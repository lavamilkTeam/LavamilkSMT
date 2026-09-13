from dataclasses import dataclass, field
from typing import Protocol

from smt_controller.common.cameras import CameraRole

@dataclass(frozen=True)
class Frame:
    frame_id: int
    width: int
    height: int
    gray8: bytes
    received_at: float
    camera_role: CameraRole = field(kw_only=True)
    simulated: bool = False


class Camera(Protocol):
    @property
    def role(self) -> CameraRole: ...

    async def capture_after_settle(self) -> Frame:
        """返回停稳后有效帧。

        真正的 USB 实现必须验证采集缓存/曝光时序，不能只使用读取时间。
        图像缓冲需有界，阻塞采集不可占用控制事件循环。
        """
        ...

    async def close(self) -> None: ...
