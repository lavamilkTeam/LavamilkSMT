import asyncio
from time import monotonic

from smt_controller.common.cameras import CameraRole
from smt_controller.common.geometry import MachinePose
from smt_controller.ports.camera import Frame
from smt_controller.vision.models import Detection


class SimulatedMotionController:
    def __init__(self) -> None:
        self.connected = False
        self.position: MachinePose | None = None

    async def connect(self) -> None:
        self.connected = True

    async def home(self) -> None:
        if not self.connected:
            raise RuntimeError("模拟控制器未连接")
        await asyncio.sleep(0.01)
        self.position = MachinePose(0, 0, 0)

    async def move_to(self, target: MachinePose) -> None:
        if not self.connected or self.position is None:
            raise RuntimeError("模拟控制器未回零")
        await asyncio.sleep(0.01)
        self.position = target

    async def close(self) -> None:
        self.connected = False
        self.position = None


class SimulatedCamera:
    def __init__(self, role: CameraRole = CameraRole.DOWN_LOOKING) -> None:
        self._frame_id = 0
        self._role = role

    @property
    def role(self) -> CameraRole:
        return self._role

    async def capture_after_settle(self) -> Frame:
        await asyncio.sleep(0.01)
        self._frame_id += 1
        return Frame(self._frame_id, 8, 8, bytes(64), monotonic(),
                     camera_role=self.role, simulated=True)

    async def close(self) -> None:
        pass


class SimulatedLocator:
    async def locate(self, frame: Frame) -> Detection:
        if not frame.simulated:
            raise ValueError("模拟定位器不能处理真实图像")
        # 固定的合成结果，用于验证调用关系，不是视觉算法。
        return Detection(frame.frame_id, 3.5, 3.5, 0.0,
                         camera_role=frame.camera_role, simulated=True)
