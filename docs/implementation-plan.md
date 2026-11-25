# Tally Implementation Plan: Architecture, Maintainability & UX Improvements

This document outlines prioritized improvements to make Tally more readable, easier to maintain, and smoother to use across the Kotlin/Ktor backend (`server/kt`), the Lit/TypeScript frontend (`client`), the CLI, and CI/build tooling.

---

## Phase 1: Repository Hygiene, Tooling & Quick Wins

Low-risk cleanup that reduces noise, removes dead code, and establishes automated quality gates.

### 1.1 Remove Legacy & Misplaced Files — Done
- [x] Removed obsolete `client/karma.conf.js` and the unused `"karma"` script in [`client/package.json`](../client/package.json).
- [x] Deleted untracked legacy artifacts under `client/public/` (stale Webpack/Handlebars/Jetty `.d.ts` files).
- [x] Removed stray tracked `server/kt/skills/system/run-workflow/SKILL.md` (`server/kt/kt.iml` is already gitignored via `server/kt/.gitignore`).
- [x] Moved test-only helpers (`toObjectNode.kt` and `toSnapshot.kt`) from `server/kt/src/main/kotlin/com/cherba29/tally/testing/` to `server/kt/src/test/kotlin/com/cherba29/tally/testing/` so they are excluded from `tally-all.jar`.

### 1.2 Unify Environment Variables & Config Defaults — Done
- [x] Standardized on `TALLY_FILES` across the web server, shell scripts ([`dev.sh`](../dev.sh), [`run.sh`](../run.sh), [`server/kt/run.sh`](../server/kt/run.sh)), and all CLI commands via shared [`TallyPathOption.kt`](../server/kt/src/main/kotlin/com/cherba29/tally/cli/cmds/TallyPathOption.kt), while preserving `TALLY_PATH` as a backwards-compatible fallback.
- [x] Replaced the placeholder default `path: '$TALLY_FILES:123'` in [`server/kt/src/main/resources/application.yaml`](../server/kt/src/main/resources/application.yaml) with optional `$?TALLY_FILES` + `$?TALLY_PATH` and `resolveTallyDataPath()` in [`Application.kt`](../server/kt/src/main/kotlin/com/cherba29/tally/Application.kt) (falls back to `./sample-data` if present, otherwise fails with a clear configuration message).
- [x] Updated CLI documentation in [`README.md`](../README.md) and [`server/kt/README.md`](../server/kt/README.md), and added unit tests covering primary, fallback, default, and error cases.

### 1.3 Enforce Static Checks in CI — Done
- [x] Fixed client `tsc --noEmit` errors (enabled `skipLibCheck` in [`client/tsconfig.json`](../client/tsconfig.json) for duplicate transitive `@types/chai`, moved `errorPolicy: 'all'` to per-query options in [`client/src/api.ts`](../client/src/api.ts), and fixed strict test types in `summary-table.test.ts` and `tally-app.test.ts`) and added `"typecheck"` script to [`client/package.json`](../client/package.json).
- [x] Fixed all client `pnpm lint` findings (`prefer-const`, explicit D3 types in [`transfers-summary-tooltip.ts`](../client/src/components/transfers-summary-tooltip.ts), `node:process` import in [`web-test-runner.config.mjs`](../client/web-test-runner.config.mjs), `string[]` wrapper types, and Chai expression override in [`client/eslint.config.mjs`](../client/eslint.config.mjs)).
- [x] Formatted Kotlin sources with `./gradlew ktlintFormat`, renamed `toSnapshot.kt`/`toObjectNode.kt` to PascalCase (`ToSnapshot.kt`/`ToObjectNode.kt`), and resolved remaining manual `ktlintCheck` violations (`ToGql.kt`, `ApplicationTest.kt`, `BudgetTest.kt`).
- [x] Wired `pnpm typecheck`, `pnpm lint`, and `./gradlew ktlintCheck` into [`.github/workflows/ci.yml`](../.github/workflows/ci.yml).

### 1.4 Small Reliability & Hygiene Fixes — Done
- [x] Updated `StatusPages` in [`Application.kt`](../server/kt/src/main/kotlin/com/cherba29/tally/Application.kt) to return `500 Internal Server Error` instead of `400 Bad Request` for unhandled exceptions (verified via `ApplicationTest`).
- [x] Removed `HelloWorldQuery` from [`Application.kt`](../server/kt/src/main/kotlin/com/cherba29/tally/Application.kt) and the SDL snapshot ([`ApplicationTest.ss`](../server/kt/src/test/kotlin/com/cherba29/tally/ApplicationTest.ss)); warm-up now executes `query WarmUp { __typename }`.
- [x] Fixed backend log typos (`startMonth=` in [`TableService.kt`](../server/kt/src/main/kotlin/com/cherba29/tally/TableService.kt) and [`SummaryService.kt`](../server/kt/src/main/kotlin/com/cherba29/tally/SummaryService.kt), `"statement query"` in [`StatementService.kt`](../server/kt/src/main/kotlin/com/cherba29/tally/StatementService.kt)) and removed debug `console.log` statements from `client/src/`.
- [x] Removed rethrow inside `reloadTable().catch(...)` in [`tally-app.ts`](../client/src/components/tally-app.ts) so rejected table loads update `errorMessage` and clear popups without unhandled promise rejections (covered in [`tally-app.test.ts`](../client/src/components/tally-app.test.ts)).

---

## Phase 2: Backend Architecture & Readability (`server/kt`)

### 2.1 Deduplicate Transfer Resolution & Validation in `BudgetBuilder` — Done
- **File**: [`server/kt/src/main/kotlin/com/cherba29/tally/data/builder/BudgetBuilder.kt`](../server/kt/src/main/kotlin/com/cherba29/tally/data/builder/BudgetBuilder.kt)
- [x] Replaced duplicate validation/lookup logic across `validateTransfers()` and `buildTransactionStatements()` with a single-pass `resolveTransfers()` returning `List<ResolvedTransfer>` (preserving `validate()` public API and `BudgetLoadingException` file/line context).
- [x] Passed the resolved transfers directly into `buildTransactionStatements()` so transfer resolution and validation cannot diverge.

### 2.2 Share Query & Aggregation Logic Between GraphQL Services and CLI — Done
- **Files**: [`BudgetQueries.kt`](../server/kt/src/main/kotlin/com/cherba29/tally/data/BudgetQueries.kt), [`SummaryService.kt`](../server/kt/src/main/kotlin/com/cherba29/tally/SummaryService.kt), [`StatementService.kt`](../server/kt/src/main/kotlin/com/cherba29/tally/StatementService.kt), [`TableService.kt`](../server/kt/src/main/kotlin/com/cherba29/tally/TableService.kt), [`cli/cmds/Summary.kt`](../server/kt/src/main/kotlin/com/cherba29/tally/cli/cmds/Summary.kt)
- [x] Extracted `Budget.summary(...)`, `Budget.statement(...)`, and `Budget.table(...)` into [`BudgetQueries.kt`](../server/kt/src/main/kotlin/com/cherba29/tally/data/BudgetQueries.kt) (matching `Budget.transfersSummary(...)`).
- [x] Refactored `SummaryService`, `StatementService`, `TableService`, and CLI `Summary` into thin adapters over the shared `Budget` queries so CLI and GraphQL share identical aggregation and closed-account exclusion (verified in [`SummaryTest.kt`](../server/kt/src/test/kotlin/com/cherba29/tally/cli/cmds/SummaryTest.kt)).

### 2.3 Use `suspend` GraphQL Resolvers Instead of `runBlocking` — Done
- **Files**: [`TableService.kt`](../server/kt/src/main/kotlin/com/cherba29/tally/TableService.kt), [`SummaryService.kt`](../server/kt/src/main/kotlin/com/cherba29/tally/SummaryService.kt), [`StatementService.kt`](../server/kt/src/main/kotlin/com/cherba29/tally/StatementService.kt)
- [x] Converted `table`, `summary`, `transfersSummary`, and `statement` resolvers to `suspend fun` and removed all `runBlocking` calls from `server/kt/src/main`.

### 2.4 Split Plumbing Out of `Application.kt` — Done
- **Files**: [`Application.kt`](../server/kt/src/main/kotlin/com/cherba29/tally/Application.kt), [`RequestLoggingPlugin.kt`](../server/kt/src/main/kotlin/com/cherba29/tally/server/RequestLoggingPlugin.kt), [`QueryLoggingInstrumentation.kt`](../server/kt/src/main/kotlin/com/cherba29/tally/server/QueryLoggingInstrumentation.kt), [`LoggingDataFetcherExceptionHandler.kt`](../server/kt/src/main/kotlin/com/cherba29/tally/server/LoggingDataFetcherExceptionHandler.kt)
- [x] Extracted `RequestLoggingPlugin`, `QueryLoggingInstrumentation`, and `LoggingDataFetcherExceptionHandler` into dedicated files under `com.cherba29.tally.server`, leaving `Application.kt` focused on server wiring and routing.

---

## Phase 3: Frontend Architecture & Maintainability (`client/src`)

### 3.1 Declarative State & Row Folding in Lit — Done
- **Files**: [`client/src/components/summary-table.ts`](../client/src/components/summary-table.ts), [`client/src/components/tally-app.ts`](../client/src/components/tally-app.ts)
- [x] Replaced imperative `childTableRows` mutation and DOM `style.display` toggling in `SummaryTable` with `@state() collapsedIds`/`expandedIds` and a pure O(N) indent stack in `render()`, preserving collapsed groups across live reloads and keeping inactive groups collapsed initially.
- [x] Simplified `months` and `rows` in `SummaryTable` to `@property({attribute: false})`.
- [x] Converted `TallyApp` UI fields to Lit `@state()` decorators, removed manual `requestUpdate()` calls, and deleted dead `popupMap` code.

### 3.2 Discriminated Union for `PopupData` — Done
- **Files**: [`client/src/utils.ts`](../client/src/utils.ts), [`client/src/components/tally-app.ts`](../client/src/components/tally-app.ts)
- [x] Added `kind: 'summary' | 'transfers-summary' | 'statement' | 'account'` discriminant to all `PopupData` interfaces in [`client/src/utils.ts`](../client/src/utils.ts).
- [x] Replaced `'...' in this.popupData` checks and the unreachable fallback throw in `TallyApp.tooltipFragment()` with an exhaustive `switch (this.popupData.kind)`.

### 3.3 Simplify GraphQL Client (`api.ts`) — Done
- **Files**: [`client/src/api.ts`](../client/src/api.ts), [`client/package.json`](../client/package.json), [`client/pnpm-lock.yaml`](../client/pnpm-lock.yaml), [`client/src/api.test.ts`](../client/src/api.test.ts)
- [x] Replaced `@apollo/client` with a lightweight typed `fetch('/graphql', ...)` `executeQuery()` helper in [`client/src/api.ts`](../client/src/api.ts) while retaining `@graphql-codegen` generated types (`gql_types.ts`).
- [x] Removed `@apollo/client` from [`client/package.json`](../client/package.json), moved `graphql` to `devDependencies`, and regenerated [`client/pnpm-lock.yaml`](../client/pnpm-lock.yaml).
- [x] Reduced the production JS bundle from **344.5 kB (103.6 kB gzip, 631 modules)** to **148.7 kB (46.3 kB gzip, 70 modules)** — a **57% reduction** — and added error-handling unit tests in [`client/src/api.test.ts`](../client/src/api.test.ts).

---

## Phase 4: Usability & Day-to-Day UX Polish

### 4.1 Clearer Date Toolbar & Owner Tabs — Done
- **File**: [`client/src/components/tally-app.ts`](../client/src/components/tally-app.ts)
- [x] Replaced the fixed unlabeled `<< < > >>` toolbar with a sticky flex `<header class="toolbar">` featuring labeled End/Start/Shift control groups (`+1y`, `+1m`, `-1m`, `-1y`), descriptive `title` tooltips, and a `Today` reset button (`resetRange()`).
- [x] Updated owner tabs to render full owner names in a `<nav class="owner-tabs" role="tablist">` with active tab styling and `aria-selected`, and conditioned the error banner so it takes no space when empty.

### 4.2 Sticky Table Headers & Account Column — Done
- **Files**: [`client/src/components/summary-table.ts`](../client/src/components/summary-table.ts), [`client/src/components/tally-app.ts`](../client/src/components/tally-app.ts)
- [x] Applied `position: sticky; top: 0` to `<thead>` and `position: sticky; left: 0` to `thead th:first-child` and `tbody td:first-child` (preserving `#ffd` background on total rows and `#f6f8fa` on headers with `border-collapse: separate`) and made `.tabcontent` scrollable so headers and account names stay pinned while scrolling wide/tall ledgers.
- [x] Added computed-style assertions in [`summary-table.test.ts`](../client/src/components/summary-table.test.ts).

### 4.3 Popup Ergonomics & Copy Feedback — Done
- **Files**: [`client/src/components/tally-app.ts`](../client/src/components/tally-app.ts), [`client/src/components/summary-table.ts`](../client/src/components/summary-table.ts), [`client/src/components/balance-tooltip.ts`](../client/src/components/balance-tooltip.ts)
- [x] Anchored fixed-position popups to the clicked cell's `DOMRect` (`anchorRect`), awaiting child tooltip render + `ResizeObserver` to flip above/left near viewport edges without flicker or covering the clicked cell, and constrained `.toolTip` with `max-width`/`max-height` + `overflow: auto`.
- [x] Added `Escape`-key and click-outside dismissal (using `composedPath()` and ignoring the originating cell click) with full listener cleanup in `disconnectedCallback()`.
- [x] Dispatched bubbling `copied` events from [`summary-table.ts`](../client/src/components/summary-table.ts) and [`balance-tooltip.ts`](../client/src/components/balance-tooltip.ts) and rendered an auto-dismissing status toast in [`tally-app.ts`](../client/src/components/tally-app.ts).

### 4.4 Live-Reload Status Indicator — Done
- **Files**: [`client/src/api.ts`](../client/src/api.ts), [`client/src/components/tally-app.ts`](../client/src/components/tally-app.ts)
- [x] Exported `ConnectionState` (`'connected' | 'reconnecting' | 'disconnected'`) and added an optional `onStateChange` callback to `BackendClient.subscribeToUpdates()` emitting deduplicated WebSocket lifecycle transitions (covered in [`api.test.ts`](../client/src/api.test.ts)).
- [x] Added a compact live-reload status badge (`.live-status`) with colored dot, connection label (`Live` / `Reconnecting` / `Disconnected`), and last-reload `HH:MM:SS` timestamp to the `<header class="toolbar">` in [`tally-app.ts`](../client/src/components/tally-app.ts) (covered in [`tally-app.test.ts`](../client/src/components/tally-app.test.ts)).

### 4.5 Fast CLI Invocation
- **Files**: Root script (e.g., `./tally.sh`) and/or unified fat-JAR entry point in [`server/kt`](../server/kt)
- **Action**: Provide a lightweight `./tally.sh` wrapper—or support `java -jar tally-all.jar cli <subcommand>`—so CLI tools (`unaccounted`, `csv-to-transfers`, `generate`, `summary`, `report`) run immediately without Gradle startup overhead.

---

## Verification Checklist (Per Phase)

- **Client**: `cd client && pnpm build && pnpm test && pnpm lint && npx tsc --noEmit`
- **Server**: `cd server/kt && ./gradlew test ktlintCheck buildFatJar`
- **End-to-End**: Run `./dev.sh` and `./run.sh` against `./sample-data`, verify live YAML reload, collapsed-row persistence, popup interactions, and CLI commands.
