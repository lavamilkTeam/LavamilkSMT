from smt_controller.connectivity.protocol import ProtocolError
from smt_controller.firmware_update.client import UpdaterClient


class MCUUpdateService:
    """The current controller has no real stop/output/version adapter; never issue a permit."""

    def __init__(self, updater: UpdaterClient):
        self.updater = updater

    async def handle(self, kind: str) -> dict:
        if kind == "start_mcu_update":
            # No file written, no Go start call, no inference of safety from disconnected/idle.
            return {"can_update": False, "state": "blocked", "code": "MAINTENANCE_UNAVAILABLE",
                    "blockers": ["真实下位机的停机、输出安全确认及维护恢复尚未接入，禁止烧录。"]}
        operation = {"check_mcu_update": "check", "get_mcu_update_status": "status"}.get(kind)
        if operation is None:
            raise ValueError("不支持的更新请求")
        try:
            result = await self.updater.request(operation)
        except (OSError, EOFError, TimeoutError, ProtocolError) as exc:
            return {"can_update": False, "state": "unavailable", "code": "UPDATER_UNAVAILABLE",
                    "blockers": ["下位机升级服务不可用，请检查服务是否启动。"],
                    "detail": str(exc)[:512]}
        # A Go readiness result is never permission to operate the machine.
        result["can_update"] = False
        result["state"] = "blocked"
        result["code"] = "MAINTENANCE_UNAVAILABLE"
        blockers = result.get("blockers", [])
        if not isinstance(blockers, list) or not all(isinstance(x, str) for x in blockers):
            return {"can_update": False, "state": "unavailable", "code": "UPDATER_INVALID_RESPONSE",
                    "blockers": ["升级服务返回的状态格式错误。"]}
        result["blockers"] = blockers + ["真实下位机维护接口尚未接入，当前只允许检查与查询。"]
        return result
