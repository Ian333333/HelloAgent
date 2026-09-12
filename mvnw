#!/usr/bin/env bash
# HelloAgent 轻量 Maven Wrapper：首次运行自动下载 Apache Maven 3.9.9
set -e

MAVEN_VERSION=3.9.9
URLS=(
  "https://mirrors.huaweicloud.com/apache/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz"
  "https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz"
)

if [ -z "$JAVA_HOME" ]; then
  JAVA_HOME="$(java -XshowSettings:properties -version 2>&1 | sed -n 's/^[[:space:]]*java.home[[:space:]]*=[[:space:]]*//p')"
fi
if [ -z "$JAVA_HOME" ] || [ ! -x "$JAVA_HOME/bin/java" ]; then
  echo "[mvnw] 未找到 JDK，请安装 JDK 17+ 或设置 JAVA_HOME" >&2
  exit 1
fi

MVN_HOME="$HOME/.m2/wrapper/dists/apache-maven-${MAVEN_VERSION}"
if [ ! -x "$MVN_HOME/bin/mvn" ]; then
  echo "[mvnw] 首次运行，正在下载 Apache Maven ${MAVEN_VERSION} ..."
  TGZ="$(mktemp -t apache-maven.XXXXXX.tar.gz)"
  ok=0
  for url in "${URLS[@]}"; do
    echo "[mvnw] $url"
    if curl -fsSL "$url" -o "$TGZ" 2>/dev/null || wget -qO "$TGZ" "$url" 2>/dev/null; then
      ok=1
      break
    fi
  done
  if [ "$ok" != "1" ]; then
    echo "[mvnw] Maven 下载失败，请检查网络后重试" >&2
    rm -f "$TGZ"
    exit 1
  fi
  mkdir -p "$(dirname "$MVN_HOME")"
  tar -xzf "$TGZ" -C "$(dirname "$MVN_HOME")"
  rm -f "$TGZ"
fi

exec "$MVN_HOME/bin/mvn" "$@"
