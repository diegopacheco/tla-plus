#!/usr/bin/env bash
set -euo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh"

port_up "$APP_PORT" || fail "app is not running on $APP_PORT, run ./scripts/start-all.sh first"

url="$(service_url app)/acme"
log "opening $url"
open_url "$url"
