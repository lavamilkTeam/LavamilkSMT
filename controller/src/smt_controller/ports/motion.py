from typing import Protocol

from smt_controller.common.geometry import MachinePose


class MotionController(Protocol):
    async def connect(self) -> None: ...

    async def home(self) -> None:
        """回零完成才返回；ACK 不代表完成；失败或超时抛异常。"""
        ...

    async def move_to(self, target: MachinePose) -> None:
        """运动完成才返回；真实实现需关联命令编号并处理超时。"""
        ...

    async def close(self) -> None:
        """释放连接。关闭连接不等价于停止运动。"""
        ...
