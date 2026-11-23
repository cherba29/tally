# tally

[![CI/CD](https://github.com/cherba29/tally/actions/workflows/ci.yml/badge.svg)](https://github.com/cherba29/tally/actions/workflows/ci.yml)

Personal financial ledger based on simple text (yaml) files.

There are many other similar tools (see https://plaintextaccounting.org/).
While other tools primary focus is reporting and tracking,
this tool is more about month-to-month accounting and tracking obligations like making sure
all the bills are paid, with projections for cashflow for next few months.

It consists of backend micro-service with web-ui interface.

## Setup

### Initial

```bash
sudo apt install npm
sudo npm install -g pnpm
```

```bash
cd tally
ln -s <path data directory> data
(cd client && pnpm install)
```

Test the client:

```bash
cd client && pnpm test
```

## Run

### Development mode (auto-reload UI & backend)

Starts Ktor with autoreload (`:8080`), continuous Kotlin compilation, and the Vite dev server (`:5173`, proxying `/graphql`, `/subscriptions`, `/graphiql`, and `/sdl` to Ktor) with automatic browser reload whenever the UI changes:

```bash
./dev.sh
```

Open http://localhost:5173 (override data path with `TALLY_FILES=/path/to/data ./dev.sh`).

### Production mode (single fat JAR with embedded UI)

Build the frontend with Vite and package frontend + backend into a single executable JAR (`server/kt/build/libs/tally-all.jar`):

```bash
./build.sh
```

Run the packaged server (builds automatically if the JAR doesn't exist; pass `--build` to force a rebuild):

```bash
./run.sh
# or directly:
TALLY_FILES=./data java -jar server/kt/build/libs/tally-all.jar
```

Access the app at http://localhost:8080, GraphiQL at http://localhost:8080/graphiql, and SDL at http://localhost:8080/sdl.

## CLI

The CLI commands (`generate`, `report`, `transactions`, `unaccounted`, `summary`, `transfers-summary`, `csv-to-transfers`) are implemented in Kotlin under `server/kt`:

```bash
cd server/kt
TALLY_PATH=../../data ./gradlew runTally --args="generate External --start-month Dec2022"
```

## CI/CD

GitHub Actions runs automatically via [`.github/workflows/ci.yml`](.github/workflows/ci.yml) on pushes and pull requests to `master`:

- **Client**: installs workspace dependencies with `pnpm --frozen-lockfile`, runs the Vite production build, and executes all `web-test-runner` tests in headless Chromium.
- **Server**: runs the full Kotlin/Ktor test suite (`./gradlew test`), packages the standalone fat JAR (`tally-all.jar` with embedded UI), and uploads the JAR and JaCoCo coverage report as workflow artifacts.
- **Release (CD)**: pushing a version tag (e.g., `git tag v0.4.0 && git push origin v0.4.0`) automatically publishes a GitHub Release with `tally-all.jar` attached.

## Update dependencies

List outdated client packages:
```bash
cd client && pnpm outdated
```

```bash
cd client && pnpm update --latest
```
