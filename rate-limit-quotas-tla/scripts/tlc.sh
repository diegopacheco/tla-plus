#!/usr/bin/env bash
set -euo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh"

[ -s "$TLA_JAR" ] || fail "tla2tools not found, run ./scripts/setup.sh first"

failed=0
for cfg in "$SPEC_DIR"/*.cfg; do
  name="$(basename "$cfg" .cfg)"
  spec="$(ls "$SPEC_DIR"/*.tla | head -1)"
  out="$LOGS/tlc-$name.log"
  set +e
  ( cd "$SPEC_DIR" && java -XX:+UseParallelGC -cp "$TLA_JAR" tlc2.TLC -workers auto -noGenerateSpecTE \
      -metadir "$RUN/tlc-states/$name" -config "$cfg" "$spec" ) >"$out" 2>&1
  code=$?
  set -e
  case "$name" in
    *Safe)
      if [ "$code" -eq 0 ] && grep -q "No error has been found" "$out"; then
        log "PASS $name: no error found, $(grep 'distinct states found' "$out")"
      else
        printf "FAIL %s: expected no error, see %s\n" "$name" "$out" >&2
        failed=1
      fi
      ;;
    *)
      bug="$(grep -E 'is violated|Deadlock reached' "$out" | head -1 || true)"
      if [ "$code" -ne 0 ] && [ -n "$bug" ]; then
        log "PASS $name: bug found as expected -> $bug (trace in $out)"
      else
        printf "FAIL %s: expected TLC to find the bug, see %s\n" "$name" "$out" >&2
        failed=1
      fi
      ;;
  esac
done

[ "$failed" -eq 0 ] || fail "model checking did not match expectations"
