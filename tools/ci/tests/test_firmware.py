"""Exercise readiness and artifact rejection without pretending to compile board firmware."""
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest


SCRIPTS = Path(__file__).resolve().parents[1]


class FirmwareCIGuardTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.board = self.root / "firmware/mainboard"
        self.board.mkdir(parents=True)
        self.scripts = self.root / "tools/ci"
        self.scripts.mkdir(parents=True)
        for name in ("check_firmware.py", "collect_firmware.py"):
            shutil.copy2(SCRIPTS / name, self.scripts / name)

    def readiness(self):
        env = dict(os.environ, GITHUB_OUTPUT=str(self.root / "output"),
                   GITHUB_STEP_SUMMARY=str(self.root / "summary"))
        return subprocess.run([sys.executable, str(self.scripts / "check_firmware.py")],
                              env=env, capture_output=True, text=True)

    def test_docs_only_is_explicitly_not_built(self):
        (self.board / "README.md").write_text("Board project pending", encoding="utf-8")
        result = self.readiness()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual((self.root / "output").read_text(), "buildable=false\n")
        self.assertIn("Firmware is NOT BUILT", (self.root / "summary").read_text())

    def test_sources_without_build_entry_fail_instead_of_skip(self):
        (self.board / "main.c").write_text("void main(void) {}", encoding="utf-8")
        result = self.readiness()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("CMakeLists.txt is missing", result.stderr)

    def test_cmake_entry_enables_the_real_build_job(self):
        (self.board / "CMakeLists.txt").write_text("project(board C)", encoding="utf-8")
        result = self.readiness()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual((self.root / "output").read_text(), "buildable=true\n")

    def collect(self):
        return subprocess.run([sys.executable, str(self.scripts / "collect_firmware.py"),
                               str(self.root / "build"), str(self.root / "dist")],
                              capture_output=True, text=True)

    def test_empty_build_cannot_publish_firmware(self):
        result = self.collect()
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse((self.root / "dist").exists())

    def test_cmake_probe_cannot_be_published_as_board_firmware(self):
        probe = self.root / "build/CMakeFiles/probe.elf"
        probe.parent.mkdir(parents=True)
        probe.write_bytes(b"not firmware")
        result = self.collect()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("did not produce a firmware .elf", result.stderr)

    def test_host_executable_cannot_be_published_as_arm_firmware(self):
        image = self.root / "build/board.elf"
        image.parent.mkdir()
        shutil.copy2(sys.executable, image)
        result = self.collect()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("ARM hard-float executable", result.stderr)
        self.assertFalse((self.root / "dist").exists())


if __name__ == "__main__":
    unittest.main()
