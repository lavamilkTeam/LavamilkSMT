import asyncio
import json
import logging
import signal
from dataclasses import asdict

from smt_controller.connectivity.client import ControllerClient
from smt_controller.connectivity.discovery import DiscoveryPublisher, discover
from smt_controller.connectivity.identity import load_identity
from smt_controller.connectivity.protocol import HEARTBEAT_SECONDS, ProtocolError
from smt_controller.connectivity.server import ConnectionServer
from smt_controller.firmware_update.client import UpdaterClient
from smt_controller.firmware_update.service import MCUUpdateService


def disconnected_status() -> dict:
    return {
        "mode": "unconfigured", "machine": "disconnected",
        "hardware_connected": False, "motion_enabled": False,
        "job_id": None, "job_state": None,
    }


async def serve(args) -> None:
    identity = load_identity(args.state_dir, args.name)
    updates = MCUUpdateService(UpdaterClient(args.mcu_updater_socket))
    server = ConnectionServer(identity, disconnected_status, update_handler=updates.handle)
    stop = asyncio.Event()
    loop = asyncio.get_running_loop()
    for sig in (signal.SIGTERM, signal.SIGINT):
        loop.add_signal_handler(sig, stop.set)
    publisher_task = None
    stop_task = None
    try:
        await server.start(port=args.port)
        publisher_task = asyncio.create_task(DiscoveryPublisher(identity, server.port, args.interface).run())
        stop_task = asyncio.create_task(stop.wait())
        print(json.dumps({"event": "listening", **identity.describe(), "port": server.port}, ensure_ascii=False), flush=True)
        done, _ = await asyncio.wait([publisher_task, stop_task], return_when=asyncio.FIRST_COMPLETED)
        if publisher_task in done:
            await publisher_task  # 发现服务故障使进程退出，由 systemd 重启。
    finally:
        for task in (publisher_task, stop_task):
            if task is not None:
                task.cancel()
        await asyncio.gather(*(t for t in (publisher_task, stop_task) if t is not None), return_exceptions=True)
        await server.close()
        for sig in (signal.SIGTERM, signal.SIGINT):
            loop.remove_signal_handler(sig)


async def run_network(args) -> None:
    if args.serve:
        logging.basicConfig(level=logging.INFO)
        await serve(args)
        return
    if args.discover:
        print(json.dumps([asdict(d) for d in await discover(args.timeout, args.interface)], ensure_ascii=False, indent=2))
        return
    if args.connect_id:
        devices = await discover(args.timeout, args.interface)
        device = next((d for d in devices if d.device_id == args.connect_id), None)
        if device is None:
            raise ValueError("未发现指定设备；请重新发现，或使用 --connect IP")
        client = None
        failures = []
        for address in device.addresses:
            try:
                client = await ControllerClient.connect(address, device.port, expected_device_id=device.device_id)
                break
            except (OSError, TimeoutError, EOFError, ProtocolError) as exc:
                failures.append(f"{address}: {exc}")
        if client is None:
            raise ConnectionError("设备所有地址均连接失败: " + "; ".join(failures))
    else:
        client = await ControllerClient.connect(args.connect, args.port)
    async with client:
        print(json.dumps(client.hello, ensure_ascii=False), flush=True)
        while True:
            print(json.dumps(await client.request("get_status"), ensure_ascii=False), flush=True)
            if args.once:
                return
            await asyncio.sleep(HEARTBEAT_SECONDS)
            await client.request("ping")
