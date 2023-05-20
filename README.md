# tally

Financial ledger based on simple text files

```
gradle eclipse
```

## Conventions

Style guide: https://google.github.io/styleguide/javaguide.html

Line wrapper: https://google.github.io/styleguide/javaguide.html#s4.4-column-limit

## Setup

### Initial

```
npm update
```

Build client side:

```
cd client/hbs
npm run build
```

Start server with

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
 