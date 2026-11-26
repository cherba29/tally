import {expect} from '@esm-bundle/chai';
import {
  DEFAULT_SORT_ORDER,
  sortTransactionsWithRunningBalance,
  updateSortOrder,
} from './statement-sort';
import {type GqlBalance, type GqlTransaction} from './gql_types';

const tx = (
  overrides: Partial<GqlTransaction> & {
    amount: number;
    date: string;
    balanceFromStart?: number;
  }
): GqlTransaction => ({
  toAccountName: overrides.toAccountName ?? 'expenses',
  description: overrides.description ?? '',
  isIncome: overrides.isIncome ?? false,
  isExpense: overrides.isExpense ?? true,
  balance: {
    amount: overrides.amount,
    date: overrides.date,
    desc: '',
    type: 'CONFIRMED',
  },
  balanceFromStart: overrides.balanceFromStart ?? 0,
  recurringGroupId: overrides.recurringGroupId ?? null,
  recurringCount: overrides.recurringCount ?? null,
  recurringCadence: overrides.recurringCadence ?? null,
});

describe('statement-sort', () => {
  describe('updateSortOrder', () => {
    it('toggles direction when clicking the current primary column without disturbing secondary criteria', () => {
      const next = updateSortOrder(DEFAULT_SORT_ORDER, 'date');
      expect(next).to.deep.equal([
        {column: 'date', direction: 'asc'},
        {column: 'amount', direction: 'desc'},
      ]);

      const toggledBack = updateSortOrder(next, 'date');
      expect(toggledBack).to.deep.equal([
        {column: 'date', direction: 'desc'},
        {column: 'amount', direction: 'desc'},
      ]);
    });

    it('remembers previous sort order when switching columns (amount -> date makes date primary, amount secondary)', () => {
      // Start with default [date desc, amount desc]
      const byAmount = updateSortOrder(DEFAULT_SORT_ORDER, 'amount');
      expect(byAmount).to.deep.equal([
        {column: 'amount', direction: 'desc'},
        {column: 'date', direction: 'desc'},
      ]);

      // Toggle amount to asc
      const byAmountAsc = updateSortOrder(byAmount, 'amount');
      expect(byAmountAsc).to.deep.equal([
        {column: 'amount', direction: 'asc'},
        {column: 'date', direction: 'desc'},
      ]);

      // Now sort by date -> date becomes primary, amount (asc) becomes secondary
      const byDateThenAmountAsc = updateSortOrder(byAmountAsc, 'date');
      expect(byDateThenAmountAsc).to.deep.equal([
        {column: 'date', direction: 'desc'},
        {column: 'amount', direction: 'asc'},
      ]);

      // Sort by account (new column defaults to asc) -> [account asc, date desc, amount asc]
      const byAccount = updateSortOrder(byDateThenAmountAsc, 'account');
      expect(byAccount).to.deep.equal([
        {column: 'account', direction: 'asc'},
        {column: 'date', direction: 'desc'},
        {column: 'amount', direction: 'asc'},
      ]);
    });
  });

  describe('sortTransactionsWithRunningBalance', () => {
    const startBalance: GqlBalance = {
      amount: 100000,
      date: '2026-11-01',
      desc: '',
      type: 'CONFIRMED',
    };

    const sampleTxs: GqlTransaction[] = [
      tx({
        toAccountName: 'expenses',
        description: 'Rent',
        amount: -50000,
        date: '2026-11-02',
        balanceFromStart: 50000,
        recurringGroupId: 'grp-rent',
        recurringCount: 5,
        recurringCadence: 'MONTHLY',
      }),
      tx({
        toAccountName: 'payroll',
        description: 'Paycheck',
        isIncome: true,
        isExpense: false,
        amount: 200000,
        date: '2026-11-15',
        balanceFromStart: 250000,
        recurringGroupId: 'grp-pay',
        recurringCount: 9,
        recurringCadence: 'BIWEEKLY',
      }),
      tx({
        toAccountName: 'expenses',
        description: 'Internet',
        amount: -7500,
        date: '2026-11-15',
        balanceFromStart: 242500,
        recurringGroupId: 'grp-net',
        recurringCount: 5,
        recurringCadence: 'MONTHLY',
      }),
      tx({
        toAccountName: 'credit-card',
        description: 'Card transfer',
        isIncome: false,
        isExpense: false,
        amount: -20000,
        date: '2026-11-20',
        balanceFromStart: 222500,
      }),
      tx({
        toAccountName: 'savings',
        description: 'Transfer in',
        isIncome: false,
        isExpense: false,
        amount: 10000,
        date: '2026-11-20',
        balanceFromStart: 232500,
      }),
    ];

    it('sorts by Date descending then Amount descending by default and recomputes running balance from bottom up', () => {
      const rows = sortTransactionsWithRunningBalance(sampleTxs, startBalance, []);
      expect(rows.map((r) => r.transaction.description)).to.deep.equal([
        'Transfer in', // 2026-11-20, +10000
        'Card transfer', // 2026-11-20, -20000
        'Paycheck', // 2026-11-15, +200000
        'Internet', // 2026-11-15, -7500
        'Rent', // 2026-11-02, -50000
      ]);
      // Bottom-to-top from 100000:
      // Rent: 50000 -> Internet: 42500 -> Paycheck: 242500 -> Card: 222500 -> Transfer in: 232500
      expect(rows.map((r) => r.computedBalanceFromStart)).to.deep.equal([
        232500, 222500, 242500, 42500, 50000,
      ]);
    });

    it('uses secondary sort key as tie-breaker when primary values match', () => {
      // Date DESC, then Amount ASC -> on 2026-11-20, -20000 comes before +10000
      const rows = sortTransactionsWithRunningBalance(sampleTxs, startBalance, [
        {column: 'date', direction: 'desc'},
        {column: 'amount', direction: 'asc'},
      ]);
      expect(rows.map((r) => r.transaction.description)).to.deep.equal([
        'Card transfer',
        'Transfer in',
        'Internet',
        'Paycheck',
        'Rent',
      ]);
      // Top-most row still equals startBalance + total (232500)
      expect(rows[0]!.computedBalanceFromStart).to.equal(232500);
    });

    it('sorts by account, description, recurrence, and type and recomputes running balance', () => {
      const byAccount = sortTransactionsWithRunningBalance(sampleTxs, startBalance, [
        {column: 'account', direction: 'asc'},
        {column: 'description', direction: 'asc'},
      ]);
      expect(byAccount.map((r) => r.transaction.toAccountName)).to.deep.equal([
        'credit-card',
        'expenses',
        'expenses',
        'payroll',
        'savings',
      ]);
      expect(byAccount[1]!.transaction.description).to.equal('Internet');
      expect(byAccount[2]!.transaction.description).to.equal('Rent');
      expect(byAccount[0]!.computedBalanceFromStart).to.equal(232500);

      const byRecurrence = sortTransactionsWithRunningBalance(sampleTxs, startBalance, [
        {column: 'recurrence', direction: 'desc'},
      ]);
      expect(byRecurrence[0]!.transaction.description).to.equal('Paycheck'); // 9
      expect(byRecurrence[1]!.transaction.recurringCount).to.equal(5);
      expect(byRecurrence[2]!.transaction.recurringCount).to.equal(5);
      expect(byRecurrence[3]!.transaction.recurringGroupId).to.equal(null);

      const byType = sortTransactionsWithRunningBalance(sampleTxs, startBalance, [
        {column: 'type', direction: 'asc'},
      ]);
      // E ->, E ->, I <-, T <-, T ->
      expect(byType.map((r) => r.transaction.description)).to.deep.equal([
        'Rent',
        'Internet',
        'Paycheck',
        'Transfer in',
        'Card transfer',
      ]);
    });

    it('preserves chronological balanceFromStart when sorting primarily by balance', () => {
      const byBalanceAsc = sortTransactionsWithRunningBalance(sampleTxs, startBalance, [
        {column: 'balance', direction: 'asc'},
      ]);
      expect(byBalanceAsc.map((r) => r.computedBalanceFromStart)).to.deep.equal([
        50000, 222500, 232500, 242500, 250000,
      ]);
    });

    it('returns 0 running balances when startBalance is null or undefined', () => {
      const rows = sortTransactionsWithRunningBalance(sampleTxs, null, DEFAULT_SORT_ORDER);
      expect(rows.every((r) => r.computedBalanceFromStart === 0)).to.equal(true);
    });
  });
});
