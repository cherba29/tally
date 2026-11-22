#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "Building frontend and packaging Tally fat JAR..."
(cd "$ROOT_DIR/server/kt" && ./gradlew buildFatJar)

echo ""
echo "Build complete: $ROOT_DIR/server/kt/build/libs/tally-all.jar"
