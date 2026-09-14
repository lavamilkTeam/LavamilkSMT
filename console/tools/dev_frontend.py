"""在控制端通过 noVNC 提供 OpenPnP 开发界面；不实现机器业务 API。"""
import argparse
import fcntl
import os
from pathlib import Path
import secrets
import shutil
import signal
import socket
import string
import subprocess
import sys
import threading
import time


CONSOLE = Path(__file__).resolve().parents[1]
REPO = CONSOLE.parent
TOOL_ROOT = REPO / "toolchains/console-dev/root"


def tool(name):
    local = TOOL_ROOT / "usr/bin" / name
    found = str(local) if local.is_file() else shutil.which(name)
    if not found:
        raise RuntimeError(f"缺少 {name}；请先运行 console/setup-dev-frontend.sh")
    return found


def wait_until(check, children, stopped, description, timeout=15):
    deadline = time.monotonic() + timeout
    while not stopped.is_set():
        for name, process in children:
            if process.poll() is not None:
                raise RuntimeError(f"{name} 已退出（{process.returncode}），请查看开发日志")
        if check():
            return
        if time.monotonic() >= deadline:
            raise RuntimeError(f"等待 {description} 超时")
        stopped.wait(0.1)
    raise InterruptedError("开发模式已停止")


def port_ready(port):
    try:
        with socket.create_connection(("127.0.0.1", port), timeout=0.2):
            return True
    except OSError:
        return False


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--host", default="0.0.0.0", help="HTTP/WebSocket IPv4 监听地址")
    parser.add_argument("--port", type=int, default=6080)
    parser.add_argument("--display", type=int, default=98, help="独立虚拟 X 显示器编号")
    args = parser.parse_args()
    if not 1 <= args.port <= 65535 or not 1 <= args.display <= 999:
        parser.error("端口范围 1–65535，显示器编号范围 1–999")
    state = REPO / "local/console-dev"
    state.mkdir(parents=True, exist_ok=True)
    logs = state / "logs"
    logs.mkdir(exist_ok=True)
    stopped = threading.Event()
    children = []
    handles = []
    lock = None
    owns_lock = False
    try:
        lock = (state / "session.lock").open("a")
        try:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError:
            raise RuntimeError("已有 console 开发模式在运行") from None
        owns_lock = True
        (state / "session.pid").write_text(str(os.getpid()) + "\n")
        display = f":{args.display}"
        x_socket = Path(f"/tmp/.X11-unix/X{args.display}")
        if x_socket.exists() or Path(f"/tmp/.X{args.display}-lock").exists():
            raise RuntimeError(f"显示器 {display} 已被占用；用 --display 选择其他编号")
        # 在创建任何显示/应用进程前检查监听地址。
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
            sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            sock.bind((args.host, args.port))
        xvfb, x11vnc = tool("Xvfb"), tool("x11vnc")
        novnc = TOOL_ROOT / "usr/share/novnc"
        if not (novnc / "vnc.html").is_file():
            novnc = Path("/usr/share/novnc")
        if not (novnc / "vnc.html").is_file():
            raise RuntimeError("缺少 noVNC；请运行 console/setup-dev-frontend.sh")
        if not (CONSOLE / "frontend/target/openpnp-gui-0.0.1-alpha-SNAPSHOT.jar").is_file():
            raise RuntimeError("请先运行 console/build-frontend.sh")
        env = os.environ.copy()
        local_python = TOOL_ROOT / "usr/lib/python3/dist-packages"
        if local_python.is_dir():
            env["PYTHONPATH"] = str(local_python) + (os.pathsep + env["PYTHONPATH"] if env.get("PYTHONPATH") else "")
        libs = list((TOOL_ROOT / "usr/lib").glob("*-linux-gnu"))
        if libs:
            env["LD_LIBRARY_PATH"] = os.pathsep.join(map(str, libs)) + (os.pathsep + env["LD_LIBRARY_PATH"] if env.get("LD_LIBRARY_PATH") else "")
        subprocess.run([sys.executable, "-m", "websockify", "--help"], env=env,
                       stdout=subprocess.DEVNULL, check=True)
        password_file = state / "password"
        if not password_file.exists():
            password = "".join(secrets.choice(string.ascii_letters + string.digits) for _ in range(8))
            fd = os.open(password_file, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
            with os.fdopen(fd, "w") as output:
                output.write(password + "\n")
        password_file.chmod(0o600)
        password = password_file.read_text().strip()
        if len(password) != 8 or not password.isascii() or not password.isalnum():
            raise RuntimeError(f"{password_file} 必须包含 8 位 ASCII 字母或数字")
        env["DISPLAY"] = display
        env["SMT_CONSOLE_CONFIG_DIR"] = str(state / "config")
        for sig in (signal.SIGINT, signal.SIGTERM):
            signal.signal(sig, lambda *_: stopped.set())

        def start(name, command):
            handle = (logs / f"{name}.log").open("w")
            handles.append(handle)
            process = subprocess.Popen(command, env=env, cwd=CONSOLE,
                                       stdout=handle, stderr=subprocess.STDOUT, start_new_session=True)
            children.append((name, process))

        start("display", [xvfb, display, "-screen", "0", "1280x800x24", "-nolisten", "tcp", "-ac"])
        wait_until(x_socket.exists, children, stopped, "虚拟显示器")
        with socket.socket() as sock:
            sock.bind(("127.0.0.1", 0))
            vnc_port = sock.getsockname()[1]
        start("vnc", [x11vnc, "-norc", "-display", display, "-localhost", "-no6",
                      "-rfbport", str(vnc_port), "-passwdfile", str(password_file),
                      "-forever", "-shared", "-noxdamage", "-nolookup"])
        wait_until(lambda: port_ready(vnc_port), children, stopped, "VNC 服务")
        start("frontend", [str(CONSOLE / "run-frontend.sh")])
        start("web", [sys.executable, "-m", "websockify", "--web", str(novnc),
                      f"{args.host}:{args.port}", f"127.0.0.1:{vnc_port}"])
        # host 可以限定其他网卡，此时用其实际地址检测。
        def web_ready():
            host = "127.0.0.1" if args.host == "0.0.0.0" else args.host
            try:
                with socket.create_connection((host, args.port), timeout=0.2):
                    return True
            except OSError:
                return False
        wait_until(web_ready, children, stopped, "浏览器入口")
        print(f"OpenPnP 开发模式监听 {args.host}:{args.port}", flush=True)
        print(f"本机访问：http://{'127.0.0.1' if args.host == '0.0.0.0' else args.host}:{args.port}/vnc.html?autoconnect=true&resize=scale", flush=True)
        print(f"局域网访问时将地址换为控制端 IP。密码文件：{password_file}", flush=True)
        print(f"日志：{logs}；Ctrl+C 停止。Java 修改后重新构建并重启，没有 Web 热更新。", flush=True)
        while not stopped.wait(0.5):
            for name, process in children:
                if process.poll() is not None:
                    raise RuntimeError(f"{name} 已退出（{process.returncode}），请查看 {logs}")
    except InterruptedError:
        pass
    except (OSError, RuntimeError, subprocess.SubprocessError) as exc:
        print(f"开发模式启动/运行失败：{exc}", file=sys.stderr)
        return 1
    finally:
        for _, process in reversed(children):
            # 包括 websockify 为客户端创建的子进程。
            try:
                os.killpg(process.pid, signal.SIGTERM)
            except ProcessLookupError:
                pass
        for _, process in reversed(children):
            try:
                process.wait(timeout=3)
            except subprocess.TimeoutExpired:
                try:
                    os.killpg(process.pid, signal.SIGKILL)
                except ProcessLookupError:
                    pass
                process.wait()
        for handle in handles:
            handle.close()
        if owns_lock:
            (state / "session.pid").unlink(missing_ok=True)
        if lock is not None:
            lock.close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
