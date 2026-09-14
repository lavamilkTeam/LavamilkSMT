import fcntl
import logging
import threading
import time
from pathlib import Path

import cv2

from smt_controller.preview.frames import LatestFrames
from smt_controller.preview.processing import PreviewProcessor

logger = logging.getLogger(__name__)


class CaptureWorker(threading.Thread):
    """单一相机所有者；采集、处理、编码在一个工作线程内完成。"""

    def __init__(self, frames: LatestFrames, *, device: str, width: int, height: int,
                 fps: float, quality: int, lock_file: Path, processor=None):
        super().__init__(name="preview-capture", daemon=True)
        self.frames, self.device = frames, device
        self.width, self.height, self.fps, self.quality = width, height, fps, quality
        self.lock_file = lock_file
        self.processor = processor if processor is not None else PreviewProcessor()
        self.stop_requested = threading.Event()

    def run(self) -> None:
        cv2.setNumThreads(2)
        self.lock_file.parent.mkdir(parents=True, exist_ok=True)
        # 此锁协调预览实例；其他程序也必须遵守单一相机所有者约定。
        with self.lock_file.open("a") as lock:
            try:
                fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
            except BlockingIOError:
                self.frames.fail("camera is owned by another preview process")
                return
            last_error = None
            while not self.stop_requested.is_set():
                cap = None
                try:
                    cap = cv2.VideoCapture(self.device, cv2.CAP_V4L2)
                    if not cap.isOpened():
                        raise RuntimeError("cannot open camera; check device, video group and camera ownership")
                    for prop, value in ((cv2.CAP_PROP_FOURCC, cv2.VideoWriter_fourcc(*"MJPG")),
                                        (cv2.CAP_PROP_FRAME_WIDTH, self.width),
                                        (cv2.CAP_PROP_FRAME_HEIGHT, self.height)):
                        if not cap.set(prop, value):
                            raise RuntimeError("camera rejected MJPEG/resolution settings")
                    cap.set(cv2.CAP_PROP_FPS, 30)
                    cap.set(cv2.CAP_PROP_BUFFERSIZE, 1)
                    # 丢弃启动阶段的若干帧；这不构成停稳后曝光保证。
                    for _ in range(5):
                        if self.stop_requested.is_set():
                            return
                        if not cap.grab():
                            raise RuntimeError("camera warmup failed")
                    next_output = 0.0
                    previous_output = None
                    while not self.stop_requested.is_set():
                        started = time.monotonic()
                        if not cap.grab():
                            raise RuntimeError("camera capture failed")
                        grabbed = time.monotonic()
                        if grabbed < next_output:
                            continue  # 消费采集缓存，但跳过不需展示的 JPEG 解码。
                        ok, raw = cap.retrieve()
                        decoded = time.monotonic()
                        received_at = time.time()
                        if not ok or raw is None:
                            raise RuntimeError("camera decode failed")
                        processed = self.processor.process(raw)
                        processed_at = time.monotonic()
                        encoded = {}
                        for mode, image in processed.items():
                            ok, jpeg = cv2.imencode(".jpg", image, [cv2.IMWRITE_JPEG_QUALITY, self.quality])
                            if not ok:
                                raise RuntimeError(f"JPEG encoding failed: {mode}")
                            encoded[mode] = jpeg.tobytes()
                        finished = time.monotonic()
                        metrics = {
                            "grab_ms": round((grabbed - started) * 1000, 2),
                            "decode_ms": round((decoded - grabbed) * 1000, 2),
                            "process_ms": round((processed_at - decoded) * 1000, 2),
                            "encode_three_views_ms": round((finished - processed_at) * 1000, 2),
                            "output_fps": round(1 / (finished - previous_output), 2) if previous_output else 0,
                        }
                        self.frames.publish(received_at=received_at, width=raw.shape[1], height=raw.shape[0],
                                            images=encoded, metrics=metrics)
                        previous_output = finished
                        next_output = grabbed + 1 / self.fps
                        last_error = None
                except Exception as exc:
                    error = str(exc)
                    self.frames.fail(error)
                    if error != last_error:
                        logger.warning("preview camera unavailable: %s", error)
                    last_error = error
                finally:
                    if cap is not None:
                        cap.release()
                self.stop_requested.wait(2)

    def stop(self) -> None:
        self.stop_requested.set()
