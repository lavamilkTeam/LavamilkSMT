"""验证目标环境；使用合成图像，不打开相机、不发送机器指令。"""

import argparse
import importlib.metadata
import json
import os
import platform
import statistics
import sys
import time
from pathlib import Path

os.environ.setdefault("OPENBLAS_NUM_THREADS", "1")


def verify(iterations: int) -> dict:
    import cv2
    import numpy as np
    import serial

    cv2.setNumThreads(2)
    frame = np.zeros((720, 1280), dtype=np.uint8)
    cv2.circle(frame, (640, 360), 40, 255, -1)
    timings = []
    for index in range(iterations + 3):
        started = time.perf_counter()
        blurred = cv2.GaussianBlur(frame, (5, 5), 0)
        _, mask = cv2.threshold(blurred, 127, 255, cv2.THRESH_BINARY)
        contours, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
        contour = max(contours, key=cv2.contourArea)
        (x, y), radius = cv2.minEnclosingCircle(contour)
        if index >= 3:
            timings.append((time.perf_counter() - started) * 1000)
    if abs(x - 640) > 1 or abs(y - 360) > 1 or not 38 < radius < 42:
        raise RuntimeError("合成图像测量不符合预期")
    ok, encoded = cv2.imencode(".png", frame)
    if not ok or not np.array_equal(cv2.imdecode(encoded, cv2.IMREAD_GRAYSCALE), frame):
        raise RuntimeError("PNG 编解码验证失败")
    source = np.array([[0, 0], [10, 0], [0, 10], [10, 10]], dtype=np.float32)
    expected = source + np.array([2, 3], dtype=np.float32)
    transform, _ = cv2.estimateAffinePartial2D(source, expected)
    if transform is None or not np.allclose(transform, [[1, 0, 2], [0, 1, 3]], atol=1e-5):
        raise RuntimeError("二维坐标变换验证失败")
    v4l2 = cv2.videoio_registry.hasBackend(cv2.CAP_V4L2)
    if not v4l2:
        raise RuntimeError("OpenCV 未启用 V4L2 后端")
    return {
        "python": sys.version.split()[0],
        "executable": sys.executable,
        "architecture": platform.machine(),
        "numpy": np.__version__,
        "opencv": cv2.__version__,
        "pyserial": serial.__version__,
        "controller": importlib.metadata.version("smt-controller"),
        "v4l2_backend": v4l2,
        "video_devices": [str(p) for p in sorted(Path("/dev").glob("video*"))],
        "synthetic_image": {
            "size": [1280, 720],
            "iterations": iterations,
            "opencv_threads": cv2.getNumThreads(),
            "median_ms": round(statistics.median(timings), 3),
            "max_ms": round(max(timings), 3),
            "center_px": [x, y],
            "png_roundtrip": True,
            "affine_transform": True,
        },
        "limitations": "合成图像仅验证环境；不代表真实相机延迟、定位精度或贴装节拍。",
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--iterations", type=int, default=30)
    args = parser.parse_args()
    if args.iterations < 1:
        parser.error("iterations 必须大于零")
    print(json.dumps(verify(args.iterations), ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
