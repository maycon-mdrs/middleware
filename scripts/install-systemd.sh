#!/usr/bin/env bash
# Instala o serviço systemd para subir o middleware no boot.
# Rodar NA EC2, uma vez, como ec2-user (usa sudo):
#   cd ~/middleware && git pull && bash scripts/install-systemd.sh
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
UNIT_SRC="$ROOT/scripts/middleware.service"
UNIT_DST=/etc/systemd/system/middleware.service

chmod +x "$ROOT/scripts/start-all.sh" "$ROOT/scripts/stop-all.sh" "$ROOT/scripts/install-systemd.sh"

# Garante caminhos do unit apontando para este clone
tmp="$(mktemp)"
sed "s|/home/ec2-user/middleware|$ROOT|g" "$UNIT_SRC" > "$tmp"

sudo cp "$tmp" "$UNIT_DST"
rm -f "$tmp"

sudo systemctl daemon-reload
sudo systemctl enable middleware.service
sudo systemctl restart middleware.service

echo
echo "Status:"
sudo systemctl status middleware.service --no-pager || true
echo
echo "No próximo boot o middleware sobe sozinho."
echo "Manual:  sudo systemctl start|stop|restart|status middleware"
