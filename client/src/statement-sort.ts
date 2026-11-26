import {dateFormat, formatCadence} from './format';
import {type GqlBalance, type GqlTransaction} from './gql_types';

export type SortColumn =
  | 'account'
  | 'description'
  | 'recurrence'
  | 'type'
  | 'amount'
  | 'date'
  | 'balance';

export type SortDirection = 'asc' | 'desc';

export interface SortCriterion {
  column: SortColumn;
  direction: SortDirection;
}

export interface SortedTransactionRow {
  transaction: GqlTransaction;
  computedBalanceFromStart: number;
}

export const DEFAULT_SORT_ORDER: readonly SortCriterion[] = [
  {column: 'date', direction: 'desc'},
  {column: 'amount', direction: 'desc'},
];

const DEFAULT_DIRECTION_BY_COLUMN: Record<SortColumn, SortDirection> = {
  account: 'asc',
  description: 'asc',
  recurrence: 'desc',
  type: 'asc',
  amount: 'desc',
  date: 'desc',
  balance: 'desc',
};

/**
 * Updates the multi-column sort stack when the user clicks [column]:
 * - If [column] is already the primary sort key, toggles its direction (`asc` <-> `desc`)
 *   while preserving all secondary/tertiary tie-breakers.
 * - Otherwise promotes [column] to primary (reusing its prior direction if already in the stack,
 *   or its sensible default direction) and shifts previous sort keys down in priority.
 */
export function updateSortOrder(
  current: readonly SortCriterion[],
  column: SortColumn
): SortCriterion[] {
  if (current.length > 0 && current[0]!.column === column) {
    const toggled: SortDirection = current[0]!.direction === 'asc' ? 'desc' : 'asc';
    return [{column, direction: toggled}, ...current.slice(1)];
  }
  const existing = current.find((c) => c.column === column);
  const direction = existing ? existing.direction : DEFAULT_DIRECTION_BY_COLUMN[column];
  const rest = current.filter((c) => c.column !== column);
  return [{column, direction}, ...rest];
}

function transactionTypeSortKey(t: GqlTransaction): string {
  if (t.isExpense) return 'EXPENSE';
  if (t.isIncome) return 'INCOME';
  if (Number(t.balance?.amount ?? 0) > 0) return 'TRANSFER_IN';
  return 'TRANSFER_OUT';
}

function compareByColumn(a: GqlTransaction, b: GqlTransaction, column: SortColumn): number {
  switch (column) {
    case 'account':
      return (a.toAccountName ?? '').localeCompare(b.toAccountName ?? '', undefined, {
        sensitivity: 'base',
      });
    case 'description':
      return (a.description ?? '').localeCompare(b.description ?? '', undefined, {
        sensitivity: 'base',
      });
    case 'recurrence': {
      const countA = a.recurringGroupId ? Number(a.recurringCount ?? 1) : 0;
      const countB = b.recurringGroupId ? Number(b.recurringCount ?? 1) : 0;
      if (countA !== countB) return countA - countB;
      const cadenceCmp = (a.recurringGroupId ? formatCadence(a.recurringCadence) : '').localeCompare(
        b.recurringGroupId ? formatCadence(b.recurringCadence) : ''
      );
      if (cadenceCmp !== 0) return cadenceCmp;
      return (a.recurringGroupId ?? '').localeCompare(b.recurringGroupId ?? '');
    }
    case 'type':
      return transactionTypeSortKey(a).localeCompare(transactionTypeSortKey(b));
    case 'amount':
      return Number(a.balance?.amount ?? 0) - Number(b.balance?.amount ?? 0);
    case 'date':
      return dateFormat(a.balance?.date).localeCompare(dateFormat(b.balance?.date));
    case 'balance':
      return Number(a.balanceFromStart ?? 0) - Number(b.balanceFromStart ?? 0);
  }
}

/**
 * Sorts [transactions] according to [sortOrder] (remembering primary, secondary, ... order
 * with stable original-index tie-breaking) and recomputes running balances upward from
 * [startBalance] (bottom row to top row).
 *
 * When sorting primarily by `balance`, each row retains its chronological `balanceFromStart`
 * so the Balance column stays monotonically ordered.
 */
export function sortTransactionsWithRunningBalance(
  transactions: readonly GqlTransaction[],
  startBalance: GqlBalance | null | undefined,
  sortOrder: readonly SortCriterion[]
): SortedTransactionRow[] {
  const effectiveOrder = sortOrder.length > 0 ? sortOrder : DEFAULT_SORT_ORDER;
  const indexed = transactions.map((transaction, originalIndex) => ({transaction, originalIndex}));

  indexed.sort((a, b) => {
    for (const criterion of effectiveOrder) {
      const cmp = compareByColumn(a.transaction, b.transaction, criterion.column);
      if (cmp !== 0) {
        return criterion.direction === 'asc' ? cmp : -cmp;
      }
    }
    return a.originalIndex - b.originalIndex;
  });

  const primaryColumn = effectiveOrder[0]?.column;
  if (primaryColumn === 'balance') {
    return indexed.map(({transaction}) => ({
      transaction,
      computedBalanceFromStart: Number(transaction.balanceFromStart ?? 0),
    }));
  }

  const results: SortedTransactionRow[] = new Array(indexed.length);
  if (startBalance == null) {
    for (let i = 0; i < indexed.length; i++) {
      results[i] = {
        transaction: indexed[i]!.transaction,
        computedBalanceFromStart: 0,
      };
    }
    return results;
  }

  // Accumulate running balance upward from Starting Balance (bottom of table -> top of table).
  let running = Number(startBalance.amount);
  for (let i = indexed.length - 1; i >= 0; i--) {
    const transaction = indexed[i]!.transaction;
    running += Number(transaction.balance?.amount ?? 0);
    results[i] = {
      transaction,
      computedBalanceFromStart: running,
    };
  }
  return results;
}
