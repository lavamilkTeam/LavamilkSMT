"""Exercise the Swing client -> real Python TCP server -> real Go Unix-socket server.

Run after building mcu-updater and running Maven's McuUpdateClientTest. No hardware is used.
"""
import argparse
import os
from pathlib import Path
import select
import shutil
import subprocess
import sys
import tempfile
import time


ROOT = Path(__file__).resolve().parents[1]
CONTROLLER = """
import asyncio
from pathlib import Path
import sys
from smt_controller.connectivity.identity import load_identity
from smt_controller.connectivity.server import ConnectionServer
from smt_controller.firmware_update.client import UpdaterClient
from smt_controller.firmware_update.service import MCUUpdateService

async def main():
    updates = MCUUpdateService(UpdaterClient(Path(sys.argv[1])))
    server = ConnectionServer(load_identity(Path(sys.argv[2])),
        lambda: {"machine": "disconnected"}, update_handler=updates.handle)
    await server.start("127.0.0.1", 0)
    print(server.port, flush=True)
    try:
        await asyncio.Event().wait()
    finally:
        await server.close()
asyncio.run(main())
"""


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--updater", type=Path, default=ROOT / "artifacts/smt-mcu-updater")
    parser.add_argument("--java", default=shutil.which("java") or str(ROOT / "toolchains/openpnp/jdk/bin/java"))
    args = parser.parse_args()
    children = []
    with tempfile.TemporaryDirectory(prefix="smt-link-") as scratch:
        directory = Path(scratch)
        socket = directory / "updater.sock"
        with (directory / "processes.log").open("w+") as log:
            try:
                go = subprocess.Popen([str(args.updater.resolve()), "--socket", str(socket),
                    "--state-dir", str(directory / "go")], stdout=log, stderr=log)
                children.append(go)
                deadline = time.monotonic() + 10
                while not socket.exists():
                    if go.poll() is not None or time.monotonic() >= deadline:
                        raise RuntimeError("Go updater did not start")
                    time.sleep(0.05)
                env = dict(os.environ, PYTHONPATH=str(ROOT / "controller/src"))
                python = subprocess.Popen([sys.executable, "-c", CONTROLLER, str(socket),
                    str(directory / "identity")], stdout=subprocess.PIPE, stderr=log, text=True, env=env)
                children.append(python)
                if not select.select([python.stdout], [], [], 10)[0]:
                    raise RuntimeError("Python controller did not start")
                port = int(python.stdout.readline().strip())
                frontend = ROOT / "console/frontend/target"
                classpath = os.pathsep.join(str(frontend / path) for path in ("test-classes", "classes", "lib/*"))
                subprocess.run([args.java, "-Djava.awt.headless=true", "-cp", classpath,
                    "McuUpdateClientTest", str(port)], timeout=25, check=True)
                print("PASS: Java -> Python -> Go; real maintenance unavailable, flashing blocked")
            except BaseException:
                log.flush()
                log.seek(0)
                print(log.read(), file=sys.stderr)
                raise
            finally:
                for child in reversed(children):
                    if child.poll() is None:
                        child.terminate()
                    try:
                        child.wait(timeout=15)
                    except subprocess.TimeoutExpired:
                        child.kill()
                        child.wait()


if __name__ == "__main__":
    main()
