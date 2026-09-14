#!/usr/bin/env bash
set -euo pipefail
console_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
exec python3 "$console_dir/tools/dev_frontend.py" "$@"
