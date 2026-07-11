# Tally client side

## Setup

To build js bundle use.

```
npm run build
```

also via bazel

```
bazel build //client/hbs:bundle
```

and then running kotlin server via
```
TALLY_FILES=../../data TALLY_CLIENT_BUNDLE=../../bazel-bin/client/hbs ./gradlew run -t --info
```

the web app can be rebuilt without restarting the server, just reload page.


## Development

```
npm run test
```

## Graphql

To regenerate ts graphql schema, using codegen.yaml config.
```
$ pnpm client graphql-codegen
```
