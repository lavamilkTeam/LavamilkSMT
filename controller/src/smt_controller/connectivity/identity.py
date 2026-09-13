import os
from dataclasses import dataclass
from pathlib import Path
from tempfile import NamedTemporaryFile
from uuid import UUID, uuid4


@dataclass(frozen=True)
class DeviceIdentity:
    device_id: str
    name: str

    def describe(self) -> dict:
        return {
            "device_id": self.device_id,
            "name": self.name,
            "product": "smt-controller",
            "protocol_version": 1,
            "capabilities": ["status", "ping"],
        }


def load_identity(state_dir: Path, name: str | None = None) -> DeviceIdentity:
    """原子保存设备 ID；重启、重命名和更换 IP 都不会重新生成。"""
    state_dir.mkdir(parents=True, exist_ok=True)
    identity_path = state_dir / "device-id"
    if not identity_path.exists():
        with NamedTemporaryFile(mode="w", dir=state_dir, delete=False) as temporary:
            temporary.write(str(uuid4()) + "\n")
            temporary.flush()
            os.fsync(temporary.fileno())
        try:
            try:
                os.link(temporary.name, identity_path)
            except FileExistsError:
                pass
        finally:
            Path(temporary.name).unlink()
    device_id = str(UUID(identity_path.read_text().strip()))
    display_name = name or f"SMT-{device_id[:8]}"
    if not display_name.strip() or len(display_name.encode("utf-8")) > 128:
        raise ValueError("设备名称必须为 1–128 个 UTF-8 字节")
    return DeviceIdentity(device_id, display_name)
