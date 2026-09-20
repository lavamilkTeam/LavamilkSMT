"""Export ARM executable ELFs, fail if a build only produced libraries or host binaries."""
from pathlib import Path
import shutil
import struct
import subprocess
import sys


def collect(build, destination):
    images = sorted(p for p in build.rglob("*.elf")
                    if "CMakeFiles" not in p.relative_to(build).parts)
    if not images:
        raise SystemExit("Build did not produce a firmware .elf; no firmware artifact will be published")
    for image in images:
        with image.open("rb") as stream:
            header = stream.read(52)
        if (len(header) < 52 or header[:6] != b"\x7fELF\x01\x01"
                or struct.unpack_from("<HH", header, 16) != (2, 40)
                or not struct.unpack_from("<I", header, 36)[0] & 0x400):
            raise SystemExit(f"Expected a 32-bit little-endian ARM hard-float executable: {image}")
    for image in images:
        target = destination / image.relative_to(build)
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(image, target)
        subprocess.run(["arm-none-eabi-size", str(image)], check=True)
        for fmt, suffix in (("binary", ".bin"), ("ihex", ".hex")):
            subprocess.run(["arm-none-eabi-objcopy", "-O", fmt, str(image),
                            str(target.with_suffix(suffix))], check=True)
        for suffix in (".bin", ".hex"):
            if target.with_suffix(suffix).stat().st_size == 0:
                raise SystemExit(f"Empty firmware image: {target.with_suffix(suffix)}")
    for source in build.rglob("*.map"):
        target = destination / source.relative_to(build)
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source, target)


if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit("Usage: collect_firmware.py BUILD_DIRECTORY OUTPUT_DIRECTORY")
    collect(Path(sys.argv[1]), Path(sys.argv[2]))
