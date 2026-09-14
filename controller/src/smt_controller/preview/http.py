import json
import logging
import socket
import threading
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlsplit

from smt_controller.preview.frames import LatestFrames, MODES

logger = logging.getLogger(__name__)


class PreviewHTTPServer(ThreadingHTTPServer):
    daemon_threads = True
    allow_reuse_address = True
    request_queue_size = 8

    def __init__(self, address, frames: LatestFrames, *, device: str, max_streams: int = 4):
        self.frames = frames
        self.device = device
        self.stream_slots = threading.BoundedSemaphore(max_streams)
        self._request_slots = threading.BoundedSemaphore(max_streams + 4)
        super().__init__(address, PreviewHandler)

    def get_request(self):
        request, address = super().get_request()
        request.settimeout(3)
        # 限制慢客户端在内核发送队列里积压的数据。
        request.setsockopt(socket.SOL_SOCKET, socket.SO_SNDBUF, 32768)
        request.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
        return request, address

    def process_request(self, request, client_address):
        if not self._request_slots.acquire(blocking=False):
            self.shutdown_request(request)
            return
        try:
            super().process_request(request, client_address)
        except BaseException:
            self._request_slots.release()
            raise

    def process_request_thread(self, request, client_address):
        try:
            super().process_request_thread(request, client_address)
        finally:
            self._request_slots.release()


class PreviewHandler(BaseHTTPRequestHandler):
    server_version = "SMTPreview/1"
    sys_version = ""

    def do_GET(self):
        try:
            self._get()
        except (OSError, TimeoutError):
            pass  # 客户端关闭或过慢，不影响相机和其他客户端。
        finally:
            self.close_connection = True

    def _get(self):
        try:
            path = urlsplit(self.path).path
        except ValueError:
            self.send_error(400, "Invalid URL")
            return
        if path == "/":
            self.send_response(302)
            self.send_header("Location", "/stream/contours.mjpg")
            self.send_header("Content-Length", "0")
            self.end_headers()
            return
        if path == "/status.json":
            body = {"service": "smt-preview", "device": self.server.device,
                    "modes": list(MODES), "motion_enabled": False,
                    "streams": {m: f"/stream/{m}.mjpg" for m in MODES}, **self.server.frames.status()}
            self._response(200, "application/json", json.dumps(body, ensure_ascii=False).encode())
            return
        parts = path.strip("/").split("/")
        if len(parts) != 2 or parts[0] not in ("stream", "snapshot"):
            self.send_error(404)
            return
        suffix = ".mjpg" if parts[0] == "stream" else ".jpg"
        mode = parts[1].removesuffix(suffix)
        if not parts[1].endswith(suffix) or mode not in MODES:
            self.send_error(404)
            return
        if parts[0] == "snapshot":
            frame = self.server.frames.wait_after(0)
            if frame is None:
                self._response(503, "application/json", b'{"error":"camera unavailable or frame stale"}')
            else:
                self._response(200, "image/jpeg", frame.images[mode], frame=frame)
            return
        if not self.server.stream_slots.acquire(blocking=False):
            self._response(503, "application/json", b'{"error":"too many preview clients"}')
            return
        try:
            frame = self.server.frames.wait_after(0)
            if frame is None:
                self._response(503, "application/json", b'{"error":"camera unavailable or frame stale"}')
                return
            self.send_response(200)
            self.send_header("Content-Type", "multipart/x-mixed-replace; boundary=smt-frame")
            self.send_header("Cache-Control", "no-store, no-cache, must-revalidate")
            self.send_header("X-Accel-Buffering", "no")
            self.send_header("Connection", "close")
            self.end_headers()
            while frame is not None:
                jpeg = frame.images[mode]
                header = (f"--smt-frame\r\nContent-Type: image/jpeg\r\nContent-Length: {len(jpeg)}\r\n"
                          f"X-Frame-Sequence: {frame.sequence}\r\n"
                          f"X-Received-At: {frame.received_at:.6f}\r\n\r\n").encode("ascii")
                self.wfile.write(header + jpeg + b"\r\n")
                self.wfile.flush()
                frame = self.server.frames.wait_after(frame.sequence)
            self.wfile.write(b"--smt-frame--\r\n")
        finally:
            self.server.stream_slots.release()

    def _response(self, code, content_type, body, *, frame=None):
        self.send_response(code)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        if frame is not None:
            self.send_header("X-Frame-Sequence", str(frame.sequence))
            self.send_header("X-Received-At", str(frame.received_at))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, format, *args):
        logger.debug("%s %s", self.client_address[0], format % args)
