import asyncio
from collections.abc import Mapping

from smt_controller.common.cameras import CameraRole

from smt_controller.jobs.models import InspectionJob, JobSnapshot, JobState
from smt_controller.machine.coordinator import MachineCoordinator
from smt_controller.ports.camera import Camera
from smt_controller.ports.vision import Locator


class InspectionWorkflow:
    """最小演示：移动完成 → 获取有效帧 → 定位。

    不包含取料、贴装或暂停恢复。一个操作期间独占机器，不创建脱离流程的任务。
    """

    def __init__(self, machine: MachineCoordinator,
                 cameras: Mapping[CameraRole, Camera], locator: Locator) -> None:
        self._machine = machine
        self._cameras = dict(cameras)
        for role, camera in self._cameras.items():
            if camera.role != role:
                raise ValueError("相机注册角色与设备角色不一致")
        self._locator = locator
        self._snapshot: JobSnapshot | None = None

    @property
    def snapshot(self) -> JobSnapshot | None:
        return self._snapshot

    async def run(self, job: InspectionJob) -> JobSnapshot:
        # 在任何运动之前确认请求的相机已配置。
        camera = self._cameras[job.camera_role]
        # 先取得控制权，再更新快照；被拒绝的新请求不能覆盖正在运行的任务。
        async with self._machine.operation() as session:
            try:
                self._snapshot = JobSnapshot(job.job_id, JobState.MOVING)
                await session.move_to(job.target)
                self._snapshot = JobSnapshot(job.job_id, JobState.CAPTURING)
                frame = await camera.capture_after_settle()
                if frame.camera_role != job.camera_role:
                    raise ValueError("图像不属于请求的相机")
                self._snapshot = JobSnapshot(job.job_id, JobState.LOCATING)
                detection = await self._locator.locate(frame)
                if (detection.frame_id != frame.frame_id
                        or detection.camera_role != frame.camera_role):
                    raise ValueError("识别结果不属于当前图像")
                self._snapshot = JobSnapshot(job.job_id, JobState.COMPLETED, detection)
                return self._snapshot
            except (Exception, asyncio.CancelledError) as exc:
                self._snapshot = JobSnapshot(
                    job.job_id, JobState.FAILED, error=str(exc) or type(exc).__name__
                )
                raise
