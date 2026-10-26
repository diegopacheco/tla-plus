#!/usr/bin/env bash
set -euo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh"

log "setup started"

require java
require mvn
require curl
require podman
require podman-compose

if [ ! -s "$TLA_JAR" ]; then
  mkdir -p "$TOOLS"
  curl -fsSL -o "$TLA_JAR.tmp" "$TLA_URL" || fail "could not download $TLA_URL"
  mv "$TLA_JAR.tmp" "$TLA_JAR"
fi
log "tla2tools ready at $TLA_JAR"

podman-compose pull >"$LOGS/setup-pull.log" 2>&1 || fail "podman-compose pull failed, see $LOGS/setup-pull.log"

( cd "$APP_DIR" && mvn -q -DskipTests package ) || fail "app build failed"
log "app built at $(app_jar)"

log "setup done"
