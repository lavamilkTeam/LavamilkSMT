from dataclasses import dataclass
from math import isfinite


@dataclass(frozen=True)
class MachinePose:
    """机器绝对坐标，长度为 mm，旋转为 degree。"""

    x_mm: float
    y_mm: float
    z_mm: float
    rotation_deg: float = 0.0

    def __post_init__(self) -> None:
        if not all(isfinite(v) for v in (
            self.x_mm, self.y_mm, self.z_mm, self.rotation_deg
        )):
            raise ValueError("机器坐标必须为有限数值")
