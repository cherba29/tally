# tally

Personal financial ledger based on simple text (yaml) files.

There are many other similar tools (see https://plaintextaccounting.org/).
While other tools primary focus is reporting and tracking,
this tool is more about month-to-month accounting and tracking obligations like making sure
all the bills are paid, with projections for cashflow for next few months.

It consists of backend micro-service with web-ui interface.

## Setup

### Initial

```
sudo apt install npm
sudo npm install -g pnpm @bazel/bazelisk @bazel/ibazel
```

```
cd tally
ln -s <path data directory> data
pnpm -r install
bazel test //...
```

Build client side:

```
pnpm lib build
pnpm client build
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

```
 bazel run //client/cli:tally_cli  --  generate External --start-month Dec2022
```

or

```
pnpm cli exect generate External --start-month Dec2022
```

## Format files

```
bazel run @aspect_rules_format//format
```

## Update dependencies

List outdate packeges
```
pnpm outdated --recursive
```

```
pnpm update --latest --recursive
```
