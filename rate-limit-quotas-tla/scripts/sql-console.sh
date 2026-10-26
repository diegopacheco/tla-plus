#!/usr/bin/env bash
set -euo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh"

port_up "$DB_PORT" || fail "postgres is not running on $DB_PORT, run ./scripts/start-all.sh first"

if command -v psql >/dev/null 2>&1; then
  PGPASSWORD="$DB_PASSWORD" psql -h localhost -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" "$@"
else
  podman exec -it "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" "$@"
fi
