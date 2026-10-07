#!/usr/bin/env bash
set -euo pipefail
CONSOLE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
binary="$CONSOLE_DIR/build/bin/smt-desktop"
if [[ ! -x "$binary" ]]; then
  echo "请先运行 ./console/build-panel.sh desktop。" >&2
  exit 1
fi
if [[ -z "${DISPLAY:-}" && -z "${WAYLAND_DISPLAY:-}" ]]; then
  echo "未发现图形会话。无桌面设备请使用 deploy/kiosk/ 的启动配置；纯 Web 使用 smt-panel。" >&2
  exit 1
fi
exec "$binary" "$@"
