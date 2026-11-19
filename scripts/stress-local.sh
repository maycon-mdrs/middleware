#!/usr/bin/env bash
# Carga só na EC2 (127.0.0.1) — isola rede PC↔AWS do código.
# Uso: bash scripts/stress-local.sh [seconds] [parallel]
set -euo pipefail
SECONDS_TOTAL="${1:-60}"
PARALLEL="${2:-8}"
URL_BR=http://127.0.0.1:8080/time/br
URL_PT=http://127.0.0.1:8080/time/pt
OK=0
ERR=0
START=$(date +%s)

echo "[stress-local] ${SECONDS_TOTAL}s parallel=${PARALLEL} -> ${URL_BR} / ${URL_PT}"

worker() {
  local end=$((START + SECONDS_TOTAL))
  local ok=0 err=0
  while (( $(date +%s) < end )); do
    if curl -fsS --max-time 3 "$URL_BR" >/dev/null && curl -fsS --max-time 3 "$URL_PT" >/dev/null; then
      ok=$((ok + 1))
    else
      err=$((err + 1))
    fi
  done
  echo "$ok $err"
}

export -f worker
export START SECONDS_TOTAL URL_BR URL_PT

tmp=$(mktemp)
for i in $(seq 1 "$PARALLEL"); do
  worker >>"$tmp" &
done
wait

while read -r o e; do
  OK=$((OK + o))
  ERR=$((ERR + e))
done <"$tmp"
rm -f "$tmp"

TOTAL=$((OK + ERR))
echo "[stress-local] ok=$OK err=$ERR total=$TOTAL"
if (( TOTAL > 0 )); then
  python3 - <<PY
ok=$OK; err=$ERR; t=$TOTAL
print(f"[stress-local] error%={100.0*err/t:.2f}%")
PY
fi
