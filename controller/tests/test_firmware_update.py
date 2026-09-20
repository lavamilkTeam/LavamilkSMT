import asyncio
import tempfile
import unittest
from pathlib import Path

from smt_controller.connectivity.client import ControllerClient
from smt_controller.connectivity.identity import load_identity
from smt_controller.connectivity.protocol import ProtocolError, read_message, write_message, close_writer
from smt_controller.connectivity.server import ConnectionServer
from smt_controller.firmware_update.client import UpdaterClient
from smt_controller.firmware_update.service import MCUUpdateService


class FirmwareUpdateTests(unittest.IsolatedAsyncioTestCase):
    async def asyncSetUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        self.socket = self.root / "updater.sock"
        self.calls = []
        self.wrong_id = False
        self.result = {"can_update": True, "blockers": [], "task": None}

        async def handle(reader, writer):
            try:
                request = await read_message(reader, 1)
                self.calls.append(request)
                await write_message(writer, {"protocol_version": 1,
                    "request_id": "wrong" if self.wrong_id else request["request_id"],
                    "result": self.result})
            finally:
                await close_writer(writer)

        self.go = await asyncio.start_unix_server(handle, str(self.socket))
        self.updates = MCUUpdateService(UpdaterClient(self.socket, timeout=1))
        self.server = ConnectionServer(load_identity(self.root / "identity"),
            lambda: {"machine": "disconnected"}, update_handler=self.updates.handle)
        await self.server.start("127.0.0.1", 0)

    async def asyncTearDown(self):
        await self.server.close()
        self.go.close()
        await self.go.wait_closed()
        self.tmp.cleanup()

    async def test_console_query_reaches_updater_but_readiness_cannot_grant_maintenance(self):
        async with await ControllerClient.connect("127.0.0.1", self.server.port) as client:
            response = await client.request("check_mcu_update")
            self.assertEqual(self.calls[0]["type"], "check")
            self.assertFalse(response["update"]["can_update"])
            self.assertEqual(response["update"]["code"], "MAINTENANCE_UNAVAILABLE")
            self.assertEqual((await client.request("get_status"))["status"]["machine"], "disconnected")

    async def test_start_is_rejected_without_sending_a_flash_request(self):
        async with await ControllerClient.connect("127.0.0.1", self.server.port) as client:
            response = await client.request("start_mcu_update", version="v1", outputs_safe=True)
            self.assertFalse(response["update"]["can_update"])
            self.assertEqual(self.calls, [])
            self.assertFalse((self.root / "permit.json").exists())

    async def test_unavailable_updater_preserves_status_connection(self):
        self.updates.updater = UpdaterClient(self.root / "missing.sock", timeout=1)
        async with await ControllerClient.connect("127.0.0.1", self.server.port) as client:
            response = await client.request("check_mcu_update")
            self.assertEqual(response["update"]["code"], "UPDATER_UNAVAILABLE")
            self.assertEqual((await client.request("ping"))["type"], "pong")

    async def test_mismatched_response_is_rejected(self):
        self.wrong_id = True
        with self.assertRaises(ProtocolError):
            await self.updates.updater.request("check")

    async def test_malformed_readiness_is_not_forwarded_as_permission(self):
        self.result["blockers"] = "not a list"
        result = await self.updates.handle("check_mcu_update")
        self.assertFalse(result["can_update"])
        self.assertEqual(result["code"], "UPDATER_INVALID_RESPONSE")
