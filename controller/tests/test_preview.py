import json
import threading
import time
import unittest
from contextlib import closing
from urllib.error import HTTPError
from urllib.request import urlopen

from smt_controller.preview.client import iter_preview
from smt_controller.preview.frames import LatestFrames, MODES
from smt_controller.preview.http import PreviewHTTPServer


def publish(frames, content=b"example-jpeg"):
    frames.publish(received_at=time.time(), width=640, height=480,
                   images={mode: content for mode in MODES}, metrics={"output_fps": 10})


class FrameStoreTests(unittest.TestCase):
    def test_slow_reader_gets_latest_frame_without_queue(self):
        frames = LatestFrames()
        publish(frames, b"first")
        first = frames.wait_after(0)
        for index in range(10):
            publish(frames, str(index).encode())
        latest = frames.wait_after(first.sequence)
        self.assertEqual(latest.sequence, 11)
        self.assertEqual(latest.images["gray"], b"9")
        with self.assertRaises(TypeError):
            latest.images["gray"] = b"mutated"

    def test_offline_discards_old_image_and_reconnect_keeps_sequence(self):
        frames = LatestFrames()
        publish(frames)
        frames.fail("camera unplugged")
        self.assertFalse(frames.status()["ready"])
        self.assertIsNone(frames.wait_after(0, timeout=0))
        publish(frames, b"new")
        self.assertEqual(frames.wait_after(0).sequence, 2)

    def test_stale_frame_not_served_and_close_wakes_waiter(self):
        frames = LatestFrames(max_age=0.01)
        publish(frames)
        time.sleep(0.02)
        self.assertFalse(frames.status()["ready"])
        self.assertIsNone(frames.wait_after(0, timeout=0))
        finished = threading.Event()
        thread = threading.Thread(target=lambda: (frames.wait_after(99, timeout=10), finished.set()))
        thread.start()
        frames.close()
        self.assertTrue(finished.wait(1))
        thread.join()


class PreviewHTTPTests(unittest.TestCase):
    def setUp(self):
        self.frames = LatestFrames()
        self.server = PreviewHTTPServer(("127.0.0.1", 0), self.frames, device="test-camera", max_streams=2)
        self.thread = threading.Thread(target=self.server.serve_forever, kwargs={"poll_interval": 0.01})
        self.thread.start()
        self.base = f"http://127.0.0.1:{self.server.server_port}"

    def tearDown(self):
        self.frames.close()
        self.server.shutdown()
        self.server.server_close()
        self.thread.join(timeout=2)

    def test_processed_snapshots_match_their_mode_and_status_has_no_motion(self):
        self.frames.publish(received_at=time.time(), width=640, height=480,
                            images={mode: mode.encode() for mode in MODES}, metrics={})
        for mode in MODES:
            with urlopen(f"{self.base}/snapshot/{mode}.jpg", timeout=2) as response:
                self.assertEqual(response.read(), mode.encode())
                self.assertEqual(response.headers["Content-Type"], "image/jpeg")
                self.assertEqual(response.headers["X-Frame-Sequence"], "1")
        with urlopen(f"{self.base}/status.json", timeout=2) as response:
            status = json.load(response)
        self.assertTrue(status["ready"])
        self.assertFalse(status["motion_enabled"])

    def test_two_streams_share_frames_and_one_disconnect_does_not_stop_other(self):
        publish(self.frames)
        with closing(iter_preview(f"{self.base}/stream/gray.mjpg")) as first, \
                closing(iter_preview(f"{self.base}/stream/threshold.mjpg")) as second:
            self.assertEqual(next(first).sequence, 1)
            self.assertEqual(next(second).sequence, 1)
            first.close()
            publish(self.frames, b"second")
            reply = next(second)
            self.assertEqual(reply.sequence, 2)
            self.assertEqual(reply.jpeg, b"second")

    def test_stream_limit_and_unknown_route(self):
        publish(self.frames)
        with closing(iter_preview(f"{self.base}/stream/gray.mjpg")) as first, \
                closing(iter_preview(f"{self.base}/stream/contours.mjpg")) as second:
            next(first)
            next(second)
            with self.assertRaises(HTTPError) as exc:
                urlopen(f"{self.base}/stream/gray.mjpg", timeout=2)
            self.assertEqual(exc.exception.code, 503)
            exc.exception.close()
        with self.assertRaises(HTTPError) as exc:
            urlopen(f"{self.base}/../../etc/passwd", timeout=2)
        self.assertEqual(exc.exception.code, 404)
        exc.exception.close()

    def test_offline_snapshot_is_503_and_active_stream_ends(self):
        publish(self.frames)
        with closing(iter_preview(f"{self.base}/stream/gray.mjpg", timeout=5)) as stream:
            next(stream)
            self.frames.fail("camera unplugged")
            with self.assertRaises(StopIteration):
                next(stream)
        # 关闭缓存会立即唤醒等待，仍然不能把旧帧作为可用快照返回。
        self.frames.close()
        with self.assertRaises(HTTPError) as exc:
            urlopen(f"{self.base}/snapshot/gray.jpg", timeout=2)
        self.assertEqual(exc.exception.code, 503)
        exc.exception.close()


try:
    import cv2
    import numpy as np
    from smt_controller.preview.processing import PreviewProcessor
except ModuleNotFoundError:
    PreviewProcessor = None


@unittest.skipIf(PreviewProcessor is None, "需准备 NumPy/OpenCV 才能验证图像处理")
class ProcessingTests(unittest.TestCase):
    def test_gray_binary_and_contour_outputs_preserve_source(self):
        image = np.zeros((120, 160, 3), dtype=np.uint8)
        cv2.rectangle(image, (30, 25), (100, 90), (255, 255, 255), -1)
        original = image.copy()
        results = PreviewProcessor().process(image)
        self.assertTrue(np.array_equal(image, original))
        self.assertEqual(results["gray"].shape, (120, 160))
        self.assertEqual(set(np.unique(results["threshold"]).tolist()), {0, 255})
        green = results["contours"][:, :, 1] > results["contours"][:, :, 0]
        self.assertGreater(int(green.sum()), 20)
        for result in results.values():
            ok, jpeg = cv2.imencode(".jpg", result)
            self.assertTrue(ok)
            self.assertEqual(cv2.imdecode(jpeg, cv2.IMREAD_COLOR).shape, image.shape)
