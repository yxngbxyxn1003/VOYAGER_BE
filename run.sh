#!/usr/bin/env bash
set -euo pipefail

cd /srv/app/app

# .env 로드(값에 공백/특수문자 있어도 안전)
if [ -f .env ]; then
  set -a
  . ./.env
  set +a
fi

# 최신 JAR(plain 제외) 선택
JAR=$(ls -t build/libs/*.jar 2>/dev/null | grep -v -- '-plain\.jar$' | head -n1 || true)
if [ -z "${JAR:-}" ]; then
  echo "실행할 JAR를 못 찾았어.(build/libs/*.jar 없음)"
  exit 1
fi

echo "Starting with JAR => $JAR"
exec /usr/bin/java -jar "$JAR"
#!/usr/bin/env bash
set -euo pipefail
cd /srv/app/app

# .env 로드 (있으면)
if [ -f .env ]; then
  export $(grep -v '^#' .env | xargs)
fi

# 실행용 JAR 선택(plain 제외)
JAR="$(ls build/libs/*-SNAPSHOT.jar 2>/dev/null | grep -v plain | head -n1)"
[ -z "${JAR:-}" ] && JAR="$(ls target/*-SNAPSHOT.jar 2>/dev/null | grep -v plain | head -n1)"
[ -z "${JAR:-}" ] && { echo "실행할 JAR 없음."; exit 1; }

exec /usr/bin/java -jar "$JAR"
