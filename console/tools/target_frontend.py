"""在目标板运行 OpenPnP 和带密码的 noVNC 入口，由 systemd 管理生命周期。"""
import os
from pathlib import Path
import signal
import socket
import subprocess
import threading
import time


APP = Path(__file__).resolve().parents[1]
STATE = Path(os.environ.get("SMT_CONSOLE_STATE", "/var/lib/smt-console"))
HTTP_PORT = int(os.environ.get("SMT_CONSOLE_PORT", "6080"))
VNC_PORT = 5901
DISPLAY = ":99"


def main():
    stopped = threading.Event()
    children = []
    for sig in (signal.SIGINT, signal.SIGTERM):
        signal.signal(sig, lambda *_: stopped.set())
    env = os.environ.copy()
    env["DISPLAY"] = DISPLAY
    password = STATE / "password"
    if not password.is_file():
        raise RuntimeError("缺少 /var/lib/smt-console/password")
    if len(password.read_text().strip().encode("ascii")) != 8:
        raise RuntimeError("VNC 密码必须为 8 位 ASCII 字符")
    if Path("/tmp/.X99-lock").exists() or Path("/tmp/.X11-unix/X99").exists():
        raise RuntimeError("虚拟显示器 :99 已被占用")
    for host, port in (("0.0.0.0", HTTP_PORT), ("127.0.0.1", VNC_PORT)):
        with socket.socket() as sock:
            sock.bind((host, port))

    def start(name, args):
        process = subprocess.Popen(args, cwd=APP / "frontend", env=env)
        children.append((name, process))

    def wait_for(check, name, timeout=20):
        deadline = time.monotonic() + timeout
        while not stopped.is_set():
            for child_name, process in children:
                if process.poll() is not None:
                    raise RuntimeError("{} 提前退出：{}".format(child_name, process.returncode))
            if check():
                return
            if time.monotonic() >= deadline:
                raise RuntimeError("等待 {} 超时".format(name))
            stopped.wait(0.1)
        raise InterruptedError()

    def listening(port):
        try:
            with socket.create_connection(("127.0.0.1", port), timeout=0.2):
                return True
        except OSError:
            return False

    try:
        start("Xvfb", ["/usr/bin/Xvfb", DISPLAY, "-screen", "0", "1280x800x24",
                       "-nolisten", "tcp", "-ac"])
        wait_for(lambda: Path("/tmp/.X11-unix/X99").exists(), "虚拟显示器")
        start("VNC", ["/usr/bin/x11vnc", "-norc", "-display", DISPLAY, "-localhost",
                      "-no6", "-rfbport", str(VNC_PORT), "-passwdfile", "read:" + str(password),
                      "-forever", "-shared", "-noxdamage", "-nolookup", "-quiet"])
        wait_for(lambda: listening(VNC_PORT), "VNC")
        start("OpenPnP", ["/usr/bin/java", "-Xms64m", "-Xmx512m", "-XX:ActiveProcessorCount=2",
                          "-Duser.language=zh", "-Duser.country=CN",
                          "-DconfigDir=" + str(STATE / "config"),
                          "--add-opens=java.base/java.lang=ALL-UNNAMED",
                          "--add-opens=java.desktop/java.awt=ALL-UNNAMED",
                          "--add-opens=java.desktop/java.awt.color=ALL-UNNAMED",
                          "-jar", str(APP / "frontend/target/openpnp-gui-0.0.1-alpha-SNAPSHOT.jar")])
        start("noVNC", ["/usr/bin/python3", "-m", "websockify", "--web", "/usr/share/novnc",
                        "0.0.0.0:" + str(HTTP_PORT), "127.0.0.1:" + str(VNC_PORT)])
        wait_for(lambda: listening(HTTP_PORT), "浏览器入口")
        print("OpenPnP 浏览器入口已启动，端口 {}".format(HTTP_PORT), flush=True)
        while not stopped.wait(1):
            for name, process in children:
                if process.poll() is not None:
                    raise RuntimeError("{} 已退出：{}".format(name, process.returncode))
    except InterruptedError:
        pass
    finally:
        for _, process in reversed(children):
            if process.poll() is None:
                process.terminate()
        for _, process in reversed(children):
            try:
                process.wait(timeout=3)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait()


if __name__ == "__main__":
    main()
