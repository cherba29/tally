# tally

Personal financial ledger based on simple text (yaml) files.

There are many other similar tools (see https://plaintextaccounting.org/).
While other tools primary focus is reporting and tracking,
this tool is more about accounting andtracking obligations like making sure
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
pnpm -r install
echo 'TALLY_FILES="<PATH TO DATA YAML FILES>' > client/cli/.env
cp client/cli/.env server/apollo
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
cd server/apollo
node dist/server
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

## Bazel

Running CLI

```
 bazel run //client/cli:tally_cli  --  generate External --start-month Dec2022
```

## Format files

```
 bazel run @aspect_rules_format//format
```
