#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if ! command -v node >/dev/null 2>&1; then
  if [[ -x "$REPO_ROOT/toolchains/node/bin/node" ]]; then
    export PATH="$REPO_ROOT/toolchains/node/bin:$PATH"
  else
    echo "请先安装 Node.js 22.12+ 和 npm，再运行此脚本。" >&2
    exit 1
  fi
fi
cd "$REPO_ROOT/console/web"
if [[ ! -d node_modules ]]; then
  npm ci
fi
exec npm run dev -- "$@"
