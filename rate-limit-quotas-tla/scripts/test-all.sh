#!/usr/bin/env bash
set -euo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh"

log "tests started"

"$SCRIPTS/tlc.sh" || fail "tla+ model checking failed"

db_up
( cd "$APP_DIR" && mvn test ) || fail "app tests failed"

log "tests passed"
