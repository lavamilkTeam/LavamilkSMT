from dataclasses import dataclass
from enum import Enum

from smt_controller.common.cameras import CameraRole
from smt_controller.common.geometry import MachinePose
from smt_controller.vision.models import Detection


class JobState(str, Enum):
    CREATED = "created"
    MOVING = "moving"
    CAPTURING = "capturing"
    LOCATING = "locating"
    COMPLETED = "completed"
    FAILED = "failed"


@dataclass(frozen=True)
class InspectionJob:
    job_id: str
    target: MachinePose
    camera_role: CameraRole = CameraRole.DOWN_LOOKING


@dataclass(frozen=True)
class JobSnapshot:
    job_id: str
    state: JobState
    detection: Detection | None = None
    error: str | None = None
