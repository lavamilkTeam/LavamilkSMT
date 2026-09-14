#!/usr/bin/env bash
# 在目标板执行；依赖已安装，部署包与单元文件位于 /opt/smt/setup/console。
set -euo pipefail
checksum=${1:?请传入 console.tar.gz 的 SHA-256}
[[ "$checksum" =~ ^[0-9a-f]{64}$ ]] || { echo 'SHA-256 格式无效' >&2; exit 1; }
[[ $(id -u) == 0 ]] || { echo '请以 root 执行' >&2; exit 1; }
setup=/opt/smt/setup/console
base=/opt/smt/console
release="$base/releases/${checksum:0:16}"
cd "$setup"
printf '%s  console.tar.gz\n' "$checksum" | sha256sum -c -
for command in java Xvfb x11vnc python3; do command -v "$command" >/dev/null; done
python3 -c 'import websockify'
test -f /usr/share/novnc/vnc.html
if ! id smt-console >/dev/null 2>&1; then
    useradd --system --user-group --home-dir /var/lib/smt-console --shell /usr/sbin/nologin smt-console
fi
install -d -o smt-console -g smt-console -m 700 /var/lib/smt-console /var/lib/smt-console/config
if [[ -f "$setup/password" ]]; then
    install -o smt-console -g smt-console -m 600 "$setup/password" /var/lib/smt-console/password
fi
test -s /var/lib/smt-console/password
if [[ ! -d "$release" ]]; then
    install -d "$base/releases"
    staging=$(mktemp -d "$base/releases/.staging.XXXXXX")
    trap 'rm -rf -- "$staging"' EXIT
    tar -xzf console.tar.gz -C "$staging" --no-same-owner
    chmod -R a+rX "$staging"
    mv "$staging" "$release"
    trap - EXIT
fi
if [[ -L "$base/current" ]]; then
    readlink "$base/current" > "$setup/previous-release.txt"
fi
ln -sfn "$release" "$base/current.next"
mv -Tf "$base/current.next" "$base/current"
install -m 644 "$setup/smt-console.service" /etc/systemd/system/smt-console.service
systemctl daemon-reload
systemctl enable smt-console.service
systemctl restart smt-console.service
systemctl is-active smt-console.service
