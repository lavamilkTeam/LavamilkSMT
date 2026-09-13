#!/usr/bin/env bash
# 目标板执行；前置为 /opt/smt/venv 及 setup 中已校验的源码、wheel、项目快照、服务文件。
set -euo pipefail
task_setup=/opt/smt/setup
task_python=/opt/smt/venv/bin/python
cd "$task_setup"
sha256sum -c network-source.sha256

"$task_python" -m pip install --no-index --find-links "$task_setup/network-wheels" poetry-core==2.4.1 ifaddr==0.2.0
SKIP_CYTHON=1 "$task_python" -m pip wheel --no-build-isolation --no-deps \
  "$task_setup/zeroconf-0.151.3.tar.gz" -w "$task_setup/network-wheels"
"$task_python" -m pip install --no-index --find-links "$task_setup/network-wheels" zeroconf==0.151.3

task_stamp=$(date -u +%Y%m%dT%H%M%SZ)
tar -czf "$task_setup/controller-before-network-$task_stamp.tar.gz" -C /opt/smt controller
tar -xzf "$task_setup/controller.tar.gz" -C /opt/smt
"$task_python" -m pip install --no-build-isolation --no-deps -e '/opt/smt/controller[network]'
"$task_python" -m pip check
"$task_python" -m unittest discover -s /opt/smt/controller/tests -v

if ! getent passwd smt-controller >/dev/null; then
  useradd --system --user-group --no-create-home --home-dir /var/lib/smt-controller \
    --shell /usr/sbin/nologin smt-controller
fi
if [[ -f /etc/systemd/system/smt-controller.service ]]; then
  cp /etc/systemd/system/smt-controller.service "$task_setup/smt-controller.service.before-$task_stamp"
fi
install -m 644 "$task_setup/smt-controller.service" /etc/systemd/system/smt-controller.service
systemctl daemon-reload
systemctl enable smt-controller.service
systemctl restart smt-controller.service
systemctl is-active smt-controller.service
