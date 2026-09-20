"""Run controller tests against the installed wheel; missing optional coverage fails CI."""
from pathlib import Path
import sys
import unittest

# Import explicitly so CI cannot silently skip vision or lack discovery dependencies.
import cv2
import numpy
import zeroconf
import ifaddr
import smt_controller


def main():
    root = Path(__file__).resolve().parents[2]
    if (root / "controller/src") in Path(smt_controller.__file__).resolve().parents:
        raise SystemExit("CI must test the installed wheel, not an editable source checkout")
    print(f"OpenCV {cv2.__version__}; NumPy {numpy.__version__}", flush=True)
    suite = unittest.defaultTestLoader.discover(str(root / "controller/tests"))
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    if result.testsRun == 0 or result.skipped:
        print("CI requires nonempty coverage with no skipped tests", file=sys.stderr)
        return 1
    return 0 if result.wasSuccessful() else 1


if __name__ == "__main__":
    raise SystemExit(main())
