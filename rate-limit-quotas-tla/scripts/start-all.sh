#!/usr/bin/env bash
set -euo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh"

log "starting"

db_up

jar="$(app_jar)"
[ -n "$jar" ] || fail "app jar not found, run ./scripts/setup.sh first"
start_bg app "$APP_DIR" java -jar "$jar"
wait_port_up "$APP_PORT" 60 || fail "app did not open port $APP_PORT, see $LOGS/app.log"
log "app up on $(service_url app)"

"$SCRIPTS/status.sh"

log "links"
for name in $(service_names); do
  printf "%-14s %s\n" "$name" "$(service_url "$name")"
done
