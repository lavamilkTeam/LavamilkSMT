import argparse
import asyncio
import json
import os
from dataclasses import asdict
from pathlib import Path

from smt_controller.app.bootstrap import simulated_application
from smt_controller.common.geometry import MachinePose
from smt_controller.jobs.models import InspectionJob
from smt_controller.connectivity.protocol import ProtocolError


async def run_demo() -> None:
    async with simulated_application() as api:
        result = await api.inspect(InspectionJob("demo-inspection", MachinePose(10, 20, 5)))
        print(json.dumps({"mode": "simulation", "result": asdict(result), "status": api.status()},
                         ensure_ascii=False, indent=2))


def main() -> None:
    parser = argparse.ArgumentParser(description="SMT 无界面控制器：模拟流程与局域网连接")
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--demo", action="store_true", help="运行一次模拟移动和定位后退出")
    mode.add_argument("--serve", action="store_true", help="启动发现与只读状态服务")
    mode.add_argument("--discover", action="store_true", help="发现同一局域网的 SMT 控制器")
    mode.add_argument("--connect", metavar="IP", help="通过 IP 连接控制器")
    mode.add_argument("--connect-id", metavar="UUID", help="发现后按设备 ID 连接")
    parser.add_argument("--port", type=int, default=8765, help="TCP 服务端口，默认 8765")
    parser.add_argument("--name", help="设备显示名称")
    parser.add_argument("--interface", help="发现使用的网卡，如 eth0；默认所有 IPv4 网卡")
    parser.add_argument("--state-dir", type=Path,
                        default=Path(os.environ.get("XDG_STATE_HOME", str(Path.home() / ".local/state"))) / "smt-controller",
                        help="持久设备 ID 保存目录")
    parser.add_argument("--timeout", type=float, default=5, help="发现等待秒数，默认 5")
    parser.add_argument("--once", action="store_true", help="连接并查询一次状态后退出")
    parser.add_argument("--mcu-updater-socket", type=Path,
                        default=Path("/run/smt-mcu-updater/service.sock"),
                        help="本机 Go 下位机升级服务 Unix socket")
    args = parser.parse_args()
    if not 1 <= args.port <= 65535 or not 0 < args.timeout <= 60:
        parser.error("端口必须为 1–65535，发现等待时间必须大于 0 且不超过 60 秒")
    if args.demo:
        asyncio.run(run_demo())
    elif args.serve or args.discover or args.connect or args.connect_id:
        try:
            from smt_controller.app.network import run_network
        except ModuleNotFoundError as exc:
            parser.error(f"网络依赖未安装：{exc.name}；请安装 'controller[network]'")
        try:
            asyncio.run(run_network(args))
        except KeyboardInterrupt:
            pass
        except (OSError, ValueError, EOFError, TimeoutError, ProtocolError) as exc:
            parser.exit(1, f"连接服务错误：{exc}\n")
    else:
        parser.print_help()
