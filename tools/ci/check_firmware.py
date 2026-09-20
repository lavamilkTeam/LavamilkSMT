"""Report absent firmware honestly; reject sources without a CMake build entry."""
import os
from pathlib import Path


def main():
    root = Path(__file__).resolve().parents[2]
    board = root / "firmware/mainboard"
    configured = (board / "CMakeLists.txt").is_file()
    sources = [p for p in board.rglob("*") if p.is_file()
               and p.suffix.lower() in {".c", ".cpp", ".cc", ".cxx", ".s", ".asm", ".ioc"}
               and not {"build", ".git", "__pycache__"}.intersection(p.relative_to(board).parts)]
    if not configured and sources:
        raise SystemExit("Firmware sources exist but firmware/mainboard/CMakeLists.txt is missing")
    message = ("CMake entry found; the ARM GCC job must compile and produce an ARM ELF."
               if configured else
               "Firmware is NOT BUILT: no board project exists yet. ARM compilation and images are skipped; "
               "this is not a successful firmware compilation or hardware validation.")
    print(message)
    if "GITHUB_OUTPUT" in os.environ:
        with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as output:
            output.write(f"buildable={str(configured).lower()}\n")
    if "GITHUB_STEP_SUMMARY" in os.environ:
        with open(os.environ["GITHUB_STEP_SUMMARY"], "a", encoding="utf-8") as summary:
            summary.write(f"## Firmware readiness\n\n{message}\n")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
