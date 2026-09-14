import argparse
import logging
import signal
import threading
from pathlib import Path

from smt_controller.preview.frames import LatestFrames
from smt_controller.preview.http import PreviewHTTPServer


def main() -> None:
    parser = argparse.ArgumentParser(description="OpenCV 灰度/二值化/轮廓图像的局域网 MJPEG 预览")
    parser.add_argument("--device", default="/dev/video0", help="优先使用 /dev/v4l/by-id 的稳定路径")
    parser.add_argument("--host", default="0.0.0.0")
    parser.add_argument("--port", type=int, default=8766)
    parser.add_argument("--width", type=int, default=640)
    parser.add_argument("--height", type=int, default=480)
    parser.add_argument("--fps", type=float, default=10, help="处理输出帧率上限，实际速度取决于硬件")
    parser.add_argument("--quality", type=int, default=75, help="JPEG 质量，1–100")
    parser.add_argument("--lock-file", type=Path, default=Path("/tmp/smt-preview-camera.lock"))
    args = parser.parse_args()
    if not 1 <= args.port <= 65535 or not 1 <= args.fps <= 30 or not 1 <= args.quality <= 100:
        parser.error("端口范围 1–65535，帧率 1–30，JPEG 质量 1–100")
    if not 160 <= args.width <= 1920 or not 120 <= args.height <= 1080:
        parser.error("预览宽度范围 160–1920，高度范围 120–1080")
    try:
        from smt_controller.preview.capture import CaptureWorker
    except ModuleNotFoundError as exc:
        parser.error(f"缺少视觉依赖 {exc.name}；请先准备 NumPy/OpenCV 环境")
    logging.basicConfig(level=logging.INFO)
    frames = LatestFrames()
    worker = CaptureWorker(frames, device=args.device, width=args.width, height=args.height,
                           fps=args.fps, quality=args.quality, lock_file=args.lock_file)
    try:
        server = PreviewHTTPServer((args.host, args.port), frames, device=args.device)
    except OSError as exc:
        parser.exit(1, f"无法监听预览端口：{exc}\n")
    stop = threading.Event()
    old_handlers = {sig: signal.signal(sig, lambda *_: stop.set()) for sig in (signal.SIGINT, signal.SIGTERM)}
    http_thread = threading.Thread(target=server.serve_forever, kwargs={"poll_interval": 0.2}, daemon=True)
    try:
        worker.start()
        http_thread.start()
        logging.info("预览服务已启动：http://%s:%d/stream/contours.mjpg", args.host, server.server_port)
        while not stop.wait(0.5):
            if not worker.is_alive() or not http_thread.is_alive():
                raise RuntimeError("预览工作线程已退出")
    finally:
        worker.stop()
        frames.close()
        server.shutdown()
        server.server_close()
        http_thread.join(timeout=2)
        worker.join(timeout=2)
        for sig, old in old_handlers.items():
            signal.signal(sig, old)


if __name__ == "__main__":
    main()
