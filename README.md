# VaultVPN Project

VaultVPN contains an Android client plus a Node/Mongo backend.

## Current Status

- Android app includes the VPN UI, server picker, and `VpnService` wiring.
- Backend routes live under `backend/src/routes`.
- Oracle VPS deployment is automated with `scripts/deploy_oracle_vps.sh`.

## Oracle VPS Deploy

Set the target host and run the deploy script:

```bash
export DEPLOY_HOST=your.oracle.vps.ip
export DEPLOY_USER=ubuntu
export SSH_KEY=~/.ssh/your-key
./scripts/deploy_oracle_vps.sh
```

Useful optional variables:

- `APP_DIR` defaults to `/opt/vaultvpn`
- `SERVICE_NAME` defaults to `vaultvpn-backend`
- `START_COMMAND` defaults to `npm run start`
- `DEPLOY_PORT` defaults to `22`

The script syncs the repo, installs Node.js and PM2 if needed, runs `npm ci` in `backend/`, creates a starter `backend/.env` if missing, and starts the backend with PM2.
