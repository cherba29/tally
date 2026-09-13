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

### Start server

and then running kotlin server via
```
TALLY_FILES=../../data TALLY_CLIENT_BUNDLE=../../bazel-bin/client/hbs ./gradlew run -t --info
```

access server via http://localhost:8080 and graphql via http://localhost:8080/graphiql, and schema description at http://localhost:8080/sdl

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
