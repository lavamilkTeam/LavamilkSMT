from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

from smt_controller.common.errors import MachineBusyError, MachineNotReadyError
from smt_controller.common.geometry import MachinePose
from smt_controller.machine.state import MachineState
from smt_controller.ports.motion import MotionController


class MachineSession:
    """限定于一次操作的运动能力，退出操作后失效。"""

    def __init__(self, motion: MotionController) -> None:
        self._motion = motion
        self._active = True

    async def move_to(self, target: MachinePose) -> None:
        if not self._active:
            raise MachineNotReadyError("操作已结束")
        await self._motion.move_to(target)


class MachineCoordinator:
    """由一个 asyncio 事件循环拥有；不允许跨线程直接调用。

    当前只有模拟启动。故障不自动清除；真实恢复流程需核对下位机状态。
    """

    def __init__(self, motion: MotionController) -> None:
        self._motion = motion
        self._state = MachineState.DISCONNECTED

    @property
    def state(self) -> MachineState:
        return self._state

    async def initialize(self) -> None:
        if self._state is not MachineState.DISCONNECTED:
            raise MachineNotReadyError("机器已初始化或处于故障状态")
        self._state = MachineState.BUSY
        try:
            await self._motion.connect()
            await self._motion.home()
        except BaseException:
            self._state = MachineState.FAULT
            raise
        self._state = MachineState.IDLE

    @asynccontextmanager
    async def operation(self) -> AsyncIterator[MachineSession]:
        if self._state is MachineState.BUSY:
            raise MachineBusyError("机器已有操作，拒绝插入指令")
        if self._state is not MachineState.IDLE:
            raise MachineNotReadyError(f"机器未就绪：{self._state.value}")
        # 检查和占用之间没有 await，同一个事件循环中不会被其他任务插入。
        self._state = MachineState.BUSY
        session = MachineSession(self._motion)
        try:
            yield session
        except BaseException:
            # 包含取消：停止等待不代表物理动作已停止，必须保持故障状态。
            self._state = MachineState.FAULT
            raise
        else:
            self._state = MachineState.IDLE
        finally:
            session._active = False

    async def close(self) -> None:
        if self._state is MachineState.BUSY:
            raise MachineBusyError("先结束当前操作，再释放连接")
        await self._motion.close()
        if self._state is not MachineState.FAULT:
            self._state = MachineState.DISCONNECTED
