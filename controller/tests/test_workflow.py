import asyncio
import unittest

from smt_controller.adapters.simulation import SimulatedCamera, SimulatedLocator, SimulatedMotionController
from smt_controller.app.bootstrap import simulated_application
from smt_controller.common.cameras import CameraRole
from smt_controller.common.errors import MachineBusyError, MachineNotReadyError
from smt_controller.common.geometry import MachinePose
from smt_controller.jobs.models import InspectionJob, JobState
from smt_controller.jobs.workflow import InspectionWorkflow
from smt_controller.machine.coordinator import MachineCoordinator
from smt_controller.machine.state import MachineState
from smt_controller.vision.models import Detection


class GatedCamera(SimulatedCamera):
    def __init__(self):
        super().__init__()
        self.entered = asyncio.Event()
        self.release = asyncio.Event()

    async def capture_after_settle(self):
        self.entered.set()
        await self.release.wait()
        return await super().capture_after_settle()


class StaleLocator:
    async def locate(self, frame):
        return Detection(frame.frame_id - 1, 0, 0, 0,
                         camera_role=frame.camera_role, simulated=True)


class WrongCameraLocator:
    async def locate(self, frame):
        return Detection(frame.frame_id, 0, 0, 0,
                         camera_role=CameraRole.UP_LOOKING, simulated=True)


class WorkflowTests(unittest.IsolatedAsyncioTestCase):
    async def test_demo_completes_with_explicitly_simulated_result(self):
        async with simulated_application() as api:
            result = await api.inspect(InspectionJob("test", MachinePose(10, 20, 5)))
            self.assertEqual(result.state, JobState.COMPLETED)
            self.assertTrue(result.detection.simulated)
            self.assertEqual(api.status()["machine"], "idle")

    async def test_concurrent_request_preserves_active_job(self):
        machine = MachineCoordinator(SimulatedMotionController())
        camera = GatedCamera()
        workflow = InspectionWorkflow(machine, {camera.role: camera}, SimulatedLocator())
        await machine.initialize()
        task = asyncio.create_task(workflow.run(InspectionJob("first", MachinePose(0, 0, 5))))
        try:
            await asyncio.wait_for(camera.entered.wait(), 1)
            with self.assertRaises(MachineBusyError):
                await workflow.run(InspectionJob("second", MachinePose(10, 10, 5)))
            self.assertEqual(workflow.snapshot.job_id, "first")
        finally:
            camera.release.set()
            await asyncio.wait_for(task, 1)
            await machine.close()

    async def test_stale_result_latches_fault(self):
        machine = MachineCoordinator(SimulatedMotionController())
        camera = SimulatedCamera()
        workflow = InspectionWorkflow(machine, {camera.role: camera}, StaleLocator())
        await machine.initialize()
        try:
            with self.assertRaises(ValueError):
                await workflow.run(InspectionJob("stale", MachinePose(0, 0, 5)))
            self.assertEqual(workflow.snapshot.state, JobState.FAILED)
            self.assertEqual(machine.state, MachineState.FAULT)
            with self.assertRaises(MachineNotReadyError):
                await workflow.run(InspectionJob("next", MachinePose(0, 0, 5)))
        finally:
            await machine.close()

    async def test_cancel_does_not_report_idle(self):
        machine = MachineCoordinator(SimulatedMotionController())
        camera = GatedCamera()
        workflow = InspectionWorkflow(machine, {camera.role: camera}, SimulatedLocator())
        await machine.initialize()
        task = asyncio.create_task(workflow.run(InspectionJob("cancel", MachinePose(0, 0, 5))))
        try:
            await asyncio.wait_for(camera.entered.wait(), 1)
        finally:
            task.cancel()
            with self.assertRaises(asyncio.CancelledError):
                await task
            await machine.close()
        self.assertEqual(machine.state, MachineState.FAULT)
        self.assertEqual(workflow.snapshot.state, JobState.FAILED)

    async def test_finished_session_cannot_move(self):
        machine = MachineCoordinator(SimulatedMotionController())
        await machine.initialize()
        try:
            async with machine.operation() as session:
                await session.move_to(MachinePose(0, 0, 5))
            with self.assertRaises(MachineNotReadyError):
                await session.move_to(MachinePose(10, 10, 5))
        finally:
            await machine.close()

    async def test_two_cameras_can_have_same_frame_id_without_mixing_results(self):
        async with simulated_application() as api:
            down = await api.inspect(InspectionJob("mark", MachinePose(10, 20, 5)))
            up = await api.inspect(InspectionJob(
                "component", MachinePose(30, 40, 5), CameraRole.UP_LOOKING
            ))
            self.assertEqual(down.detection.frame_id, up.detection.frame_id)
            self.assertEqual(down.detection.camera_role, CameraRole.DOWN_LOOKING)
            self.assertEqual(up.detection.camera_role, CameraRole.UP_LOOKING)

    async def test_wrong_camera_result_is_rejected_even_with_matching_frame_id(self):
        machine = MachineCoordinator(SimulatedMotionController())
        camera = SimulatedCamera()
        workflow = InspectionWorkflow(machine, {camera.role: camera}, WrongCameraLocator())
        await machine.initialize()
        try:
            with self.assertRaises(ValueError):
                await workflow.run(InspectionJob("mark", MachinePose(0, 0, 5)))
            self.assertEqual(machine.state, MachineState.FAULT)
        finally:
            await machine.close()
