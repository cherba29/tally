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
