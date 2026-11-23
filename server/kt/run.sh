#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TALLY_FILES="${TALLY_FILES:-$SCRIPT_DIR/../../sample-data}"

usage() {
  echo "Usage: $(basename "$0") [--data <path> | <path>]"
  echo ""
  echo "Starts the Kotlin/Ktor backend directly via Gradle."
  echo "Defaults to '$SCRIPT_DIR/../../sample-data' (overridable via CLI argument or TALLY_FILES env var)."
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

cd "$SCRIPT_DIR"
TALLY_FILES="$TALLY_FILES" ./gradlew run --info
