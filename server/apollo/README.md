# GraphQL Tally server.

To run

Assuming in project directory `tally`, the server can be started as

```
cd server/apollo
node dist/server
```

It serves files from the client registering them under `/app` path.
So to load it open http://localhost:4000/app/js_summary.html

This version of the server is [Apollo](https://www.apollographql.com) based.

In a terminal run

```
npm run build
```

wou will get

```
webpack is watching the files…
```

Than in another terminal, start the server:

```
npm run start
```

By default, it will start server at http://localhost:4000/.

In the query/mutation window (left pannel), enter a query like

```
{
  accounts {
    name
  }
}
```

## Regenerate GraphQL typescript types

Make sure server is running and then run

```
npm run generate
```

which uses
[GraphQL Code generator](https://graphql-code-generator.com/docs/getting-started/index).

## Test

```
npm run test:unit -- --coverage
```

## Lint

```
npm run lint
```

## Format

```
npm run prettier-format
```
