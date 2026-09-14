#!/usr/bin/env bash
# Debian/Ubuntu 控制端：下载已配置 APT 源中的工具，仅解包到工作区，无需 sudo。
set -euo pipefail
console_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
repo_dir=$(cd -- "$console_dir/.." && pwd)
setup_dir="$repo_dir/artifacts/console-dev-setup"
tool_root="$repo_dir/toolchains/console-dev/root"
mkdir -p "$setup_dir/debs" "$tool_root"
cd -- "$setup_dir/debs"
apt-get download xvfb xserver-common x11vnc libvncserver1 libvncclient1 \
  novnc python3-websockify python3-numpy python3-jwcrypto python3-redis python3-simplejson
sha256sum ./*.deb > "$setup_dir/packages.sha256"
for package_file in ./*.deb; do
  dpkg-deb -x "$package_file" "$tool_root"
done
PYTHONPATH="$tool_root/usr/lib/python3/dist-packages${PYTHONPATH:+:$PYTHONPATH}" \
  python3 -m websockify --help >/dev/null
echo '开发显示工具已准备好。构建前端后运行 ./console/dev-frontend.sh。'
