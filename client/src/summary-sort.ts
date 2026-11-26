import {dateFormat} from './format';
import {type SortDirection} from './statement-sort';
import {type StatementEntry} from './utils';

export type SummarySortColumn =
  | 'account'
  | 'startDate'
  | 'startBalance'
  | 'endDate'
  | 'endBalance'
  | 'change'
  | 'percentChange'
  | 'annualizedPercentChange'
  | 'inFlows'
  | 'outFlows'
  | 'addSub'
  | 'income'
  | 'expenses'
  | 'transfers'
  | 'unaccounted';

export interface SummarySortCriterion {
  column: SummarySortColumn;
  direction: SortDirection;
}

const DEFAULT_DIRECTION_BY_COLUMN: Record<SummarySortColumn, SortDirection> = {
  account: 'asc',
  startDate: 'desc',
  startBalance: 'desc',
  endDate: 'desc',
  endBalance: 'desc',
  change: 'desc',
  percentChange: 'desc',
  annualizedPercentChange: 'desc',
  inFlows: 'desc',
  outFlows: 'desc',
  addSub: 'desc',
  income: 'desc',
  expenses: 'desc',
  transfers: 'desc',
  unaccounted: 'desc',
};

/**
 * Updates the multi-column sort stack when the user clicks [column]:
 * - If [column] is already the primary sort key, toggles its direction (`asc` <-> `desc`)
 *   while preserving all secondary/tertiary tie-breakers.
 * - Otherwise promotes [column] to primary (reusing its prior direction if already in the stack,
 *   or its sensible default direction) and shifts previous sort keys down in priority.
 */
export function updateSummarySortOrder(
  current: readonly SummarySortCriterion[],
  column: SummarySortColumn
): SummarySortCriterion[] {
  if (current.length > 0 && current[0]!.column === column) {
    const toggled: SortDirection = current[0]!.direction === 'asc' ? 'desc' : 'asc';
    return [{column, direction: toggled}, ...current.slice(1)];
  }
  const existing = current.find((c) => c.column === column);
  const direction = existing ? existing.direction : DEFAULT_DIRECTION_BY_COLUMN[column];
  const rest = current.filter((c) => c.column !== column);
  return [{column, direction}, ...rest];
}

function parseNumeric(value: unknown): number | null {
  if (value === null || value === undefined || value === '') {
    return null;
  }
  if (typeof value === 'number') {
    return Number.isFinite(value) ? value : null;
  }
  if (typeof value === 'string') {
    const parsed = Number(value.replace(/%$/, '').trim());
    return Number.isFinite(parsed) ? parsed : null;
  }
  return null;
}

function parseDateKey(value: string | Date | undefined | null): string | null {
  const formatted = dateFormat(value);
  return formatted === 'n/a' ? null : formatted;
}

function compareNullableNumbers(
  a: number | null,
  b: number | null,
  direction: SortDirection
): number {
  if (a === null && b === null) return 0;
  if (a === null) return 1;
  if (b === null) return -1;
  if (a === b) return 0;
  return direction === 'asc' ? a - b : b - a;
}

function compareNullableStrings(
  a: string | null,
  b: string | null,
  direction: SortDirection
): number {
  if (a === null && b === null) return 0;
  if (a === null) return 1;
  if (b === null) return -1;
  const cmp = a.localeCompare(b);
  return direction === 'asc' ? cmp : -cmp;
}

function compareBySummaryColumn(
  a: StatementEntry,
  b: StatementEntry,
  column: SummarySortColumn,
  direction: SortDirection
): number {
  switch (column) {
    case 'account': {
      const nameA = a.name.split('/').pop() ?? a.name;
      const nameB = b.name.split('/').pop() ?? b.name;
      const cmp = nameA.localeCompare(nameB, undefined, {sensitivity: 'base'});
      const result = cmp !== 0 ? cmp : a.name.localeCompare(b.name, undefined, {sensitivity: 'base'});
      return direction === 'asc' ? result : -result;
    }
    case 'startDate':
      return compareNullableStrings(
        parseDateKey(a.stmt.startBalance?.date),
        parseDateKey(b.stmt.startBalance?.date),
        direction
      );
    case 'startBalance':
      return compareNullableNumbers(
        parseNumeric(a.stmt.startBalance?.amount),
        parseNumeric(b.stmt.startBalance?.amount),
        direction
      );
    case 'endDate':
      return compareNullableStrings(
        parseDateKey(a.stmt.endBalance?.date),
        parseDateKey(b.stmt.endBalance?.date),
        direction
      );
    case 'endBalance':
      return compareNullableNumbers(
        parseNumeric(a.stmt.endBalance?.amount),
        parseNumeric(b.stmt.endBalance?.amount),
        direction
      );
    case 'change':
      return compareNullableNumbers(parseNumeric(a.stmt.change), parseNumeric(b.stmt.change), direction);
    case 'percentChange':
      return compareNullableNumbers(
        parseNumeric(a.stmt.percentChange),
        parseNumeric(b.stmt.percentChange),
        direction
      );
    case 'annualizedPercentChange':
      return compareNullableNumbers(
        parseNumeric(a.stmt.annualizedPercentChange),
        parseNumeric(b.stmt.annualizedPercentChange),
        direction
      );
    case 'inFlows':
      return compareNullableNumbers(parseNumeric(a.stmt.inFlows), parseNumeric(b.stmt.inFlows), direction);
    case 'outFlows':
      return compareNullableNumbers(parseNumeric(a.stmt.outFlows), parseNumeric(b.stmt.outFlows), direction);
    case 'addSub':
      return compareNullableNumbers(parseNumeric(a.stmt.addSub), parseNumeric(b.stmt.addSub), direction);
    case 'income':
      return compareNullableNumbers(parseNumeric(a.stmt.income), parseNumeric(b.stmt.income), direction);
    case 'expenses':
      return compareNullableNumbers(
        parseNumeric(a.stmt.totalPayments),
        parseNumeric(b.stmt.totalPayments),
        direction
      );
    case 'transfers':
      return compareNullableNumbers(
        parseNumeric(a.stmt.totalTransfers),
        parseNumeric(b.stmt.totalTransfers),
        direction
      );
    case 'unaccounted':
      return compareNullableNumbers(
        parseNumeric(a.stmt.unaccounted),
        parseNumeric(b.stmt.unaccounted),
        direction
      );
  }
}

/**
 * Sorts summary [entries] according to [sortOrder], remembering primary/secondary/tertiary
 * tie-breakers and falling back to the original order when [sortOrder] is empty or all keys tie.
 * Missing values (`null`/`undefined`/`NaN`/`n/a`) always sort to the bottom.
 */
export function sortSummaryEntries(
  entries: readonly StatementEntry[],
  sortOrder: readonly SummarySortCriterion[]
): StatementEntry[] {
  if (sortOrder.length === 0) {
    return [...entries];
  }
  const indexed = entries.map((entry, originalIndex) => ({entry, originalIndex}));
  indexed.sort((a, b) => {
    for (const criterion of sortOrder) {
      const cmp = compareBySummaryColumn(a.entry, b.entry, criterion.column, criterion.direction);
      if (cmp !== 0) {
        return cmp;
      }
    }
    return a.originalIndex - b.originalIndex;
  });
  return indexed.map(({entry}) => entry);
}
