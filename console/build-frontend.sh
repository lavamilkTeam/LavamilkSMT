#!/usr/bin/env bash
set -euo pipefail
console_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
repo_dir=$(cd -- "$console_dir/.." && pwd)
if [[ -z "${JAVA_HOME:-}" ]] && ! command -v java >/dev/null && [[ -x "$repo_dir/toolchains/openpnp/jdk/bin/java" ]]; then
  export JAVA_HOME="$repo_dir/toolchains/openpnp/jdk"
fi
if command -v mvn >/dev/null; then
  maven_command=$(command -v mvn)
elif [[ -x "$repo_dir/toolchains/openpnp/maven/bin/mvn" ]]; then
  maven_command="$repo_dir/toolchains/openpnp/maven/bin/mvn"
else
  echo '需要 Maven 3.9 和 JDK 17；设置 PATH/JAVA_HOME 后重试。' >&2
  exit 1
fi
# 保持 OpenPnP 的源码链接指向导入基线，避免误用外层 SMT 仓库提交号。
exec "$maven_command" -B -ntp -f "$console_dir/frontend/pom.xml" \
  -Dopenpnp.version=vendored.5bd404c package "$@"
