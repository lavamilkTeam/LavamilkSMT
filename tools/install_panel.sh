#!/usr/bin/env bash
# Install already-built native binaries. No package installs, service starts,
# firmware operations, or changes to the existing Swing/controller services.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
bin_dir="$ROOT/console/build/bin"
kiosk=0
usage() { echo "Usage: sudo $0 [--bin-dir DIR] [--kiosk]"; }
while (($#)); do
  case "$1" in
    --bin-dir) [[ $# -ge 2 ]] || { usage >&2; exit 2; }; bin_dir="$2"; shift 2 ;;
    --kiosk) kiosk=1; shift ;;
    --help|-h) usage; exit 0 ;;
    *) usage >&2; exit 2 ;;
  esac
done
[[ $EUID -eq 0 ]] || { echo "Run this installer as root." >&2; exit 1; }
[[ -x "$bin_dir/smt-panel" ]] || { echo "Missing $bin_dir/smt-panel" >&2; exit 1; }
if ((kiosk)); then
  [[ -x "$bin_dir/smt-desktop" ]] || { echo "Missing $bin_dir/smt-desktop" >&2; exit 1; }
  for program in /usr/bin/Xorg /usr/bin/xinit /usr/bin/dbus-run-session /usr/bin/openbox; do
    [[ -x "$program" ]] || { echo "Missing $program; see console/desktop/README.md" >&2; exit 1; }
  done
  getent group video >/dev/null || { echo "Missing video group." >&2; exit 1; }
  if ! id smt-kiosk >/dev/null 2>&1; then
    useradd --system --user-group --home-dir /var/lib/smt-kiosk --shell /usr/sbin/nologin smt-kiosk
  fi
fi
# Rename complete files into place, preserving running executables until restart.
install -d -m 0755 /opt/smt/panel/bin
install -m 0755 "$bin_dir/smt-panel" /opt/smt/panel/bin/smt-panel.new
mv -f /opt/smt/panel/bin/smt-panel.new /opt/smt/panel/bin/smt-panel
install -m 0644 "$ROOT/deploy/smt-panel.service" /etc/systemd/system/smt-panel.service
if ((kiosk)); then
  install -d -m 0755 /usr/local/libexec /etc/smt
  install -m 0755 "$bin_dir/smt-desktop" /opt/smt/panel/bin/smt-desktop.new
  mv -f /opt/smt/panel/bin/smt-desktop.new /opt/smt/panel/bin/smt-desktop
  install -m 0755 "$ROOT/deploy/kiosk/smt-kiosk-session" /usr/local/libexec/smt-kiosk-session
  install -m 0644 "$ROOT/deploy/kiosk/smt-kiosk.service" /etc/systemd/system/smt-kiosk.service
  # Keep site-specific display preferences on repeat installations.
  [[ -e /etc/smt/openbox-kiosk.xml ]] || install -m 0644 "$ROOT/deploy/kiosk/openbox-kiosk.xml" /etc/smt/openbox-kiosk.xml
  [[ -e /etc/smt/kiosk.env ]] || install -m 0644 "$ROOT/deploy/kiosk/kiosk.env.example" /etc/smt/kiosk.env
fi
systemctl daemon-reload
echo "Installed. Services have not been started or enabled."
echo "Web: sudo systemctl enable --now smt-panel.service"
if ((kiosk)); then
  echo "After checking local Xorg/display compatibility: sudo systemctl enable --now smt-kiosk.service"
fi
