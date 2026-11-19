#!/usr/bin/env bash
# Encerra gateway, instâncias e ops.
set -euo pipefail

echo "[stop-all] encerrando br.imd.ufrn..."
pkill -f 'br.imd.ufrn' 2>/dev/null || true
sleep 2
if ps aux | grep 'br.imd.ufrn' | grep -v grep >/dev/null; then
  echo "[stop-all] forçando..."
  pkill -9 -f 'br.imd.ufrn' 2>/dev/null || true
fi
echo "[stop-all] OK"
