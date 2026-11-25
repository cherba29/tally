# Tally Implementation Plan: Architecture, Maintainability & UX Improvements

This document outlines prioritized improvements to make Tally more readable, easier to maintain, and smoother to use across the Kotlin/Ktor backend (`server/kt`), the Lit/TypeScript frontend (`client`), the CLI, and CI/build tooling.

---

## Phase 1: Repository Hygiene, Tooling & Quick Wins

Low-risk cleanup that reduces noise, removes dead code, and establishes automated quality gates.

### 1.1 Remove Legacy & Misplaced Files
- Remove obsolete [`client/karma.conf.js`](../client/karma.conf.js) and the unused `"karma"` script in [`client/package.json`](../client/package.json).
- Delete untracked legacy artifacts under `client/public/` (stale Webpack/Handlebars/Jetty `.d.ts` files).
- Remove stray tracked files (`server/kt/skills/system/run-workflow/SKILL.md` and `server/kt/kt.iml` if IDE project files are not intended to be committed).
- Move test-only helpers (`server/kt/src/main/kotlin/com/cherba29/tally/testing/toObjectNode.kt` and `toSnapshot.kt`) from `src/main` to `src/test` so they are not packaged into `tally-all.jar`.

### 1.2 Unify Environment Variables & Config Defaults
- Standardize on `TALLY_FILES` across both the web server and CLI commands (currently CLI commands such as [`Summary.kt`](../server/kt/src/main/kotlin/com/cherba29/tally/cli/cmds/Summary.kt) read `TALLY_PATH`, while [`application.yaml`](../server/kt/src/main/resources/application.yaml), [`dev.sh`](../dev.sh), and [`run.sh`](../run.sh) use `TALLY_FILES`). Support the legacy name as a fallback for backwards compatibility.
- Replace the placeholder default `path: '$TALLY_FILES:123'` in [`server/kt/src/main/resources/application.yaml`](../server/kt/src/main/resources/application.yaml) with a sensible default (e.g., `./sample-data` or a clear missing-configuration error).

### 1.3 Enforce Static Checks in CI
- Fix existing `tsc --noEmit` errors in `client/` (e.g., `@esm-bundle/chai` vs. `@types/chai` type conflicts) and `pnpm lint` findings.
- Run `tsc --noEmit`, `pnpm lint`, and `./gradlew ktlintCheck` in [`.github/workflows/ci.yml`](../.github/workflows/ci.yml) so formatting and type regressions are caught automatically on every PR.

### 1.4 Small Reliability & Hygiene Fixes
- In [`Application.kt`](../server/kt/src/main/kotlin/com/cherba29/tally/Application.kt), return `HttpStatusCode.InternalServerError` (`500`) instead of `400 Bad Request` for unhandled exceptions in `StatusPages`.
- Remove unused `HelloWorldQuery` from the GraphQL schema (warm up GraphQL via schema initialization or an introspection query instead of exposing `hello: String!`).
- Fix minor log typos (`startMont=` in `TableService`/`SummaryService`, `"Error while processing table query"` in `StatementService`) and remove debug `console.log('###', ...)` calls in `client/src/`.
- In [`tally-app.ts`](../client/src/components/tally-app.ts), stop rethrowing inside `reloadTable().catch(...)` after updating `errorMessage`, preventing noisy unhandled promise rejections in the browser console.

---

## Phase 2: Backend Architecture & Readability (`server/kt`)

### 2.1 Deduplicate Transfer Resolution & Validation in `BudgetBuilder`
- **File**: [`server/kt/src/main/kotlin/com/cherba29/tally/data/builder/BudgetBuilder.kt`](../server/kt/src/main/kotlin/com/cherba29/tally/data/builder/BudgetBuilder.kt)
- **Problem**: `validateTransfers()` and `buildTransactionStatements()` duplicate target-account lookup, ambiguity detection, and closed-month validation while throwing different exception types (`BudgetLoadingException` vs. `IllegalArgumentException`).
- **Action**:
  - Introduce a `ResolvedTransfer(fromNode, toNode, month, balance, description, ...)` step that validates and resolves each `TransferRecord` once.
  - Reuse the resolved list directly when building `MonthTransactionStatementBuilder` entries.

### 2.2 Share Query & Aggregation Logic Between GraphQL Services and CLI
- **Files**: [`SummaryService.kt`](../server/kt/src/main/kotlin/com/cherba29/tally/SummaryService.kt), [`StatementService.kt`](../server/kt/src/main/kotlin/com/cherba29/tally/StatementService.kt), [`TableService.kt`](../server/kt/src/main/kotlin/com/cherba29/tally/TableService.kt), [`cli/cmds/Summary.kt`](../server/kt/src/main/kotlin/com/cherba29/tally/cli/cmds/Summary.kt)
- **Problem**: `SummaryService` and `cli/cmds/Summary.kt` duplicate multi-month `MonthRangeSummaryStatementBuilder` aggregation and have diverged (`SummaryService` excludes closed statements via `!budget.isClosed(...)`, while the CLI does not).
- **Action**:
  - Move domain query functions (`summary(...)`, `statement(...)`, `table(...)`) onto `Budget` or a dedicated `BudgetQueries` helper (matching the existing `budget.transfersSummary(...)` pattern).
  - Keep GraphQL resolvers and Clikt commands as thin presentation/formatting adapters.

### 2.3 Use `suspend` GraphQL Resolvers Instead of `runBlocking`
- **Files**: [`TableService.kt`](../server/kt/src/main/kotlin/com/cherba29/tally/TableService.kt), [`SummaryService.kt`](../server/kt/src/main/kotlin/com/cherba29/tally/SummaryService.kt), [`StatementService.kt`](../server/kt/src/main/kotlin/com/cherba29/tally/StatementService.kt)
- **Problem**: Resolvers wrap `loader.budget()` in `runBlocking { ... }`, which can stall Netty event-loop threads under [`application.yaml`](../server/kt/src/main/resources/application.yaml) (`workerGroupSize: 2`, `shareWorkGroup: true`).
- **Action**: Convert resolver functions to `suspend fun` (natively supported by `graphql-kotlin`) and call `loader.budget()` directly.

### 2.4 Split Plumbing Out of `Application.kt`
- **File**: [`server/kt/src/main/kotlin/com/cherba29/tally/Application.kt`](../server/kt/src/main/kotlin/com/cherba29/tally/Application.kt)
- **Action**: Extract `RequestLoggingPlugin`, `QueryLoggingInstrumentation`, and `LoggingDataFetcherExceptionHandler` into dedicated files under a `server/` or `graphql/` subpackage so `Application.kt` stays focused on application wiring.

---

## Phase 3: Frontend Architecture & Maintainability (`client/src`)

### 3.1 Declarative State & Row Folding in Lit
- **Files**: [`client/src/components/summary-table.ts`](../client/src/components/summary-table.ts), [`client/src/components/tally-app.ts`](../client/src/components/tally-app.ts)
- **Problem**:
  - `SummaryTable.render()` mutates `childTableRows` via `addRow(...)` as a render side effect and toggles row visibility imperatively via `childElement.style.display`. Live YAML reloads reset all rows to expanded.
  - `TallyApp` relies on manual `this.requestUpdate()` calls and still carries an unused `popupMap` field.
- **Action**:
  - Replace `childTableRows` in `SummaryTable` with `@state() private collapsedIds = new Set<string>()` and compute row visibility purely during `render()`—preserving collapsed groups across live reloads.
  - Simplify `months` and `rows` getters/setters to `@property({attribute: false})`.
  - Convert `TallyApp` state fields to Lit `@state()` decorators and delete dead `popupMap` code.

### 3.2 Discriminated Union for `PopupData`
- **Files**: [`client/src/utils.ts`](../client/src/utils.ts), [`client/src/components/tally-app.ts`](../client/src/components/tally-app.ts)
- **Action**: Add an explicit `kind: 'summary' | 'transfers-summary' | 'statement' | 'account'` discriminant to `PopupData` (matching `ActivePopupRequest`) so `tooltipFragment()` can use an exhaustive `switch (this.popupData.kind)` instead of `'summary' in this.popupData` checks.

### 3.3 Simplify GraphQL Client (`api.ts`)
- **File**: [`client/src/api.ts`](../client/src/api.ts)
- **Problem**: `@apollo/client` is used exclusively for 4 static POST queries with `fetchPolicy: 'no-cache'`, while `graphql-transport-ws` subscriptions are already implemented manually. This contributes significantly to the ~345 kB client bundle size.
- **Action**: Replace `@apollo/client` with a small typed `fetch('/graphql', ...)` helper while keeping `@graphql-codegen` for TypeScript schema/query types. Verify bundle size reduction and simplify unit test mocks.

---

## Phase 4: Usability & Day-to-Day UX Polish

### 4.1 Clearer Date Toolbar & Owner Tabs
- **File**: [`client/src/components/tally-app.ts`](../client/src/components/tally-app.ts)
- **Action**:
  - Replace the repeated unlabeled `<< < > >>` toolbar buttons with clearly labeled controls (`-1y`, `-1m`, `+1m`, `+1y`, `Today`) and tooltips, laid out in a clean header bar that does not overlap table rows.
  - Render full owner names on owner tabs (instead of `${owner[0]}`, which collides when owners share an initial) with a clear active-tab style.

### 4.2 Sticky Table Headers & Account Column
- **File**: [`client/src/components/summary-table.ts`](../client/src/components/summary-table.ts)
- **Action**: Apply `position: sticky; top: 0` to `<thead>` and `position: sticky; left: 0` to the first column so account names and month headers remain visible when scrolling wide or tall ledgers.

### 4.3 Popup Ergonomics & Copy Feedback
- **Files**: [`client/src/components/tally-app.ts`](../client/src/components/tally-app.ts), tooltip components
- **Action**:
  - Clamp popup `top`/`left` coordinates to the viewport so tooltips near the right or bottom edge never render off-screen.
  - Support closing active popups via the **`Escape`** key and clicking outside (in addition to the `XXX` button).
  - Show a brief non-intrusive visual indicator (toast or cell highlight) when copying an unaccounted or running-balance amount to the clipboard.

### 4.4 Live-Reload Status Indicator
- **Files**: [`client/src/api.ts`](../client/src/api.ts), [`client/src/components/tally-app.ts`](../client/src/components/tally-app.ts)
- **Action**: Expose WebSocket connection state (`connected` / `reconnecting` / `disconnected`) from `BackendClient` and display a compact status indicator (plus last reload timestamp) in the header so side-by-side YAML editing provides clear feedback.

### 4.5 Fast CLI Invocation
- **Files**: Root script (e.g., `./tally.sh`) and/or unified fat-JAR entry point in [`server/kt`](../server/kt)
- **Action**: Provide a lightweight `./tally.sh` wrapper—or support `java -jar tally-all.jar cli <subcommand>`—so CLI tools (`unaccounted`, `csv-to-transfers`, `generate`, `summary`, `report`) run immediately without Gradle startup overhead.

---

## Verification Checklist (Per Phase)

- **Client**: `cd client && pnpm build && pnpm test && pnpm lint && npx tsc --noEmit`
- **Server**: `cd server/kt && ./gradlew test ktlintCheck buildFatJar`
- **End-to-End**: Run `./dev.sh` and `./run.sh` against `./sample-data`, verify live YAML reload, collapsed-row persistence, popup interactions, and CLI commands.
