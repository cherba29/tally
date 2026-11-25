#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TALLY_FILES="${TALLY_FILES:-${TALLY_PATH:-$ROOT_DIR/sample-data}}"
TALLY_PORT="${TALLY_PORT:-8080}"
TALLY_DEV_PORT="${TALLY_DEV_PORT:-5173}"

usage() {
  echo "Usage: $(basename "$0") [--data <path> | <path>]"
  echo ""
  echo "Starts Tally in development mode (Ktor backend + Vite frontend with auto-reload)."
  echo "Defaults to '$ROOT_DIR/sample-data' (overridable via CLI argument or TALLY_FILES env var)."
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    -h|--help)
      usage
      exit 0
      ;;
    --data)
      if [[ $# -lt 2 ]]; then
        echo "Error: --data requires a directory path." >&2
        exit 1
      fi
      TALLY_FILES="$2"
      shift 2
      ;;
    --data=*)
      TALLY_FILES="${1#*=}"
      shift
      ;;
    -*)
      echo "Error: Unknown option '$1'." >&2
      usage >&2
      exit 1
      ;;
    *)
      TALLY_FILES="$1"
      shift
      ;;
  esac
done

if [[ ! -d "$TALLY_FILES" ]]; then
  echo "Error: TALLY_FILES directory '$TALLY_FILES' does not exist." >&2
  echo "Pass a data directory via '--data /path/to/data', positional argument, or TALLY_FILES env var." >&2
  exit 1
fi

PIDS=()

cleanup() {
  trap - EXIT INT TERM
  echo ""
  echo "Stopping dev servers..."
  for pid in "${PIDS[@]:-}"; do
    if kill -0 "$pid" 2>/dev/null; then
      kill "$pid" 2>/dev/null || true
    fi
  done
  wait 2>/dev/null || true
}
trap cleanup EXIT INT TERM

echo "Compiling Kotlin backend..."
(cd "$ROOT_DIR/server/kt" && ./gradlew classes -PskipFrontend=true --console=plain)

echo ""
echo "Starting Tally in development mode:"
echo "  Data directory : $TALLY_FILES"
echo "  Backend (Ktor) : http://localhost:$TALLY_PORT (autoreload enabled)"
echo "  Frontend (Vite): http://localhost:$TALLY_DEV_PORT (auto-reloads browser on UI changes)"
echo ""

# 1. Continuous Kotlin compilation so Ktor autoreloads changed classes on save
(
  cd "$ROOT_DIR/server/kt"
  ./gradlew -t classes -PskipFrontend=true --console=plain
) &
PIDS+=($!)

# 2. Ktor backend server with autoreload enabled
(
  cd "$ROOT_DIR/server/kt"
  TALLY_FILES="$TALLY_FILES" \
  TALLY_PORT="$TALLY_PORT" \
  KTOR_DEVELOPMENT=true \
    ./gradlew run -PskipFrontend=true --console=plain
) &
PIDS+=($!)

# 3. Vite dev server proxying /graphql and /subscriptions to Ktor
(
  cd "$ROOT_DIR/client"
  TALLY_BACKEND_PORT="$TALLY_PORT" \
  TALLY_DEV_PORT="$TALLY_DEV_PORT" \
    pnpm dev
) &
PIDS+=($!)

wait -n "${PIDS[@]}"
