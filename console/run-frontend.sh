#!/usr/bin/env bash
set -euo pipefail
console_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
repo_dir=$(cd -- "$console_dir/.." && pwd)
if [[ -n "${JAVA_HOME:-}" ]]; then
  java_command="$JAVA_HOME/bin/java"
elif command -v java >/dev/null; then
  java_command=$(command -v java)
else
  java_command="$repo_dir/toolchains/openpnp/jdk/bin/java"
fi
if [[ ! -x "$java_command" ]]; then
  echo '需要 JDK 17；设置 PATH/JAVA_HOME 后重试。' >&2
  exit 1
fi
app_jar="$console_dir/frontend/target/openpnp-gui-0.0.1-alpha-SNAPSHOT.jar"
if [[ ! -f "$app_jar" ]]; then
  echo '请先运行 console/build-frontend.sh。' >&2
  exit 1
fi
# 与用户已有 ~/.openpnp2 配置隔离；上游默认使用模拟机器。
config_dir="${SMT_CONSOLE_CONFIG_DIR:-$repo_dir/local/openpnp}"
mkdir -p -- "$config_dir"
cd -- "$console_dir/frontend"
exec "$java_command" "-DconfigDir=$config_dir" \
  --add-opens=java.base/java.lang=ALL-UNNAMED \
  --add-opens=java.desktop/java.awt=ALL-UNNAMED \
  --add-opens=java.desktop/java.awt.color=ALL-UNNAMED \
  -jar "$app_jar" "$@"
