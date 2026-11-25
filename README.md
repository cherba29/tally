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
(cd client && pnpm install)
```

> By default, `./dev.sh` and `./run.sh` load the bundled [`sample-data`](sample-data/README.md) ledger so you can try Tally immediately. Pass your own data directory via `--data /path/to/data` (or positional argument / `TALLY_FILES` env var).

Test the client and generate a code coverage report:

```bash
cd client && pnpm test
```

Running `pnpm test` executes the frontend test suite in headless Chromium via `@web/test-runner` with built-in V8 code coverage enabled (configured in [`client/web-test-runner.config.mjs`](client/web-test-runner.config.mjs)):

- Prints a coverage summary (statements, branches, functions, lines) directly to the terminal and enforces minimum thresholds (60% statements/functions/lines, 30% branches).
- Writes the interactive HTML report to `client/coverage/lcov-report/index.html` and raw LCOV data to `client/coverage/lcov.info`.

Open the HTML report in your browser:

```bash
xdg-open client/coverage/lcov-report/index.html   # Linux
# or: open client/coverage/lcov-report/index.html # macOS
```

## Run

### Development mode (auto-reload UI & backend)

Starts Ktor with autoreload (`:8080`), continuous Kotlin compilation, and the Vite dev server (`:5173`, proxying `/graphql`, `/subscriptions`, `/graphiql`, and `/sdl` to Ktor) with automatic browser reload whenever the UI changes:

```bash
./dev.sh
# or with your own YAML data directory:
./dev.sh /path/to/data
./dev.sh --data /path/to/data
```

Open http://localhost:5173 (defaults to `./sample-data`; also supports `TALLY_FILES=/path/to/data ./dev.sh`).

### Production mode (single fat JAR with embedded UI)

Build the frontend with Vite and package frontend + backend into a single executable JAR (`server/kt/build/libs/tally-all.jar`):

```bash
./build.sh
```

Run the packaged server (builds automatically if the JAR doesn't exist; pass `--build` to force a rebuild):

```bash
./run.sh
# or with your own YAML data directory:
./run.sh /path/to/data
./run.sh --build --data /path/to/data
# or directly:
TALLY_FILES=./sample-data java -jar server/kt/build/libs/tally-all.jar
```

Access the app at http://localhost:8080, GraphiQL at http://localhost:8080/graphiql, and SDL at http://localhost:8080/sdl.

## Sample Data

See [`sample-data/README.md`](sample-data/README.md) for a walkthrough of the bundled 5-month double-entry example ledger (checking, credit card, payroll, and expenses) and screenshots of the UI.

## CLI

The CLI commands (`generate`, `report`, `transactions`, `unaccounted`, `summary`, `transfers-summary`, `csv-to-transfers`) are packaged into `tally-all.jar` and can be invoked directly without Gradle startup overhead via `./tally.sh` (defaults to `./sample-data` if `TALLY_FILES` / `TALLY_PATH` is unset; builds the JAR automatically if missing, or pass `--build` to force a rebuild):

```bash
./tally.sh --help
./tally.sh summary alex/internal --start-month Jul2026 --end-month Nov2026
TALLY_FILES=/path/to/data ./tally.sh unaccounted

# Or directly from the packaged fat JAR:
TALLY_FILES=./sample-data java -jar server/kt/build/libs/tally-all.jar cli summary alex/internal --start-month Jul2026 --end-month Nov2026
```

During Kotlin development you can also invoke the CLI via Gradle:

```bash
cd server/kt
TALLY_FILES=../../sample-data ./gradlew runTally --args="summary alex/internal --start-month Jul2026 --end-month Nov2026"
```

## CI/CD

GitHub Actions runs automatically via [`.github/workflows/ci.yml`](.github/workflows/ci.yml) on pushes and pull requests to `master`:

- **Client**: installs workspace dependencies with `pnpm --frozen-lockfile`, runs the Vite production build, and executes all `web-test-runner` tests in headless Chromium.
- **Server**: runs the full Kotlin/Ktor test suite (`./gradlew test`), packages the standalone fat JAR (`tally-all.jar` with embedded UI), and uploads the JAR and JaCoCo coverage report as workflow artifacts.
- **Release (CD)**: [`.github/workflows/release.yml`](.github/workflows/release.yml) publishes GitHub Releases with `tally-all.jar`, its SHA-256 checksum, and release notes extracted from [`CHANGELOG.md`](CHANGELOG.md). Trigger it either by running the **Release** workflow manually in the GitHub Actions UI (defaults to the version in `client/package.json` and creates the `v<version>` tag automatically) or by pushing a version tag (e.g., `git tag v0.4.0 && git push origin v0.4.0`).

## Update dependencies

List outdated client packages:
```bash
cd client && pnpm outdated
```

```bash
cd client && pnpm update --latest
```
