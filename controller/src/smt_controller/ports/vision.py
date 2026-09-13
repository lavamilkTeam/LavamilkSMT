from typing import Protocol

from smt_controller.ports.camera import Frame
from smt_controller.vision.models import Detection


class Locator(Protocol):
    async def locate(self, frame: Frame) -> Detection:
        """只返回测量结果；耗时计算交给 worker，不操作机器。"""
        ...
