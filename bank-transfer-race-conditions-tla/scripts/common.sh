#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCRIPTS="$ROOT/scripts"
RUN="$ROOT/.run"
LOGS="$RUN/logs"
APP_DIR="$ROOT/app"
SPEC_DIR="$ROOT/spec"
TOOLS="$ROOT/.tools"
TLA_JAR="$TOOLS/tla2tools.jar"
TLA_URL="https://github.com/tlaplus/tlaplus/releases/download/v1.8.0/tla2tools.jar"
DB_NAME=bank
DB_USER=postgres
DB_PASSWORD=postgres
DB_CONTAINER=bank-transfer-postgres
APP_BASE_PATH=/api
cd "$ROOT"

mkdir -p "$RUN" "$LOGS"

SERVICES=""
if [ -f "$SCRIPTS/ports.env" ]; then
  SERVICES="$(grep -v '^[[:space:]]*#' "$SCRIPTS/ports.env" | grep '=' || true)"
fi

service_names() {
  printf "%s\n" "$SERVICES" | sed '/^$/d' | cut -d= -f1
}

service_port() {
  printf "%s\n" "$SERVICES" | sed '/^$/d' | awk -F= -v n="$1" '$1==n{print $2; exit}'
}

service_url() {
  local port
  port="$(service_port "$1")"
  case "$1" in
    postgres) printf "postgresql://localhost:%s/%s\n" "$port" "$DB_NAME" ;;
    app) printf "http://localhost:%s%s\n" "$port" "$APP_BASE_PATH" ;;
    *) printf "http://localhost:%s\n" "$port" ;;
  esac
}

export APP_PORT="$(service_port app)"
export DB_PORT="$(service_port postgres)"

port_pid() {
  lsof -ti "tcp:$1" -sTCP:LISTEN 2>/dev/null | head -1 || true
}

port_up() {
  [ -n "$(port_pid "$1")" ]
}

wait_port_up() {
  local tries
  tries="${2:-60}"
  while [ "$tries" -gt 0 ]; do
    if port_up "$1"; then return 0; fi
    sleep 1
    tries=$((tries - 1))
  done
  return 1
}

wait_port_down() {
  local tries
  tries="${2:-30}"
  while [ "$tries" -gt 0 ]; do
    if ! port_up "$1"; then return 0; fi
    sleep 1
    tries=$((tries - 1))
  done
  return 1
}

db_ready() {
  podman exec "$DB_CONTAINER" pg_isready -U "$DB_USER" -d "$DB_NAME" >/dev/null 2>&1
}

db_up() {
  require podman-compose
  if ! db_ready; then
    podman-compose up -d >"$LOGS/postgres.log" 2>&1 || fail "postgres did not start, see $LOGS/postgres.log"
  fi
  local tries=60
  while [ "$tries" -gt 0 ]; do
    if db_ready && port_up "$DB_PORT"; then
      log "postgres up on $(service_url postgres)"
      return 0
    fi
    sleep 1
    tries=$((tries - 1))
  done
  fail "postgres did not become ready on port $DB_PORT"
}

db_down() {
  require podman-compose
  podman-compose down >>"$LOGS/postgres.log" 2>&1 || fail "postgres did not stop, see $LOGS/postgres.log"
  wait_port_down "$DB_PORT" 30 || fail "postgres still listening on $DB_PORT"
  log "postgres stopped"
}

app_jar() {
  ls "$APP_DIR"/target/*.jar 2>/dev/null | grep -v '\.original$' | head -1 || true
}

start_bg() {
  local name dir
  name="$1"
  dir="$2"
  shift 2
  if [ -f "$RUN/$name.pid" ] && kill -0 "$(cat "$RUN/$name.pid")" 2>/dev/null; then
    log "$name already running"
    return 0
  fi
  ( cd "$dir" && exec "$@" >"$LOGS/$name.log" 2>&1 ) &
  echo $! >"$RUN/$name.pid"
  log "$name started pid $!"
}

stop_bg() {
  local name pid port
  name="$1"
  if [ -f "$RUN/$name.pid" ]; then
    pid="$(cat "$RUN/$name.pid")"
    if kill -0 "$pid" 2>/dev/null; then
      pkill -TERM -P "$pid" 2>/dev/null || true
      kill -TERM "$pid" 2>/dev/null || true
    fi
    rm -f "$RUN/$name.pid"
  fi
  port="$(service_port "$name" || true)"
  if [ -n "$port" ]; then
    wait_port_down "$port" 10 || true
    pid="$(port_pid "$port")"
    if [ -n "$pid" ]; then kill -KILL "$pid" 2>/dev/null || true; fi
  fi
  log "$name stopped"
}

open_url() {
  if command -v open >/dev/null 2>&1; then
    open "$1"
  elif command -v xdg-open >/dev/null 2>&1; then
    xdg-open "$1"
  else
    fail "no browser opener found, open $1 manually"
  fi
}

require() {
  command -v "$1" >/dev/null 2>&1 || fail "$1 is required but not installed"
}

log() {
  printf "%s\n" "$*"
}

fail() {
  printf "ERROR: %s\n" "$*" >&2
  exit 1
}
