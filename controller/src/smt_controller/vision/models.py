from dataclasses import dataclass, field

from smt_controller.common.cameras import CameraRole


@dataclass(frozen=True)
class Detection:
    frame_id: int
    center_x_px: float
    center_y_px: float
    angle_deg: float
    camera_role: CameraRole = field(kw_only=True)
    simulated: bool = False
