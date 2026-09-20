import asyncio
from uuid import uuid4

from smt_controller.connectivity.protocol import (
    DEFAULT_PORT, MAX_FRAME_BYTES, PROTOCOL_VERSION,
    ProtocolError, close_writer, read_message, write_message,
)


class ControllerClient:
    """可供桌面客户端复用；请求串行化，超时后关闭连接以免串错回复。"""

    def __init__(self, reader, writer, timeout: float):
        self._reader = reader
        self._writer = writer
        self._timeout = timeout
        self._lock = asyncio.Lock()
        self.hello: dict = {}

    @classmethod
    async def connect(cls, host: str, port: int = DEFAULT_PORT, *,
                      expected_device_id: str | None = None, timeout: float = 3):
        async with asyncio.timeout(timeout):
            reader, writer = await asyncio.open_connection(host, port, limit=MAX_FRAME_BYTES)
        client = cls(reader, writer, timeout)
        try:
            client.hello = await client.request(
                "hello", protocol_version=PROTOCOL_VERSION,
                expected_device_id=expected_device_id, client_name="smt-python-client",
            )
            hello = client.hello
            if (hello.get("type") != "hello" or hello.get("protocol_version") != PROTOCOL_VERSION
                    or not isinstance(hello.get("device"), dict)):
                raise ProtocolError("服务端握手响应无效")
            if expected_device_id is not None and hello["device"].get("device_id") != expected_device_id:
                raise ProtocolError("服务端设备 ID 不匹配")
            return client
        except BaseException:
            await client.close()
            raise

    async def request(self, kind: str, **payload) -> dict:
        async with self._lock:
            request_id = uuid4().hex
            try:
                await write_message(self._writer, {**payload, "type": kind, "request_id": request_id})
                response = await read_message(self._reader, self._timeout)
                if response.get("request_id") != request_id:
                    raise ProtocolError("响应 request_id 不匹配")
                if response.get("type") == "error":
                    raise ProtocolError(str(response.get("error")))
                expected_type = {"hello": "hello", "ping": "pong", "get_status": "status",
                                 "check_mcu_update": "mcu_update", "get_mcu_update_status": "mcu_update",
                                 "start_mcu_update": "mcu_update"}.get(kind)
                if expected_type is not None and response.get("type") != expected_type:
                    raise ProtocolError("响应类型不匹配")
                return response
            except BaseException:
                await self.close()
                raise

    async def close(self) -> None:
        await close_writer(self._writer)

    async def __aenter__(self):
        return self

    async def __aexit__(self, *exc):
        await self.close()
