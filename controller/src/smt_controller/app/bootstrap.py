from contextlib import AsyncExitStack, asynccontextmanager
from collections.abc import AsyncIterator

from smt_controller.adapters.simulation import (
    SimulatedCamera,
    SimulatedLocator,
    SimulatedMotionController,
)
from smt_controller.api.handlers import ControllerAPI
from smt_controller.common.cameras import CameraRole
from smt_controller.jobs.workflow import InspectionWorkflow
from smt_controller.machine.coordinator import MachineCoordinator


@asynccontextmanager
async def simulated_application() -> AsyncIterator[ControllerAPI]:
    cameras = {role: SimulatedCamera(role) for role in CameraRole}
    machine = MachineCoordinator(SimulatedMotionController())
    workflow = InspectionWorkflow(machine, cameras, SimulatedLocator())
    async with AsyncExitStack() as resources:
        resources.push_async_callback(machine.close)
        for camera in cameras.values():
            resources.push_async_callback(camera.close)
        await machine.initialize()
        yield ControllerAPI(machine, workflow)
