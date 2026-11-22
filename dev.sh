#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TALLY_FILES="${TALLY_FILES:-$ROOT_DIR/data}"
TALLY_PORT="${TALLY_PORT:-8080}"
TALLY_DEV_PORT="${TALLY_DEV_PORT:-5173}"

if [[ ! -d "$TALLY_FILES" ]]; then
  echo "Error: TALLY_FILES directory '$TALLY_FILES' does not exist." >&2
  echo "Set TALLY_FILES=/path/to/your/data or symlink ./data." >&2
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
  cd "$ROOT_DIR"
  TALLY_BACKEND_PORT="$TALLY_PORT" \
  TALLY_DEV_PORT="$TALLY_DEV_PORT" \
    pnpm --filter @tally/client dev
) &
PIDS+=($!)

wait -n "${PIDS[@]}"
