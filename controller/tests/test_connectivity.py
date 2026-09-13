import asyncio
import json
import tempfile
import unittest
from pathlib import Path
from uuid import uuid4
from types import SimpleNamespace
from unittest.mock import patch

from smt_controller.connectivity.client import ControllerClient
from smt_controller.connectivity.identity import load_identity
from smt_controller.connectivity.protocol import ProtocolError, read_message, write_message
from smt_controller.connectivity.server import ConnectionServer


class IdentityTests(unittest.TestCase):
    def test_identity_survives_restart_and_rename(self):
        with tempfile.TemporaryDirectory() as directory:
            original = load_identity(Path(directory), "第一台")
            reloaded = load_identity(Path(directory), "改名")
            self.assertEqual(original.device_id, reloaded.device_id)
            self.assertEqual(reloaded.name, "改名")

    def test_corrupt_identity_is_not_silently_replaced(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "device-id"
            path.write_text("broken")
            with self.assertRaises(ValueError):
                load_identity(Path(directory))
            self.assertEqual(path.read_text(), "broken")


class ConnectionTests(unittest.IsolatedAsyncioTestCase):
    async def asyncSetUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.identity = load_identity(Path(self.directory.name))
        self.status = {"machine": "disconnected", "motion_enabled": False}
        self.server = ConnectionServer(self.identity, lambda: self.status, idle_timeout=0.2)
        await self.server.start("127.0.0.1", 0)
        self.writers = []

    async def asyncTearDown(self):
        await self.server.close()
        for writer in self.writers:
            writer.close()
            await writer.wait_closed()
        self.directory.cleanup()

    async def raw(self):
        reader, writer = await asyncio.open_connection("127.0.0.1", self.server.port)
        self.writers.append(writer)
        return reader, writer

    async def test_two_clients_have_distinct_sessions_and_same_status(self):
        a = await ControllerClient.connect("127.0.0.1", self.server.port, expected_device_id=self.identity.device_id)
        b = await ControllerClient.connect("127.0.0.1", self.server.port)
        async with a, b:
            self.assertNotEqual(a.hello["session_id"], b.hello["session_id"])
            self.assertEqual((await a.request("get_status"))["status"], self.status)
            self.assertEqual((await b.request("ping"))["type"], "pong")
            await a.close()
            self.assertEqual((await b.request("get_status"))["status"], self.status)

    async def test_wrong_identity_and_version_rejected(self):
        with self.assertRaises(ProtocolError):
            await ControllerClient.connect("127.0.0.1", self.server.port, expected_device_id=str(uuid4()))
        reader, writer = await self.raw()
        await write_message(writer, {"type": "hello", "request_id": "1", "protocol_version": 2})
        self.assertEqual((await read_message(reader, 1))["error"]["code"], "PROTOCOL_MISMATCH")
        self.assertEqual(await reader.read(), b"")

    async def test_handshake_required(self):
        reader, writer = await self.raw()
        await write_message(writer, {"type": "get_status", "request_id": "1"})
        self.assertEqual((await read_message(reader, 1))["error"]["code"], "HELLO_REQUIRED")

    async def test_fragmented_and_coalesced_tcp_frames(self):
        reader, writer = await self.raw()
        hello = json.dumps({"type": "hello", "request_id": "1", "protocol_version": 1}).encode() + b"\n"
        writer.write(hello[:10])
        await writer.drain()
        await asyncio.sleep(0)
        writer.write(hello[10:] + b'{"type":"ping","request_id":"2"}\n{"type":"get_status","request_id":"3"}\n')
        await writer.drain()
        self.assertEqual((await read_message(reader, 1))["type"], "hello")
        self.assertEqual((await read_message(reader, 1))["type"], "pong")
        self.assertEqual((await read_message(reader, 1))["status"], self.status)

    async def test_malformed_and_oversized_input_is_closed(self):
        for payload in (b"not-json\n", b"[]\n", b"x" * 9000, b'{"request_id":null}\n'):
            reader, writer = await self.raw()
            writer.write(payload)
            await writer.drain()
            self.assertEqual((await read_message(reader, 1))["error"]["code"], "INVALID_MESSAGE")
            self.assertEqual(await reader.read(), b"")

    async def test_motion_request_cannot_execute(self):
        client = await ControllerClient.connect("127.0.0.1", self.server.port)
        async with client:
            with self.assertRaisesRegex(ProtocolError, "UNSUPPORTED_REQUEST"):
                await client.request("move", x=100)
        self.assertEqual(self.status, {"machine": "disconnected", "motion_enabled": False})

    async def test_idle_connection_expires_without_changing_machine(self):
        reader, writer = await self.raw()
        await write_message(writer, {"type": "hello", "request_id": "1", "protocol_version": 1})
        await read_message(reader, 1)
        self.assertEqual(await asyncio.wait_for(reader.read(), 1), b"")
        self.assertFalse(self.status["motion_enabled"])

    async def test_shutdown_closes_existing_connections(self):
        reader, _ = await self.raw()
        await self.server.close()
        self.assertEqual(await asyncio.wait_for(reader.read(), 1), b"")


try:
    from smt_controller.connectivity.discovery import multicast_interfaces, parse_service, service_info
except ModuleNotFoundError:
    parse_service = None


@unittest.skipIf(parse_service is None, "需安装 controller[network] 才能测试 mDNS 描述")
class DiscoveryTests(unittest.TestCase):
    def test_same_nic_aliases_use_one_multicast_membership(self):
        adapters = [SimpleNamespace(ips=[SimpleNamespace(ip="192.168.1.100"),
                                         SimpleNamespace(ip="192.168.2.7")]),
                    SimpleNamespace(ips=[SimpleNamespace(ip="10.0.0.2")])]
        with patch("smt_controller.connectivity.discovery.ifaddr.get_adapters", return_value=adapters):
            self.assertEqual(multicast_interfaces(["192.168.1.100", "192.168.2.7", "10.0.0.2"]),
                             ["192.168.1.100", "10.0.0.2"])

    def test_multihomed_service_contains_all_addresses_and_stable_identity(self):
        with tempfile.TemporaryDirectory() as directory:
            identity = load_identity(Path(directory), "SMT 测试机")
            info = service_info(identity, ["192.168.1.100", "192.168.2.7"], 8765)
            device = parse_service(info)
            self.assertEqual(device.device_id, identity.device_id)
            self.assertEqual(device.name, identity.name)
            self.assertEqual(device.addresses, ["192.168.1.100", "192.168.2.7"])
            incompatible = type(info)(info.type, info.name, properties={"protocol": "2"})
            self.assertIsNone(parse_service(incompatible))
