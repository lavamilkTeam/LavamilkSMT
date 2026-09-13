import asyncio
import ipaddress
import logging
from dataclasses import dataclass
from uuid import UUID

import ifaddr
from zeroconf import IPVersion, InterfaceChoice, ServiceInfo, ServiceStateChange
from zeroconf.asyncio import AsyncServiceBrowser, AsyncZeroconf

from smt_controller.connectivity.identity import DeviceIdentity
from smt_controller.connectivity.protocol import PROTOCOL_VERSION

SERVICE_TYPE = "_smt-controller._tcp.local."
logger = logging.getLogger(__name__)


def lan_addresses(interface: str | None = None) -> list[str]:
    addresses = set()
    for adapter in ifaddr.get_adapters():
        if interface is not None and interface not in (adapter.name, adapter.nice_name):
            continue
        for entry in adapter.ips:
            if isinstance(entry.ip, str):
                address = ipaddress.IPv4Address(entry.ip)
                if not (address.is_loopback or address.is_unspecified or address.is_multicast):
                    addresses.add(str(address))
    return sorted(addresses)


def multicast_interfaces(addresses: list[str]) -> list[str]:
    """同一网卡的静态/DHCP 别名只加入一次组播，公告仍携带全部地址。"""
    allowed = set(addresses)
    result = []
    for adapter in ifaddr.get_adapters():
        selected = next((entry.ip for entry in adapter.ips
                         if isinstance(entry.ip, str) and entry.ip in allowed), None)
        if selected is not None:
            result.append(selected)
    return result


def service_info(identity: DeviceIdentity, addresses: list[str], port: int) -> ServiceInfo:
    hostname = f"smt-{UUID(identity.device_id).hex}"
    return ServiceInfo(
        SERVICE_TYPE, f"{hostname}.{SERVICE_TYPE}",
        parsed_addresses=addresses, port=port, server=f"{hostname}.local.",
        properties={"device_id": identity.device_id, "name": identity.name,
                    "protocol": str(PROTOCOL_VERSION), "transport": "tcp-jsonl"},
        host_ttl=30, other_ttl=30,
    )


class DiscoveryPublisher:
    def __init__(self, identity: DeviceIdentity, port: int, interface: str | None = None):
        self.identity, self.port, self.interface = identity, port, interface
        self._zc: AsyncZeroconf | None = None
        self._info: ServiceInfo | None = None

    async def run(self) -> None:
        """网络晚于服务启动、DHCP 更新、拔插网线时重新公布当前地址。"""
        previous: list[str] = []
        try:
            while True:
                addresses = lan_addresses(self.interface)
                if addresses != previous:
                    interfaces = multicast_interfaces(addresses)
                    if self._zc is None:
                        self._zc = AsyncZeroconf(interfaces=interfaces, ip_version=IPVersion.V4Only)
                    if not addresses:
                        if self._info is not None:
                            await (await self._zc.async_unregister_service(self._info))
                            self._info = None
                    else:
                        await self._zc.async_update_interfaces(interfaces=interfaces, ip_version=IPVersion.V4Only)
                        info = service_info(self.identity, addresses, self.port)
                        if self._info is None:
                            await (await self._zc.async_register_service(info))
                        else:
                            await (await self._zc.async_update_service(info))
                        self._info = info
                    previous = addresses
                    logger.info("发现服务地址已更新: %s", addresses)
                await asyncio.sleep(3)
        finally:
            if self._zc is not None:
                try:
                    if self._info is not None:
                        await (await self._zc.async_unregister_service(self._info))
                finally:
                    await self._zc.async_close()


@dataclass(frozen=True)
class DiscoveredDevice:
    device_id: str
    name: str
    addresses: list[str]
    port: int
    hostname: str
    protocol_version: int = PROTOCOL_VERSION


def parse_service(info: ServiceInfo) -> DiscoveredDevice | None:
    properties = info.decoded_properties
    try:
        device_id = str(UUID(properties["device_id"]))
        if properties.get("protocol") != str(PROTOCOL_VERSION) or properties.get("transport") != "tcp-jsonl":
            return None
        name = properties["name"]
        if not isinstance(name, str) or not name or len(name.encode()) > 128:
            return None
        addresses = [str(ipaddress.IPv4Address(a)) for a in info.parsed_addresses(IPVersion.V4Only)]
        if not addresses or not info.port or not 1 <= info.port <= 65535:
            return None
    except (KeyError, TypeError, ValueError):
        return None
    return DiscoveredDevice(device_id, name, addresses, info.port, info.server or "")


async def discover(timeout: float = 5, interface: str | None = None) -> list[DiscoveredDevice]:
    if timeout <= 0:
        raise ValueError("发现超时必须大于零")
    interfaces = multicast_interfaces(lan_addresses(interface)) if interface else InterfaceChoice.All
    if interface is not None and not interfaces:
        raise ValueError(f"网卡 {interface} 没有可用的 IPv4 地址")
    zc = AsyncZeroconf(interfaces=interfaces, ip_version=IPVersion.V4Only)
    found: dict[str, DiscoveredDevice] = {}
    pending: dict[str, asyncio.Task] = {}
    all_tasks: set[asyncio.Task] = set()

    async def resolve(name: str) -> None:
        info = await zc.async_get_service_info(SERVICE_TYPE, name, timeout=1500)
        if info is not None:
            device = parse_service(info)
            if device is not None:
                found[name] = device

    def changed(zeroconf, service_type, name, state_change):
        old = pending.pop(name, None)
        if old is not None:
            old.cancel()
        found.pop(name, None)
        if state_change != ServiceStateChange.Removed:
            task = asyncio.create_task(resolve(name))
            pending[name] = task
            all_tasks.add(task)

    browser = AsyncServiceBrowser(zc.zeroconf, SERVICE_TYPE, handlers=[changed])
    try:
        await asyncio.sleep(timeout)
    finally:
        await browser.async_cancel()
        for task in all_tasks:
            if not task.done():
                task.cancel()
        await asyncio.gather(*all_tasks, return_exceptions=True)
        await zc.async_close()
    # 同一个设备可能从多个接口被发现，用持久 ID 去重。
    return sorted({d.device_id: d for d in found.values()}.values(), key=lambda d: d.device_id)
