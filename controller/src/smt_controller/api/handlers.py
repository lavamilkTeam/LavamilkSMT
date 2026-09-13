from smt_controller.jobs.models import InspectionJob, JobSnapshot
from smt_controller.jobs.workflow import InspectionWorkflow
from smt_controller.machine.coordinator import MachineCoordinator


class ControllerAPI:
    """不绑定 HTTP/TCP 框架，不直接访问硬件。

    目前 inspect 等待执行结束。后续网络层负责校验、请求去重和任务 ID，
    不能让客户端断开连接直接决定物理任务生命周期。
    """

    def __init__(self, machine: MachineCoordinator, workflow: InspectionWorkflow) -> None:
        self._machine = machine
        self._workflow = workflow

    async def inspect(self, job: InspectionJob) -> JobSnapshot:
        return await self._workflow.run(job)

    def status(self) -> dict[str, str | None]:
        snapshot = self._workflow.snapshot
        return {
            "machine": self._machine.state.value,
            "job_id": snapshot.job_id if snapshot else None,
            "job_state": snapshot.state.value if snapshot else None,
        }
