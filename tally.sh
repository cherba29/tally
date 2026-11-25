#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_PATH="$ROOT_DIR/server/kt/build/libs/tally-all.jar"
FORCE_BUILD=false

if [[ "${1:-}" == "--build" ]]; then
  FORCE_BUILD=true
  shift
fi

if [[ "$FORCE_BUILD" == "true" || ! -f "$JAR_PATH" ]]; then
  "$ROOT_DIR/build.sh" >&2
fi

if [[ -z "${TALLY_FILES:-}" && -z "${TALLY_PATH:-}" && -d "$ROOT_DIR/sample-data" ]]; then
  export TALLY_FILES="$ROOT_DIR/sample-data"
fi

exec java -jar "$JAR_PATH" cli "$@"
