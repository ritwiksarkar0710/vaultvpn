#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

: "${DEPLOY_HOST:?Set DEPLOY_HOST to your Oracle VPS IP or hostname}"

DEPLOY_USER="${DEPLOY_USER:-ubuntu}"
DEPLOY_PORT="${DEPLOY_PORT:-22}"
APP_DIR="${APP_DIR:-/opt/vaultvpn}"
SERVICE_NAME="${SERVICE_NAME:-vaultvpn-backend}"
START_COMMAND="${START_COMMAND:-npm run start}"
SSH_KEY="${SSH_KEY:-}"

SSH_OPTS=(
  -p "$DEPLOY_PORT"
  -o StrictHostKeyChecking=accept-new
)

if [[ -n "$SSH_KEY" ]]; then
  SSH_OPTS+=(-i "$SSH_KEY")
fi

REMOTE="${DEPLOY_USER}@${DEPLOY_HOST}"
SSH_CMD=(ssh "${SSH_OPTS[@]}" "$REMOTE")
RSYNC_RSH="ssh ${SSH_OPTS[*]}"

echo "Provisioning ${REMOTE}:${APP_DIR}"

"${SSH_CMD[@]}" "sudo mkdir -p '$APP_DIR' && sudo chown -R '$DEPLOY_USER':'$DEPLOY_USER' '$APP_DIR'"

rsync -az --delete \
  --exclude ".git" \
  --exclude ".idea" \
  --exclude ".codex" \
  --exclude "node_modules" \
  --exclude "backend/node_modules" \
  --exclude "android/.gradle" \
  --exclude "android/app/build" \
  --exclude "backend/.env" \
  -e "$RSYNC_RSH" \
  "$ROOT_DIR/" "$REMOTE:$APP_DIR/"

"${SSH_CMD[@]}" "bash -s" <<EOF
set -euo pipefail

export DEBIAN_FRONTEND=noninteractive

sudo apt-get update
sudo apt-get install -y curl ca-certificates gnupg nginx rsync

if ! command -v node >/dev/null 2>&1; then
  curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
  sudo apt-get install -y nodejs
fi

sudo npm install -g pm2

cd "$APP_DIR/backend"
npm ci --omit=dev

if [[ ! -f .env ]]; then
  cat > .env <<'ENVEOF'
PORT=3000
MONGODB_URI=mongodb://127.0.0.1:27017/vaultvpn
JWT_SECRET=change-me
OBFS4_HOST=bridge.vaultvpn.com
ENVEOF
fi

pm2 delete "$SERVICE_NAME" >/dev/null 2>&1 || true
pm2 start $START_COMMAND --name "$SERVICE_NAME"
pm2 save

sudo env PATH="\$PATH" pm2 startup systemd -u "$DEPLOY_USER" --hp "/home/$DEPLOY_USER"
EOF

echo "Deployment finished for $REMOTE"
