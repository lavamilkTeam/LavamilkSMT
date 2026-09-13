class MachineBusyError(RuntimeError):
    """已有操作持有机器控制权。"""


class MachineNotReadyError(RuntimeError):
    """机器未就绪，或上次操作失败后尚未完成状态核对。"""
