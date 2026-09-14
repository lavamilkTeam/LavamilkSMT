#!/usr/bin/env bash
# 在已准备好 Python/OpenCV 和 smt-controller 用户的目标板执行。
set -euo pipefail
task_setup=/opt/smt/setup/preview
task_python=/opt/smt/venv/bin/python
cd "$task_setup"
sha256sum -c preview-source.sha256
getent passwd smt-controller >/dev/null
getent group video >/dev/null
"$task_python" -c 'import cv2, numpy; assert cv2.videoio_registry.hasBackend(cv2.CAP_V4L2); print(cv2.__version__, numpy.__version__)'

task_stamp=$(date -u +%Y%m%dT%H%M%SZ)
tar -czf "$task_setup/controller-before-preview-$task_stamp.tar.gz" -C /opt/smt controller
tar -xzf "$task_setup/controller.tar.gz" -C /opt/smt
"$task_python" -m pip install --no-build-isolation --no-deps -e /opt/smt/controller
"$task_python" -m pip check
"$task_python" -m unittest discover -s /opt/smt/controller/tests -v

if [[ -f /etc/systemd/system/smt-preview.service ]]; then
  cp /etc/systemd/system/smt-preview.service "$task_setup/smt-preview.service.before-$task_stamp"
fi
install -m 644 "$task_setup/smt-preview.service" /etc/systemd/system/smt-preview.service
systemctl daemon-reload
systemctl enable smt-preview.service
systemctl restart smt-preview.service
systemctl is-active smt-preview.service
