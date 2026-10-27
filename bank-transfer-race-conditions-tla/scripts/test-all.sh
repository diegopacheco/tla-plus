#!/usr/bin/env bash
set -euo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh"

log "tests started"

[ -s "$TLA_JAR" ] || fail "tla2tools not found, run ./scripts/setup.sh first"
db_up

set +e
( cd "$APP_DIR" && mvn -q test ) >"$LOGS/test.log" 2>&1
code=$?
set -e

for report in "$APP_DIR"/target/surefire-reports/TEST-*.xml; do
  [ -f "$report" ] || continue
  suite="$(basename "$report" .xml | sed 's/^TEST-//; s/.*\.//')"
  grep -o '<testcase name="[^"]*"[^>]*\(/>\|>\)' "$report" | sed 's/<testcase name="\([^"]*\)".*/\1/' | while read -r name; do
    if awk -v n="$name" '$0 ~ "<testcase name=\""n"\"" {found=1; next} found && /<testcase/ {exit} found && /<(failure|error)/ {bad=1} END {exit bad ? 0 : 1}' "$report"; then
      printf "FAIL %s.%s\n" "$suite" "$name"
    else
      printf "PASS %s.%s\n" "$suite" "$name"
    fi
  done
done

grep -h "Tests run:" "$APP_DIR"/target/surefire-reports/*.txt 2>/dev/null | sed 's/ -- in .*//; s/, Time elapsed.*//' | awk -F'[:,]' '{t+=$2; f+=$4; e+=$6; s+=$8} END {printf "total %d, failures %d, errors %d, skipped %d\n", t, f, e, s}'

[ "$code" -eq 0 ] || fail "tests failed, see $LOGS/test.log"
log "tests passed, TLC traces in app/target/tlc/"
