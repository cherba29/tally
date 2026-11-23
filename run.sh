#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_PATH="$ROOT_DIR/server/kt/build/libs/tally-all.jar"
TALLY_FILES="${TALLY_FILES:-$ROOT_DIR/sample-data}"
TALLY_PORT="${TALLY_PORT:-8080}"
FORCE_BUILD=false

usage() {
  echo "Usage: $(basename "$0") [--build] [--data <path> | <path>]"
  echo ""
  echo "Starts the packaged Tally production server."
  echo "Defaults to '$ROOT_DIR/sample-data' (overridable via CLI argument or TALLY_FILES env var)."
  echo ""
  echo "Options:"
  echo "  --build         Force rebuilding the fat JAR before starting"
  echo "  --data <path>   Path to YAML budget data directory"
  echo "  -h, --help      Show this help message"
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    -h|--help)
      usage
      exit 0
      ;;
    --build)
      FORCE_BUILD=true
      shift
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

if [[ "$FORCE_BUILD" == "true" || ! -f "$JAR_PATH" ]]; then
  "$ROOT_DIR/build.sh"
fi

if [[ ! -d "$TALLY_FILES" ]]; then
  echo "Error: TALLY_FILES directory '$TALLY_FILES' does not exist." >&2
  echo "Pass a data directory via '--data /path/to/data', positional argument, or TALLY_FILES env var." >&2
  exit 1
fi

echo "Starting Tally production server on http://localhost:$TALLY_PORT (data: $TALLY_FILES)"
exec env TALLY_FILES="$TALLY_FILES" TALLY_PORT="$TALLY_PORT" java -jar "$JAR_PATH"
