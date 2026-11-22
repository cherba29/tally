#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TALLY_FILES="${TALLY_FILES:-$SCRIPT_DIR/../../data}"

cd "$SCRIPT_DIR"
TALLY_FILES="$TALLY_FILES" ./gradlew run --info
