#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export PATH="$ROOT/toolchains/go/bin:$ROOT/toolchains/node/bin:$PATH"
mode="${1:-all}"
case "$mode" in all|web|desktop) ;; *) echo "Usage: $0 [all|web|desktop]" >&2; exit 2;; esac
cd "$ROOT/console/web"
npm ci
npm run check
# This is a generated build boundary, never a second copy of frontend sources.
mkdir -p "$ROOT/console/internal/webui/dist"
find "$ROOT/console/internal/webui/dist" -mindepth 1 -maxdepth 1 -exec rm -rf -- {} +
cp -a dist/. "$ROOT/console/internal/webui/dist/"
cd "$ROOT/console"
mkdir -p build/bin
if [[ "$mode" != desktop ]]; then
  CGO_ENABLED=0 go build -trimpath -o build/bin/smt-panel ./backend/cmd/smt-panel
fi
if [[ "$mode" != web ]]; then
  command -v pkg-config >/dev/null || { echo "Install pkg-config, GTK3 and WebKitGTK development packages (see desktop/README.md)." >&2; exit 1; }
  pkg-config --exists gtk+-3.0 || { echo "Missing GTK3 development packages." >&2; exit 1; }
  tags=desktop,production
  if pkg-config --exists webkit2gtk-4.1; then
    tags+=,webkit2_41
  elif ! pkg-config --exists webkit2gtk-4.0; then
    echo "Missing WebKitGTK 4.1 or 4.0 development packages." >&2; exit 1
  fi
  CGO_ENABLED=1 go build -trimpath -tags "$tags" -o build/bin/smt-desktop ./desktop
fi
