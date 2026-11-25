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

Or start both frontend and backend together from the repo root with `./tally.sh server --dev`.

## Build

```bash
pnpm build
```

## Tests & Coverage

```bash
pnpm test
```

Tests run in headless Chromium via `@web/test-runner` with built-in V8 coverage enabled (see [`web-test-runner.config.mjs`](web-test-runner.config.mjs)). Each run prints a text summary to the terminal, checks minimum coverage thresholds (60% statements/functions/lines, 30% branches), and writes the full report to:

- `coverage/lcov-report/index.html` — interactive HTML coverage report (e.g., `xdg-open coverage/lcov-report/index.html`)
- `coverage/lcov.info` — LCOV coverage data

## GraphQL

To regenerate TypeScript GraphQL types using `codegen.yml`:

```bash
pnpm graphql-codegen
```
