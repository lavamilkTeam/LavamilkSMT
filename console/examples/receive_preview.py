"""接收处理后的画面；不依赖 OpenCV，不打开 GUI。"""
import argparse
import time
from contextlib import closing
from pathlib import Path

from smt_controller.preview.client import iter_preview


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--host", required=True, help="从设备发现结果选择可达的 IPv4 地址")
    parser.add_argument("--port", type=int, default=8766)
    parser.add_argument("--mode", choices=("gray", "threshold", "contours"), default="contours")
    parser.add_argument("--frames", type=int, default=30)
    parser.add_argument("--output", type=Path, help="可选：将最后一帧保存为 JPEG")
    args = parser.parse_args()
    if args.frames < 1 or not 1 <= args.port <= 65535:
        parser.error("帧数必须大于零，端口范围 1–65535")
    url = f"http://{args.host}:{args.port}/stream/{args.mode}.mjpg"
    started = time.monotonic()
    count = 0
    with closing(iter_preview(url)) as stream:
        for frame in stream:
            count += 1
            print(f"frame={frame.sequence} jpeg_bytes={len(frame.jpeg)}", flush=True)
            if count >= args.frames:
                if args.output:
                    args.output.parent.mkdir(parents=True, exist_ok=True)
                    args.output.write_bytes(frame.jpeg)
                break
    if count < args.frames:
        raise SystemExit("预览流已结束；检查相机状态后重新连接")
    print(f"received={count}, elapsed={time.monotonic() - started:.2f}s")


if __name__ == "__main__":
    main()
