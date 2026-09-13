from enum import Enum


class CameraRole(str, Enum):
    """按观看方向命名，避免上/下安装位置造成歧义。"""

    DOWN_LOOKING = "down_looking"  # 随贴装头移动，拍 PCB Mark 和料位
    UP_LOOKING = "up_looking"      # 固定在工作台，拍吸取后的元件
