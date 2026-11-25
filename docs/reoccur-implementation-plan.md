# Recurring Transaction Detection & UI — Implementation Plan

## 1. Overview & Goals

Many personal finance transactions repeat over time (rent, utilities, paychecks, subscriptions, credit card payoffs, groceries), even when their descriptions vary slightly due to embedded dates, statement month names, confirmation/reference numbers, check numbers, or status markers like `(projected)`.

This plan outlines a step-by-step implementation to:
1. **Detect recurring transaction groups** on the backend using description normalization plus multi-signal similarity scoring (description, accounts, amount, and temporal cadence).
2. **Expose recurring metadata and group lookups** through the GraphQL API without bloating statement payloads.
3. **Indicate recurring transactions clearly** inside the monthly statement popup (`balance-tooltip`).
4. **Open a secondary popup** when clicking a recurring indicator, showing all similar transactions in the detected series alongside summary statistics and cadence info.

---

## 2. Key Architectural Decisions

### 2.1 Detect on Canonical Transfers per Owner (Pre-Mirroring)
In `BudgetBuilder`, every transfer in YAML produces two mirrored `Transaction` entries (outgoing on `fromAccount`, incoming on `toAccount`). Running detection once over the `resolvedTransfers` list (scoped per owner) provides:
- **Half the work** (no duplicate clustering of mirrored entries).
- **Guaranteed symmetry** (both `checking` and `credit-card`/`expenses` reference the exact same `recurringGroupId`).
- **Cross-account detection** (e.g., if a subscription moves from `checking -> expenses` to `credit-card -> expenses`, matching `toAccount` + description + amount + cadence can still link it).

### 2.2 Handle Tally's Catch-All External Accounts Carefully
In Tally ledgers (such as `sample-data/`), many unrelated expenses share the same external destination account (`expenses`). Therefore:
- Same `(fromAccount, toAccount)` is a **reinforcing signal**, never sufficient by itself to group unrelated descriptions.
- Description similarity (after stripping volatile tokens) is the **primary signal**, reinforced by account match, amount similarity, and day-of-month / interval regularity.

### 2.3 Lightweight Statement Payload + On-Demand Group Query
Rather than embedding full recurring histories inside every `GqlTransaction` returned by `statement(...)`, each `GqlTransaction` carries only lightweight indicator metadata (`recurringGroupId`, `recurringCount`, `recurringCadence`). Clicking a recurring badge fetches full occurrences via a dedicated `recurringTransactions(groupId, accountPath)` GraphQL query.

### 2.4 Stacked Secondary Popup (Non-Destructive UX)
Clicking a recurring badge inside `balance-tooltip` opens a **second floating popup** (`.secondary-toolTip`, `z-index: 35`) beside the primary statement popup (`.toolTip`, `z-index: 30`) instead of replacing it. This lets users inspect multiple recurring items in a statement without losing their place.

---

## 3. Step-by-Step Implementation Plan

### Phase 1 — Backend Recurring Detection Engine (`server/kt`)

Create a self-contained, independently testable package `com.cherba29.tally.recurring`:

- [x] **1.1 Define domain models & configuration**
  - **New file:** `server/kt/src/main/kotlin/com/cherba29/tally/recurring/RecurringModels.kt`
  - Define:
    - `RecurringDetectionConfig` with tunable defaults:
      - `similarityThreshold: Double = 0.72`
      - `minDescriptionSimilarity: Double = 0.45` (floor when both transactions have non-empty normalized descriptions)
      - Signal weights: `descWeight = 0.55`, `accountWeight = 0.20`, `amountWeight = 0.15`, `timingWeight = 0.10`
      - `minOccurrences: Int = 2`
      - `minSpanDays: Int = 6` (prevents two identical purchases on the same day from being flagged as recurring)
    - `RecurringCadence` enum: `WEEKLY`, `BIWEEKLY`, `MONTHLY`, `QUARTERLY`, `YEARLY`, `IRREGULAR`
    - `TransferCandidate` (lightweight input struct: `transferIndex`, `owner`, `fromAccountPath`, `fromAccountName`, `toAccountPath`, `toAccountName`, `month`, `balance`, `description`, `type`)
    - `RecurringOccurrence` (single transaction occurrence inside a group)
    - `RecurringGroup` (`id`, `label`, `cadence`, `typicalDayOfMonth`, `averageAmount`, `minAmount`, `maxAmount`, `totalAmount`, `isFixedAmount`, `occurrences`)
    - `RecurringTransactionIndex` (`groupsById: Map<String, RecurringGroup>`, lookup method from a statement transaction / transfer key to its `RecurringGroup?`, plus `RecurringTransactionIndex.EMPTY`).

- [x] **1.2 Implement description normalization**
  - **New file:** `server/kt/src/main/kotlin/com/cherba29/tally/recurring/DescriptionNormalizer.kt`
  - Normalize raw descriptions into `NormalizedDescription(normalizedText: String, tokens: List<String>, tokenSet: Set<String>)`:
    1. Lowercase and collapse Unicode/whitespace.
    2. Strip temporary/status annotations: `(projected)`, `[projected]`, `(pending)`, `projected`.
    3. Strip temporal markers:
       - ISO dates (`2026-10-15`, `2026/10/15`), slash/dash dates (`10/15/2026`, `10/15/26`, `10-15`), timestamps (`14:32:05`, `14:32`).
       - Tally month tokens (`Jan2026`..`Dec2026`), full/abbreviated month names (`january`..`december`, `jan`..`dec`), standalone 4-digit years (`2020`..`2099`), quarter markers (`q1`..`q4`).
    4. Strip volatile IDs / markers while preserving alphabetic merchant names (e.g. `PAYPAL *SPOTIFY` keeps `paypal spotify`):
       - Labeled IDs: `(?:ref|conf|confirmation|txn|trans|id|inv|invoice|chk|check|order|auth|trace|seq)\s*[#:\-]?\s*[a-z0-9\-]+`.
       - Hash/asterisk IDs containing at least one digit: `[#*]\s*[a-z0-9]*\d[a-z0-9]*`.
       - Standalone numeric sequences (`\b\d+\b`) and mixed alphanumeric/hex ID tokens containing digits (`\b(?=[a-z0-9]*\d)[a-z0-9]{4,}\b`).
    5. Replace remaining punctuation with spaces, trim, collapse multiple spaces, and extract non-empty semantic tokens.
  - **Example normalizations to verify:**
    - `"Pay off Nov2026 statement (projected)"` -> `"pay off statement"`
    - `"Electric & gas utility (summer AC)"` -> `"electric gas utility summer ac"`
    - `"PAYPAL *SPOTIFY 4029357733"` -> `"paypal spotify"`
    - `"ACH DEBIT CON EDISON 2026-10-15 REF#991823"` -> `"ach debit con edison"`
    - `"ZELLE PAYMENT TO JANE DOE CONF# ABC123456"` -> `"zelle payment to jane doe"`

- [x] **1.3 Implement multi-signal pairwise similarity scorer**
  - **New file:** `server/kt/src/main/kotlin/com/cherba29/tally/recurring/TransactionSimilarity.kt`
  - Hard disqualifiers (return `0.0` immediately):
    - Different `owner`.
    - Different `Transaction.Type` (e.g., `INCOME` vs `EXPENSE` vs `TRANSFER`).
    - Opposite direction between the same two accounts (`A -> B` vs `B -> A`).
    - Neither `fromAccountPath` nor `toAccountPath` matches.
  - Compute component scores (each in `[0.0, 1.0]`):
    1. **Description (`descScore`)**:
       - Exact non-empty `normalizedText` match -> `1.0`.
       - Otherwise blend:
         - Token Dice/Jaccard similarity (`2 * |A ∩ B| / (|A| + |B|)`).
         - Subset/containment bonus when all tokens of the shorter description (size >= 2) appear in the longer one (handles `"Electric & gas utility"` vs `"Electric & gas utility (summer AC)"`).
         - Normalized Levenshtein similarity on `normalizedText` (handles typos, singular/plural, minor truncation).
       - If both descriptions are non-empty and `descScore < config.minDescriptionSimilarity`, disqualify (return `0.0`) unless exact same `(from, to)` + exact same amount + tight cadence AND accounts are non-catch-all (or simply enforce `minDescriptionSimilarity` strictly to avoid false positives on `expenses`).
       - If both descriptions are blank, require exact `(fromAccountPath, toAccountPath)` AND (`amountScore >= 0.9` or `timingScore >= 0.8`).
    2. **Accounts (`accountScore`)**:
       - Exact same `(fromAccountPath, toAccountPath)` -> `1.0`.
       - Same `toAccountPath` (e.g. `expenses` across different payment cards) or same `fromAccountPath` (same income source) -> `0.5`.
    3. **Amount (`amountScore`)**:
       - Exact match (`|a| == |b|`) -> `1.0`.
       - Otherwise smooth decay based on relative difference `|a - b| / max(|a|, |b|)`: e.g. `max(0.0, 1.0 - relDiff / 0.35)` (gives `~0.85` for 5% diff, `~0.57` for 15% diff, `0.0` above 35% diff).
    4. **Temporal regularity (`timingScore`)**:
       - Circular day-of-month distance `dayDiff = min(|d1 - d2|, 30 - |d1 - d2|)` (handles month-end drift like 28th/30th/31st and weekend shifts):
         - `dayDiff <= 3` -> `1.0`
         - `dayDiff <= 6` -> `0.5`
       - Otherwise, if absolute gap between dates `gap >= 6` days and aligns with weekly (`gap % 7 <= 1 || gap % 7 == 6`) or bi-weekly (`gap % 14 <= 2 || gap % 14 >= 12`), return `0.7`.
       - Else `0.0`.
  - Weighted sum: `descWeight * descScore + accountWeight * accountScore + amountWeight * amountScore + timingWeight * timingScore`.

- [x] **1.4 Implement clustering, cadence inference & stable group IDs**
  - **New file:** `server/kt/src/main/kotlin/com/cherba29/tally/recurring/RecurringTransactionDetector.kt`
  - **Candidate blocking** (keeps runtime near-linear for large ledgers):
    - Partition by `(owner, type)`.
    - Index candidates by shared normalized tokens (length >= 3) and by `(fromAccountPath, toAccountPath)` for blank-description entries so only plausible pairs are scored.
  - **Anti-chaining clustering**:
    - Sort candidates deterministically before clustering.
    - Use average-linkage / medoid-anchored greedy clustering: a candidate joins an existing cluster only if its average (or medoid) similarity to cluster members meets `similarityThreshold` (prevents single-linkage drift where A~B~C merges unrelated A and C).
  - **Recurrence qualification**:
    - Retain clusters with `size >= config.minOccurrences` AND date span between earliest and latest occurrence `>= config.minSpanDays` (and at least 2 distinct dates).
  - **Cadence & stats calculation**:
    - Sort member dates ascending, compute consecutive day gaps, take the median gap:
      - `5..9` -> `WEEKLY`
      - `12..17` -> `BIWEEKLY`
      - `25..36` -> `MONTHLY`
      - `80..100` -> `QUARTERLY`
      - `345..385` -> `YEARLY`
      - otherwise -> `IRREGULAR`
    - Compute `typicalDayOfMonth` (median day-of-month when `MONTHLY` / `QUARTERLY` / `YEARLY` and spread <= 5 days), `averageAmount`, `minAmount`, `maxAmount`, `totalAmount`, `isFixedAmount = (minAmount == maxAmount)`.
    - Compute representative `label`: most frequent raw description after stripping `(projected)` / `[projected]` (falling back to normalized text or `"$fromAccountName -> $toAccountName"`).
    - Compute **deterministic `groupId`**: short hex SHA-256 hash of `owner | type | primaryFromPath | primaryToPath | canonicalNormalizedText` (with deterministic suffix if hash collides), ensuring IDs stay stable across live YAML reloads.
    - Sort `occurrences` descending by date (newest first, matching `TransactionStatement`).

- [x] **1.5 Write unit tests for Phase 1**
  - **New files:**
    - `server/kt/src/test/kotlin/com/cherba29/tally/recurring/DescriptionNormalizerTest.kt`
    - `server/kt/src/test/kotlin/com/cherba29/tally/recurring/TransactionSimilarityTest.kt`
    - `server/kt/src/test/kotlin/com/cherba29/tally/recurring/RecurringTransactionDetectorTest.kt`
  - Verify against:
    - Real-world noisy bank strings (IDs, timestamps, hex codes, check numbers, `PAYPAL *MERCHANT`).
    - All `sample-data/` ledgers:
      - **Detected as recurring:** `Monthly apartment rent`, `Fiber internet`, `Electric & gas utility` (incl. `(summer AC)` and `(projected)`), `Pay off <Month> statement`, `Acme Corp bi-monthly paycheck`, `Streaming & cloud subscriptions`, `Weekly groceries`.
      - **NOT falsely merged:** `Autumn jacket`, `Running shoes`, `Weekend train tickets`, `Holiday gifts (projected)`, `Transit pass & rideshare`, `Dinner with friends`.
    - Cross-account subscription migration (`checking -> expenses` then `credit-card -> expenses`).
    - Same-day duplicate transactions (not marked recurring).

---

### Phase 2 — Budget Integration & GraphQL API (`server/kt`)

- [x] **2.1 Wire detection into `Budget` and `BudgetBuilder`**
  - **Modify:** `server/kt/src/main/kotlin/com/cherba29/tally/data/Budget.kt`
    - Add `val recurringIndex: RecurringTransactionIndex = RecurringTransactionIndex.EMPTY` (defaulted so existing callers/tests compile without changes).
  - **Modify:** `server/kt/src/main/kotlin/com/cherba29/tally/data/builder/BudgetBuilder.kt`
    - In `build()`, map `resolvedTransfers` to `List<TransferCandidate>`, run `RecurringTransactionDetector().detect(candidates)` inside `timeSource.measureTimedValue`, log timing alongside transactions/summaries, and pass `recurringIndex` into `Budget(...)`.
  - **Modify:** `server/kt/src/main/kotlin/com/cherba29/tally/data/BudgetQueries.kt`
    - Add `fun Budget.recurringGroup(groupId: String): RecurringGroup` throwing `NotFoundException` if missing.
    - Ensure `Budget.statement(...)` also has easy access to `recurringIndex` (or expose `recurringGroupFor(accountNode, month, transaction)` on `RecurringTransactionIndex`).

- [x] **2.2 Extend GraphQL schema types & mappers**
  - **Modify:** `server/kt/src/main/kotlin/com/cherba29/tally/schema/GqlData.kt`
    - Add optional fields to `GqlTransaction` (defaulted to `null` for backward compatibility):
      ```kotlin
      val recurringGroupId: String? = null,
      val recurringCount: Int? = null,
      val recurringCadence: String? = null
      ```
    - Add new GraphQL types:
      - `GqlRecurringTransaction(month, fromAccountName, fromAccountPath, toAccountName, toAccountPath, counterpartyAccountName, isIncome, isExpense, balance, description, isCurrentAccount)`
      - `GqlRecurringGroup(id, label, cadence, occurrenceCount, averageAmount, minAmount, maxAmount, totalAmount, isFixedAmount, typicalDayOfMonth, transactions)`
  - **Modify:** `server/kt/src/main/kotlin/com/cherba29/tally/schema/ToGql.kt`
    - Update `TransactionStatement.toGql(...)` and `Transaction.toGql(...)` to accept an optional `recurringGroupLookup: (Transaction) -> RecurringGroup? = { null }` and populate `recurringGroupId`, `recurringCount`, `recurringCadence`.
    - Add `RecurringGroup.toGql(viewerAccountPath: String?): GqlRecurringGroup` which orients each occurrence's `balance.amount`, `isIncome`, `isExpense`, `counterpartyAccountName`, and `isCurrentAccount` relative to `viewerAccountPath` (falling back to the transfer's outgoing perspective if `viewerAccountPath` is null or unrelated).

- [x] **2.3 Add GraphQL query service & wire into `Application.kt`**
  - **Modify:** `server/kt/src/main/kotlin/com/cherba29/tally/StatementService.kt`
    - Pass `budget.recurringIndex` lookup when converting `statement.toGql(...)`.
  - **New file:** `server/kt/src/main/kotlin/com/cherba29/tally/RecurringService.kt`
    - Implement `class RecurringService(val loader: Loader) : Query` with:
      ```kotlin
      @GraphQLDescription("Returns details and all occurrences for a recurring transaction group.")
      suspend fun recurringTransactions(
        groupId: String,
        accountPath: String? = null
      ): GqlRecurringGroup
      ```
  - **Modify:** `server/kt/src/main/kotlin/com/cherba29/tally/Application.kt`
    - Register `RecurringService(loader)` in `GraphQL { schema { queries = listOf(...) } }`.

- [x] **2.4 Update backend tests & SDL snapshot**
  - **New file:** `server/kt/src/test/kotlin/com/cherba29/tally/RecurringServiceTest.kt`
  - **Modify:**
    - `server/kt/src/test/kotlin/com/cherba29/tally/StatementServiceTest.kt` (add test verifying `recurringGroupId`, `recurringCount`, `recurringCadence` are populated for recurring transactions and `null` for one-offs).
    - `server/kt/src/test/kotlin/com/cherba29/tally/schema/ToGqlTest.kt` (test `RecurringGroup.toGql` sign orientation for both source and destination account viewpoints).
    - `server/kt/src/test/kotlin/com/cherba29/tally/ApplicationTest.ss` (update GraphQL SDL snapshot with new fields and `recurringTransactions` query).
  - **Verification:** `cd server/kt && ./gradlew test ktlintCheck`

---

### Phase 3 — CLI Inspection Command (Optional but Recommended for Validation)

- [x] **3.1 Add `recurring` CLI subcommand**
  - **New file:** `server/kt/src/main/kotlin/com/cherba29/tally/cli/cmds/Recurring.kt`
  - **Modify:** `server/kt/src/main/kotlin/com/cherba29/tally/cli/Cli.kt` (register `Recurring()`).
  - **New file:** `server/kt/src/test/kotlin/com/cherba29/tally/cli/cmds/RecurringTest.kt`
  - Prints detected recurring groups (label, cadence, count, avg/min/max, account pair, optional `--verbose` to list all occurrences) via `./tally.sh cli recurring [--owner <owner>] [--account <account>]`.
  - Makes it fast to audit detection quality against real personal YAML ledgers and tune thresholds if needed.

---

### Phase 4 — Frontend Recurring Indicator & Secondary Popup (`client/`)

- [x] **4.1 Regenerate GraphQL types & extend `BackendClient`**
  - Start the backend (`./tally.sh server --backend-only`) and run `cd client && pnpm graphql-codegen` to update `client/src/gql_types.ts`.
  - **Modify:** `client/src/api.ts`
    - Update `loadStatement(...)` query selection set to request:
      ```graphql
      recurringGroupId
      recurringCount
      recurringCadence
      ```
    - Add `loadRecurringTransactions(groupId: string, accountPath?: string): Promise<QueryResult<Query>>` executing:
      ```graphql
      query recurringTransactions($groupId: String!, $accountPath: String) {
        recurringTransactions(groupId: $groupId, accountPath: $accountPath) {
          id
          label
          cadence
          occurrenceCount
          averageAmount
          minAmount
          maxAmount
          totalAmount
          isFixedAmount
          typicalDayOfMonth
          transactions {
            month
            fromAccountName
            fromAccountPath
            toAccountName
            toAccountPath
            counterpartyAccountName
            isIncome
            isExpense
            isCurrentAccount
            description
            balance {
              amount
              date
              type
            }
          }
        }
      }
      ```

- [x] **4.2 Extract reusable viewport-clamped popup positioning helper**
  - **New file:** `client/src/popup-position.ts`
  - Extract `PopupAnchor`, `PopupSize`, `ViewportSize`, and `computePopupPosition(...)` from `TallyApp.applyPopupPosition()` so both primary and secondary popups share identical, flicker-free flip-and-clamp logic.
  - Add an option `preferSide?: boolean` (for secondary popups: prefer placing right or left of the primary popup/badge before falling back to above/below so the originating transaction row stays visible).
  - **New file:** `client/src/popup-position.test.ts`

- [x] **4.3 Add recurring indicator badge to `BalanceTooltip`**
  - **Modify:** `client/src/components/balance-tooltip.ts`
  - Define and export `RecurringClickEventData`:
    ```ts
    export interface RecurringClickEventData {
      groupId: string;
      accountPath: string;
      selectedDate: string;
      selectedAmount: number;
      selectedDescription: string;
      mouseEvent: MouseEvent;
      anchorRect: DOMRect;
    }
    ```
  - For each transaction row where `t.recurringGroupId` is non-null:
    - Render a compact, accessible `<button type="button" class="recurring-badge">` inline in the description cell (e.g., `↻ ${t.recurringCount}`) with descriptive `title` (e.g., `"Recurring (Monthly, 5 occurrences) — click to view all"`), `aria-label`, and distinct pill styling legible against `#ffff80` (e.g., `#e8f1ff` fill, `#1f6feb` border/text, darker hover highlight).
    - On click: call `e.stopPropagation()`, capture `(e.currentTarget as HTMLElement).getBoundingClientRect()` synchronously, and dispatch `new CustomEvent<RecurringClickEventData>('recurring-click', { detail, bubbles: true, composed: true })`.
  - Keep non-recurring rows visually clean with zero extra clutter.

- [x] **4.4 Build `RecurringTooltip` component**
  - **New file:** `client/src/components/recurring-tooltip.ts`
  - Custom element `<recurring-tooltip>` with properties:
    - `@property({attribute: false}) group: GqlRecurringGroup | undefined`
    - `@property({attribute: false}) highlightKey?: { date: string; amount: number; description: string }`
  - Layout & features:
    - Header bar: close control (`×` / `XXX`), group label, and cadence/count badge (`Monthly · 5`).
    - Summary metrics row:
      - Fixed amount (`Fixed: $75.00`) OR range (`Avg: $123.54 ($108.90 – $142.80)`).
      - Total across occurrences (`Total: $617.70`).
      - Typical day of month if present (`~24th of month`).
    - Transactions table (consistent styling with `balance-tooltip.ts`):
      - Columns: `#`, `Account / Description` (shows counterparty, or `from → to` badge if `!t.isCurrentAccount`), `Type` (`E →` / `I ←` / `T ←` / `T →` with matching colors), `Amount` (`.projected`/`.confirmed` styling, click-to-copy dispatching `copied` event), `Date`, `Month`.
      - Highlight the row matching `highlightKey` (e.g. `.current-occurrence` with bold text / left accent border) so the user immediately sees where the clicked transaction sits in the history.
    - Dispatches `close` event on close click.

- [x] **4.5 Wire secondary popup into `TallyApp`**
  - **Modify:** `client/src/utils.ts`
    - Add `SecondaryPopupData` interface (holding `group: GqlRecurringGroup` and optional `highlightKey`).
  - **Modify:** `client/src/components/tally-app.ts`
    - Import `./recurring-tooltip` and `computePopupPosition`.
    - Add reactive state & tracking fields:
      - `@state() private secondaryPopupData: SecondaryPopupData | undefined = undefined`
      - `private activeSecondaryPopup: { groupId: string; accountPath: string; highlightKey?: ... } | undefined`
      - `private secondaryPopupRequestId = 0`
      - `private secondaryPopupPositionToken = 0`
      - `private secondaryPopupAnchor: PopupAnchor`
      - `private secondaryPopupResizeObserver?: ResizeObserver`
      - `private lastRecurringClickEvent?: MouseEvent`
    - Render `<div class="secondary-toolTip" ...>` with `z-index: 35`, `position: fixed`, slightly distinct border/shadow (e.g., `box-shadow: 0 4px 12px rgba(0,0,0,0.22)`, `#fffde7` or `#ffffb0` background to distinguish visually from the primary `#ffff80` statement popup).
    - Listen for `@recurring-click=${this.onRecurringClick}` on `<balance-tooltip>` / `.toolTip`:
      - Capture anchor (preferring the primary `.toolTip`'s bounding box horizontal edges + the clicked badge's vertical position so the secondary popup opens neatly beside the statement popup without covering the clicked row!).
      - Call `this.reloadSecondaryPopup(groupId, accountPath, highlightKey)`.
    - Position secondary popup with the same flicker-free `visibility: hidden` -> `await childTooltip.updateComplete` -> `computePopupPosition(...)` -> `visibility: visible` flow, plus `ResizeObserver` and `window` `resize` updates.
    - Layered dismissal & lifecycle rules:
      - `Escape` key: if `secondaryPopupData || activeSecondaryPopup` is open, close only the secondary popup; otherwise close the primary popup.
      - Document click: keep popups open when clicking inside `.toolTip`, `.secondary-toolTip`, `header.toolbar`, or `nav.owner-tabs`; otherwise close both.
      - `closePopup()` (closing primary popup, switching tabs, or clicking a new table cell) also calls `closeSecondaryPopup()`.
      - `refreshActivePopup()` (on WebSocket live reload) also refreshes `activeSecondaryPopup` (closing it cleanly if the group no longer exists after a YAML edit).

- [x] **4.6 Write & update frontend unit tests**
  - **New files:**
    - `client/src/popup-position.test.ts`
    - `client/src/components/recurring-tooltip.test.ts`
  - **Modify:**
    - `client/src/api.test.ts` (assert recurring fields in `loadStatement` query and test `loadRecurringTransactions`).
    - `client/src/components/balance-tooltip.test.ts` (assert recurring badge renders when `recurringGroupId` is set, omitted otherwise, dispatches `recurring-click` with `anchorRect` and stops propagation).
    - `client/src/components/tally-app.test.ts` (assert clicking badge loads & displays `<recurring-tooltip>` alongside `<balance-tooltip>`, `Escape` closes secondary first then primary, outside click closes both, live-reload refreshes both, switching cells closes secondary).
  - **Verification:** `cd client && pnpm typecheck && pnpm lint && pnpm build && pnpm test`

---

### Phase 5 — End-to-End & Visual Verification

- [x] **5.1 Build full application & run all automated checks**
  - Backend: `cd server/kt && ./gradlew test ktlintCheck buildFatJar`
  - Frontend: `cd client && pnpm typecheck && pnpm lint && pnpm build && pnpm test`
- [x] **5.2 Live browser verification against `sample-data/`**
  - Launch `./tally.sh server` and inspect in a real/headless browser at multiple viewports (`1200×700`, `1200×400`, narrow window):
    - Open `checking` `Nov2026` statement: verify `Monthly apartment rent`, `Fiber internet`, `Electric & gas utility (projected)`, `Pay off Oct2026 statement`, and `Acme Corp bi-monthly paycheck` all display recurring indicators.
    - Open `credit-card` `Nov2026` statement: verify `Streaming & cloud subscriptions` and `Weekly groceries` show recurring indicators while `Holiday gifts (projected)` does not.
    - Click a recurring badge: verify secondary popup opens cleanly beside the statement popup with zero flicker, zero viewport clipping, accurate stats/cadence, highlighted current row, working clipboard copy, and layered `Escape` dismissal.
    - Edit a YAML file while both popups are open and verify WebSocket live-reload refreshes both popups smoothly.
- [x] **5.3 Refresh documentation/screenshots (optional)**
  - Update `sample-data/screenshot-statement.png` if desired to showcase recurring badges and the recurring transactions popup.

---

## 4. Summary of Files to Create / Modify

### New Files
| File | Purpose |
| --- | --- |
| `server/kt/src/main/kotlin/com/cherba29/tally/recurring/RecurringModels.kt` | Config, cadence enum, group/occurrence/index data models |
| `server/kt/src/main/kotlin/com/cherba29/tally/recurring/DescriptionNormalizer.kt` | Strips IDs, dates, timestamps, month tokens, and temporary markers |
| `server/kt/src/main/kotlin/com/cherba29/tally/recurring/TransactionSimilarity.kt` | Multi-signal similarity scoring & disqualifier guards |
| `server/kt/src/main/kotlin/com/cherba29/tally/recurring/RecurringTransactionDetector.kt` | Candidate blocking, anti-chaining clustering, cadence inference, stable IDs |
| `server/kt/src/main/kotlin/com/cherba29/tally/RecurringService.kt` | GraphQL `recurringTransactions` query resolver |
| `server/kt/src/main/kotlin/com/cherba29/tally/cli/cmds/Recurring.kt` | Optional CLI command to inspect detected recurring groups |
| `client/src/popup-position.ts` | Shared flip-and-clamp viewport positioning helper |
| `client/src/components/recurring-tooltip.ts` | Secondary popup displaying all similar transactions & summary stats |
| Backend & frontend test files | Unit tests for normalizer, similarity, detector, service, positioning, and UI components |

### Modified Files
| File | Change |
| --- | --- |
| `server/kt/src/main/kotlin/com/cherba29/tally/data/Budget.kt` | Hold `recurringIndex: RecurringTransactionIndex` |
| `server/kt/src/main/kotlin/com/cherba29/tally/data/builder/BudgetBuilder.kt` | Run detector during `build()` and attach index to `Budget` |
| `server/kt/src/main/kotlin/com/cherba29/tally/data/BudgetQueries.kt` | Add `Budget.recurringGroup(groupId)` lookup |
| `server/kt/src/main/kotlin/com/cherba29/tally/schema/GqlData.kt` | Add recurring metadata to `GqlTransaction`; add `GqlRecurringGroup` & `GqlRecurringTransaction` |
| `server/kt/src/main/kotlin/com/cherba29/tally/schema/ToGql.kt` | Populate recurring metadata in statement conversion; map `RecurringGroup` to GQL |
| `server/kt/src/main/kotlin/com/cherba29/tally/StatementService.kt` | Pass `budget.recurringIndex` when building `GqlStatement` |
| `server/kt/src/main/kotlin/com/cherba29/tally/Application.kt` | Register `RecurringService(loader)` in GraphQL schema |
| `server/kt/src/test/kotlin/com/cherba29/tally/ApplicationTest.ss` | Update GraphQL SDL snapshot |
| `client/src/gql_types.ts` | Regenerate TypeScript types via `pnpm graphql-codegen` |
| `client/src/api.ts` | Request recurring fields in `loadStatement`; add `loadRecurringTransactions` |
| `client/src/utils.ts` | Add `SecondaryPopupData` type |
| `client/src/components/balance-tooltip.ts` | Render clickable recurring indicator badge and dispatch `recurring-click` |
| `client/src/components/tally-app.ts` | Manage secondary popup state, positioning, live-reload refresh, and layered dismissal |

---

## 5. Tunable Parameters & Edge Cases to Watch

| Parameter / Case | Initial Default / Handling | Why It Matters |
| --- | --- | --- |
| `similarityThreshold` | `0.72` | High enough to avoid grouping unrelated expenses on catch-all `expenses`, low enough to catch descriptions with extra notes like `(summer AC)`. |
| `minDescriptionSimilarity` | `0.45` | Prevents two unrelated `$50` purchases on `expenses` from merging solely because accounts + amount + day happen to match. |
| `minSpanDays` | `6` | Excludes same-day duplicate charges while still allowing weekly/bi-weekly recurring transactions. |
| `PAYPAL *MERCHANT 12345` | Strip `#/*` tokens only when they contain digits | Preserves merchant name (`spotify`) while stripping numeric transaction suffixes (`4029357733`). |
| Projected future entries | Strip `(projected)` during normalization; include in group | Lets upcoming projected bills appear in the recurring history alongside past confirmed occurrences. |
| Secondary popup near screen edge | Place beside primary `.toolTip` (right, else left), clamped with `8px` margin | Keeps both the originating transaction row and the full recurring history legible on smaller viewports. |
