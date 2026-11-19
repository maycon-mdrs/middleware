#!/usr/bin/env bash
# Sobe gateway + 4 InstanceApp + ops (InstanceManagerServer).
# Uso: na raiz do repo OU via systemd (WorkingDirectory=/home/ec2-user/middleware).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-21-amazon-corretto}"
export PATH="$JAVA_HOME/bin:$PATH"

JSON_JAR="${HOME}/.m2/repository/org/json/json/20250517/json-20250517.jar"
CP="time-app/target/classes:middleware-core/target/classes:${JSON_JAR}"

if [[ ! -d time-app/target/classes || ! -d middleware-core/target/classes ]]; then
  echo "[start-all] classes não encontradas — rode: mvn clean install"
  exit 1
fi
if [[ ! -f "$JSON_JAR" ]]; then
  echo "[start-all] jar org.json não encontrado em $JSON_JAR"
  exit 1
fi

mkdir -p logs
pkill -f 'br.imd.ufrn' 2>/dev/null || true
sleep 2

echo "[start-all] GatewayApp..."
nohup java -cp "$CP" br.imd.ufrn.app.GatewayApp > gateway.log 2>&1 &
sleep 2

echo "[start-all] InstanceApp br-1 br-2 pt-1 pt-2..."
nohup java -cp "$CP" br.imd.ufrn.app.instance.InstanceApp br br-1 9101 > br-1.log 2>&1 &
nohup java -cp "$CP" br.imd.ufrn.app.instance.InstanceApp br br-2 9102 > br-2.log 2>&1 &
nohup java -cp "$CP" br.imd.ufrn.app.instance.InstanceApp pt pt-1 9201 > pt-1.log 2>&1 &
nohup java -cp "$CP" br.imd.ufrn.app.instance.InstanceApp pt pt-2 9202 > pt-2.log 2>&1 &

echo "[start-all] InstanceManagerServer :8081..."
nohup java -cp "$CP" br.imd.ufrn.app.ops.InstanceManagerServer > manager.log 2>&1 &

sleep 2
echo "[start-all] processos:"
ps aux | grep 'br.imd.ufrn' | grep -v grep || true
echo "[start-all] OK — logs em $ROOT/*.log"
