# Changelog

All notable user-facing changes to Tally are documented in this file.

## [0.4.0] - 2026-11-23

Major release transitioning Tally to a self-contained single-JAR application with real-time live reload, richer interactive analytics, an expanded CLI, and ready-to-run sample data.

### Highlights

- **Single-command startup & standalone fat JAR**: Run the entire application (backend + embedded web UI) from a single executable (`tally-all.jar`) via `./run.sh`, or launch hot-reloading development mode via `./dev.sh`. Both scripts accept a custom data directory on the command line (`--data <path>` or positional argument).
- **Bundled 5-month `sample-data/` ledger**: Try Tally immediately out of the box with a complete double-entry example (checking, credit card, payroll, and expenses), including walkthrough documentation and UI screenshots in [`sample-data/README.md`](sample-data/README.md).
- **Real-time live reload for tables and open popups**: Saving, adding, or deleting any YAML file automatically reloads the budget and pushes updates to the browser—refreshing both the main spreadsheet and any currently open popup without losing your place.
- **Interactive Transfers Summary popup with growth & return analytics**: Click any `+/-` cell to inspect monthly internal/external transfers, interactive reference-date growth charts, effective annualized rates of return for irregular cash flows, and weighted internal/external age metrics.
- **Enhanced Balance Summary popup**: Added quick period-selection buttons, custom month-range controls, and an annualized percentage change column to the summary tooltip.
- **Flexible hierarchical account paths, ranking & collapsible groups**: Organize accounts into arbitrary nested `path:` hierarchies (replacing fixed account types), order accounts with `rank:`, and fold/unfold summary groups in the table.
- **Automatic hiding of closed & inactive accounts**: Summary rows automatically hide when all child accounts are closed in the viewed date window, and closed external accounts fold into collapsed inactive groups to keep active views clean.
- **In-UI month-range navigation**: Step the start and end of the displayed month window forward or backward by 1 or 12 months directly from the top toolbar.
- **Click-to-copy reconciliation amounts**: Click any unaccounted (`?`) cell in the main table or any running balance in a monthly statement popup to copy the exact amount to your clipboard.
- **Streamlined YAML format & richer account metadata**: Added a concise month-grouped `transfers_to:` syntax, `statement_close_day` support, balance descriptions, and transaction `tags`.
- **Actionable YAML error reporting**: Parsing and validation errors now pinpoint the exact file, line number, field path, and valid account tree directly in the UI instead of crashing the live file watcher.
- **Expanded CLI toolkit**: Added `csv-to-transfers` (including Chase/Amazon CSV import), multi-month `summary`, and `transfers-summary` commands; improved `report` (summary support), `generate` (next-balance date prediction and flush transfers), and `unaccounted` (option to ignore projected balances).
- **Reliability & display fixes**: Fixed refreshing `AccountTooltip` when switching directly between accounts, fixed negative annualized rate calculations, and upgraded monetary amounts to 64-bit integers to avoid overflow on large balances.
