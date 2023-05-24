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
sudo apt-get install bazel
sudo apt install npm
sudo npm install -g pnpm
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
pnpm apollo build
```

## Run

### Start (nodejs) server

```
bazel run //server/apollo:server
```

or

```
pnpm apollo start
```

access server via http://localhost:4000/app/js_summary.html

### Start (java) server -- deprecated

```
cd tally
gradle -p server/jetty runT --offline
```

Load page for example by navigating to
http://localhost:8080/files/hbs/client/js_summary.html#data

where data is path to yaml files to be parsed. It can be relative to current
server directory which will be in this case `./server/jetty`.

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
