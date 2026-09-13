import asyncio
from collections.abc import Callable
from uuid import uuid4

from smt_controller.connectivity.identity import DeviceIdentity
from smt_controller.connectivity.protocol import (
    DEFAULT_PORT, HEARTBEAT_SECONDS, IDLE_TIMEOUT_SECONDS, MAX_FRAME_BYTES,
    PROTOCOL_VERSION, ProtocolError, close_writer, read_message, write_message,
)


class ConnectionServer:
    """只读连接服务；业务状态通过回调注入，没有运动命令入口。"""

    def __init__(self, identity: DeviceIdentity, status: Callable[[], dict], *,
                 idle_timeout: float = IDLE_TIMEOUT_SECONDS, max_clients: int = 16):
        self.identity = identity
        self.status = status
        self.idle_timeout = idle_timeout
        self.max_clients = max_clients
        self._server: asyncio.Server | None = None
        self._tasks: set[asyncio.Task] = set()
        self._closing = False
        self.port = 0

    async def start(self, host: str = "0.0.0.0", port: int = DEFAULT_PORT) -> None:
        self._closing = False
        self._server = await asyncio.start_server(self._accept, host, port, limit=MAX_FRAME_BYTES)
        self.port = self._server.sockets[0].getsockname()[1]

    def _accept(self, reader: asyncio.StreamReader, writer: asyncio.StreamWriter) -> None:
        if self._closing or len(self._tasks) >= self.max_clients:
            writer.close()
            return
        task = asyncio.create_task(self._handle(reader, writer))
        self._tasks.add(task)
        task.add_done_callback(self._tasks.discard)

    async def _handle(self, reader: asyncio.StreamReader, writer: asyncio.StreamWriter) -> None:
        connected = False
        request_id = None
        try:
            while True:
                request_id = None
                message = await read_message(reader, self.idle_timeout if connected else 5)
                request_id = message.get("request_id")
                if not isinstance(request_id, str) or not 1 <= len(request_id) <= 64:
                    request_id = None
                    raise ProtocolError("request_id 必须为 1–64 字符的字符串")
                kind = message.get("type")
                if not connected:
                    if kind != "hello":
                        await self._error(writer, request_id, "HELLO_REQUIRED", "请先握手")
                        return
                    version = message.get("protocol_version")
                    if type(version) is not int or version != PROTOCOL_VERSION:
                        await self._error(writer, request_id, "PROTOCOL_MISMATCH", "仅支持协议版本 1")
                        return
                    expected = message.get("expected_device_id")
                    if expected is not None and expected != self.identity.device_id:
                        await self._error(writer, request_id, "DEVICE_MISMATCH", "设备 ID 不匹配")
                        return
                    client_name = message.get("client_name", "anonymous")
                    if not isinstance(client_name, str) or not 1 <= len(client_name) <= 128:
                        raise ProtocolError("client_name 必须为 1–128 字符的字符串")
                    connected = True
                    reply = {
                        "type": "hello", "request_id": request_id,
                        "protocol_version": PROTOCOL_VERSION,
                        "session_id": str(uuid4()), "device": self.identity.describe(),
                        "heartbeat_interval_s": HEARTBEAT_SECONDS,
                        "idle_timeout_s": self.idle_timeout,
                    }
                elif kind == "ping":
                    reply = {"type": "pong", "request_id": request_id}
                elif kind == "get_status":
                    reply = {"type": "status", "request_id": request_id, "status": self.status()}
                else:
                    await self._error(writer, request_id, "UNSUPPORTED_REQUEST", "当前仅支持 ping/get_status")
                    continue
                await write_message(writer, reply)
        except ProtocolError as exc:
            try:
                await self._error(writer, request_id, "INVALID_MESSAGE", str(exc))
            except (OSError, TimeoutError):
                pass
        except (EOFError, OSError, TimeoutError):
            pass
        finally:
            await close_writer(writer)

    @staticmethod
    async def _error(writer, request_id, code, message) -> None:
        await write_message(writer, {
            "type": "error", "request_id": request_id,
            "error": {"code": code, "message": message},
        })

    async def close(self) -> None:
        self._closing = True
        if self._server is not None:
            self._server.close()
            await self._server.wait_closed()
        tasks = list(self._tasks)
        for task in tasks:
            task.cancel()
        await asyncio.gather(*tasks, return_exceptions=True)
