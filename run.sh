#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_PATH="$ROOT_DIR/server/kt/build/libs/tally-all.jar"
TALLY_FILES="${TALLY_FILES:-$ROOT_DIR/data}"
TALLY_PORT="${TALLY_PORT:-8080}"

FORCE_BUILD=false
for arg in "$@"; do
  if [[ "$arg" == "--build" ]]; then
    FORCE_BUILD=true
  fi
done

if [[ "$FORCE_BUILD" == "true" || ! -f "$JAR_PATH" ]]; then
  "$ROOT_DIR/build.sh"
fi

if [[ ! -d "$TALLY_FILES" ]]; then
  echo "Error: TALLY_FILES directory '$TALLY_FILES' does not exist." >&2
  echo "Set TALLY_FILES=/path/to/your/data or symlink ./data." >&2
  exit 1
fi

echo "Starting Tally production server on http://localhost:$TALLY_PORT (data: $TALLY_FILES)"
exec env TALLY_FILES="$TALLY_FILES" TALLY_PORT="$TALLY_PORT" java -jar "$JAR_PATH"
