# Tally client side

## Development

Run the Vite dev server (proxies `/graphql`, `/subscriptions`, `/graphiql`, and `/sdl` to Ktor on `:8080`):

```bash
pnpm --filter @tally/client dev
```

Or start both frontend and backend together from the repo root with `./dev.sh` (or `pnpm dev`).

## Build

```bash
pnpm --filter @tally/client build
```

## Tests

```bash
pnpm --filter @tally/client test
```

## GraphQL

To regenerate TypeScript GraphQL types using `codegen.yml`:

```bash
pnpm client graphql-codegen
```
