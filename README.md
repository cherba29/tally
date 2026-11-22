# tally

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
pnpm -r install
```

Test the client:

```bash
pnpm client test
```

## Run

### Development mode (auto-reload UI & backend)

Starts Ktor with autoreload (`:8080`), continuous Kotlin compilation, and the Vite dev server (`:5173`, proxying `/graphql`, `/subscriptions`, `/graphiql`, and `/sdl` to Ktor) with automatic browser reload whenever the UI changes:

```bash
./dev.sh
# or
pnpm dev
```

Open http://localhost:5173 (override data path with `TALLY_FILES=/path/to/data ./dev.sh`).

### Production mode (single fat JAR with embedded UI)

Build the frontend with Vite and package frontend + backend into a single executable JAR (`server/kt/build/libs/tally-all.jar`):

```bash
./build.sh
# or
pnpm build
```

Run the packaged server (builds automatically if the JAR doesn't exist; pass `--build` to force a rebuild):

```bash
./run.sh
# or
pnpm start
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

## Update dependencies

List outdate packeges
```
pnpm outdated --recursive
```

```
pnpm update --latest --recursive
```
