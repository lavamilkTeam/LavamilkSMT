import asyncio
import json

PROTOCOL_VERSION = 1
DEFAULT_PORT = 8765
MAX_FRAME_BYTES = 8192
HEARTBEAT_SECONDS = 5
IDLE_TIMEOUT_SECONDS = 20


class ProtocolError(Exception):
    pass


def _invalid_constant(value: str) -> None:
    raise ValueError(f"不允许 JSON 常量 {value}")


async def read_message(reader: asyncio.StreamReader, timeout: float) -> dict:
    try:
        async with asyncio.timeout(timeout):
            data = await reader.readuntil(b"\n")
    except asyncio.LimitOverrunError as exc:
        raise ProtocolError("报文超过长度限制") from exc
    except asyncio.IncompleteReadError as exc:
        raise EOFError("连接已关闭") from exc
    if len(data) > MAX_FRAME_BYTES:
        raise ProtocolError("报文超过长度限制")
    try:
        message = json.loads(data.decode("utf-8"), parse_constant=_invalid_constant)
    except (ValueError, RecursionError) as exc:
        raise ProtocolError("报文必须为 UTF-8 JSON 对象") from exc
    if not isinstance(message, dict):
        raise ProtocolError("报文必须为 JSON 对象")
    return message


async def write_message(writer: asyncio.StreamWriter, message: dict) -> None:
    data = json.dumps(message, ensure_ascii=False, allow_nan=False, separators=(",", ":")).encode() + b"\n"
    if len(data) > MAX_FRAME_BYTES:
        raise ProtocolError("报文超过长度限制")
    writer.write(data)
    async with asyncio.timeout(5):
        await writer.drain()


async def close_writer(writer: asyncio.StreamWriter) -> None:
    writer.close()
    try:
        async with asyncio.timeout(2):
            await writer.wait_closed()
    except (OSError, TimeoutError):
        pass
