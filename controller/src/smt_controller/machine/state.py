from enum import Enum


class MachineState(str, Enum):
    DISCONNECTED = "disconnected"
    IDLE = "idle"
    BUSY = "busy"
    FAULT = "fault"
