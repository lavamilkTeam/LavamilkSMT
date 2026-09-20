import asyncio
from pathlib import Path
from uuid import uuid4

from smt_controller.connectivity.protocol import (
    MAX_FRAME_BYTES, ProtocolError, close_writer, read_message, write_message,
)


class UpdaterClient:
    """One bounded Unix-socket exchange; no blocking I/O in the control event loop."""

    def __init__(self, socket_path: Path, timeout: float = 10):
        self.socket_path = socket_path
        self.timeout = timeout

    async def request(self, kind: str, **payload) -> dict:
        request_id = uuid4().hex
        async with asyncio.timeout(self.timeout):
            reader, writer = await asyncio.open_unix_connection(
                str(self.socket_path), limit=MAX_FRAME_BYTES)
            try:
                await write_message(writer, {
                    **payload, "type": kind, "request_id": request_id, "protocol_version": 1,
                })
                response = await read_message(reader, self.timeout)
                if response.get("request_id") != request_id or response.get("protocol_version") != 1:
                    raise ProtocolError("升级服务响应 ID 或协议版本不匹配")
                if "error" in response:
                    raise ProtocolError(str(response["error"]))
                if not isinstance(response.get("result"), dict):
                    raise ProtocolError("升级服务响应格式错误")
                return response["result"]
            finally:
                await close_writer(writer)
