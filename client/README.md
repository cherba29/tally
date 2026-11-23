# Tally client side

## Setup

```bash
pnpm install
```

## Development

Run the Vite dev server (proxies `/graphql`, `/subscriptions`, `/graphiql`, and `/sdl` to Ktor on `:8080`):

```bash
pnpm dev
```

Or start both frontend and backend together from the repo root with `./dev.sh`.

## Build

```bash
pnpm build
```

## Tests

```bash
pnpm test
```

## GraphQL

To regenerate TypeScript GraphQL types using `codegen.yml`:

```bash
pnpm graphql-codegen
```
