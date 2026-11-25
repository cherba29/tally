#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_PATH="$ROOT_DIR/server/kt/build/libs/tally-all.jar"
FORCE_BUILD=false

usage() {
  cat <<EOF
Usage: $(basename "$0") [--build] <command> [options...]

Commands:
  build                 Build the frontend and package the standalone fat JAR
  server [options]      Start the Tally web server (production JAR by default)
  cli [args...]         Run Tally CLI subcommands via the fat JAR

Global options:
  --build               Force rebuilding the fat JAR before running 'server' or 'cli'
  -h, --help            Show this help message

Examples:
  $(basename "$0") build
  $(basename "$0") server
  $(basename "$0") server --dev /path/to/data
  $(basename "$0") server --build --data /path/to/data
  $(basename "$0") server --backend-only --data /path/to/data
  $(basename "$0") cli --help
  $(basename "$0") cli summary alex/internal --start-month Jul2026 --end-month Nov2026
  $(basename "$0") cli generate ...

Run '$(basename "$0") <command> --help' for command-specific options.
EOF
}

server_usage() {
  cat <<EOF
Usage: $(basename "$0") server [options] [<data-path>]

Starts the Tally web server.
Defaults to '$ROOT_DIR/sample-data' (overridable via --data, positional argument, or TALLY_FILES env var).

Options:
  --dev                 Run hot-reloading dev mode (Ktor autoreload + continuous compile + Vite)
  --backend-only        Run only the Kotlin/Ktor backend directly via Gradle
  --build               Force rebuilding the fat JAR before starting production server
  --data <path>         Path to YAML budget data directory
  --port <port>         Backend HTTP port (default: \$TALLY_PORT or 8080)
  --dev-port <port>     Vite dev server port in --dev mode (default: \$TALLY_DEV_PORT or 5173)
  -h, --help            Show this help message
EOF
}

build_usage() {
  cat <<EOF
Usage: $(basename "$0") build

Builds the frontend with Vite and packages frontend + backend into:
  $JAR_PATH
EOF
}

build_jar() {
  echo "Building frontend and packaging Tally fat JAR..."
  (cd "$ROOT_DIR/server/kt" && ./gradlew buildFatJar)
  echo ""
  echo "Build complete: $JAR_PATH"
}

ensure_jar() {
  if [[ "$FORCE_BUILD" == "true" || ! -f "$JAR_PATH" ]]; then
    build_jar
  fi
}

cmd_build() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      -h|--help)
        build_usage
        exit 0
        ;;
      *)
        echo "Error: Unknown option '$1' for 'build'." >&2
        build_usage >&2
        exit 1
        ;;
    esac
  done
  build_jar
}

run_dev_server() {
  local data_dir="$1"
  local port="$2"
  local dev_port="$3"
  local pids=()

  cleanup() {
    trap - EXIT INT TERM
    echo ""
    echo "Stopping dev servers..."
    for pid in "${pids[@]:-}"; do
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
  echo "  Data directory : $data_dir"
  echo "  Backend (Ktor) : http://localhost:$port (autoreload enabled)"
  echo "  Frontend (Vite): http://localhost:$dev_port (auto-reloads browser on UI changes)"
  echo ""

  # 1. Continuous Kotlin compilation so Ktor autoreloads changed classes on save
  (
    cd "$ROOT_DIR/server/kt"
    ./gradlew -t classes -PskipFrontend=true --console=plain
  ) &
  pids+=($!)

  # 2. Ktor backend server with autoreload enabled
  (
    cd "$ROOT_DIR/server/kt"
    TALLY_FILES="$data_dir" \
    TALLY_PORT="$port" \
    KTOR_DEVELOPMENT=true \
      ./gradlew run -PskipFrontend=true --console=plain
  ) &
  pids+=($!)

  # 3. Vite dev server proxying /graphql and /subscriptions to Ktor
  (
    cd "$ROOT_DIR/client"
    TALLY_BACKEND_PORT="$port" \
    TALLY_DEV_PORT="$dev_port" \
      pnpm dev
  ) &
  pids+=($!)

  wait -n "${pids[@]}"
}

cmd_server() {
  local mode="prod"
  local data_dir="${TALLY_FILES:-${TALLY_PATH:-$ROOT_DIR/sample-data}}"
  local port="${TALLY_PORT:-8080}"
  local dev_port="${TALLY_DEV_PORT:-5173}"
  local positional_set=false

  while [[ $# -gt 0 ]]; do
    case "$1" in
      -h|--help)
        server_usage
        exit 0
        ;;
      --dev)
        if [[ "$mode" == "backend-only" ]]; then
          echo "Error: --dev and --backend-only cannot be used together." >&2
          exit 1
        fi
        mode="dev"
        shift
        ;;
      --backend-only)
        if [[ "$mode" == "dev" ]]; then
          echo "Error: --dev and --backend-only cannot be used together." >&2
          exit 1
        fi
        mode="backend-only"
        shift
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
        data_dir="$2"
        shift 2
        ;;
      --data=*)
        data_dir="${1#*=}"
        shift
        ;;
      --port)
        if [[ $# -lt 2 ]]; then
          echo "Error: --port requires a port number." >&2
          exit 1
        fi
        port="$2"
        shift 2
        ;;
      --port=*)
        port="${1#*=}"
        shift
        ;;
      --dev-port)
        if [[ $# -lt 2 ]]; then
          echo "Error: --dev-port requires a port number." >&2
          exit 1
        fi
        dev_port="$2"
        shift 2
        ;;
      --dev-port=*)
        dev_port="${1#*=}"
        shift
        ;;
      -*)
        echo "Error: Unknown option '$1' for 'server'." >&2
        server_usage >&2
        exit 1
        ;;
      *)
        if [[ "$positional_set" == "true" ]]; then
          echo "Error: Unexpected argument '$1'." >&2
          server_usage >&2
          exit 1
        fi
        data_dir="$1"
        positional_set=true
        shift
        ;;
    esac
  done

  if [[ ! -d "$data_dir" ]]; then
    echo "Error: TALLY_FILES directory '$data_dir' does not exist." >&2
    echo "Pass a data directory via '--data /path/to/data', positional argument, or TALLY_FILES env var." >&2
    exit 1
  fi

  case "$mode" in
    dev)
      run_dev_server "$data_dir" "$port" "$dev_port"
      ;;
    backend-only)
      echo "Starting Tally Kotlin/Ktor backend via Gradle on http://localhost:$port (data: $data_dir)"
      cd "$ROOT_DIR/server/kt"
      exec env TALLY_FILES="$data_dir" TALLY_PORT="$port" ./gradlew run --info
      ;;
    prod)
      ensure_jar
      echo "Starting Tally production server on http://localhost:$port (data: $data_dir)"
      exec env TALLY_FILES="$data_dir" TALLY_PORT="$port" java -jar "$JAR_PATH"
      ;;
  esac
}

cmd_cli() {
  if [[ "${1:-}" == "--build" ]]; then
    FORCE_BUILD=true
    shift
  fi

  ensure_jar >&2

  if [[ -z "${TALLY_FILES:-}" && -z "${TALLY_PATH:-}" && -d "$ROOT_DIR/sample-data" ]]; then
    export TALLY_FILES="$ROOT_DIR/sample-data"
  fi

  exec java -jar "$JAR_PATH" cli "$@"
}

if [[ $# -eq 0 ]]; then
  usage
  exit 0
fi

while [[ $# -gt 0 ]]; do
  case "$1" in
    --build)
      FORCE_BUILD=true
      shift
      ;;
    -h|--help|help)
      usage
      exit 0
      ;;
    build)
      shift
      cmd_build "$@"
      exit 0
      ;;
    server)
      shift
      cmd_server "$@"
      exit 0
      ;;
    cli)
      shift
      cmd_cli "$@"
      exit 0
      ;;
    *)
      echo "Error: Unknown command '$1'." >&2
      echo "" >&2
      usage >&2
      exit 1
      ;;
  esac
done

usage
