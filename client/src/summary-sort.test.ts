import {expect} from '@open-wc/testing';
import {type GqlStatement} from './gql_types';
import {type StatementEntry} from './utils';
import {
  sortSummaryEntries,
  updateSummarySortOrder,
  type SummarySortColumn,
  type SummarySortCriterion,
} from './summary-sort';

function makeEntry(
  name: string,
  overrides: Partial<GqlStatement> = {}
): StatementEntry {
  const stmt: GqlStatement = {
    name,
    month: 'Oct2026',
    isClosed: false,
    isCovered: true,
    isProjectedCovered: true,
    hasProjectedTransfer: false,
    startBalance: {amount: 10000, date: '2026-10-01', desc: '', type: 'CONFIRMED'},
    endBalance: {amount: 12000, date: '2026-11-01', desc: '', type: 'CONFIRMED'},
    change: 2000,
    percentChange: 20,
    annualizedPercentChange: 240,
    inFlows: 5000,
    outFlows: -3000,
    addSub: 2000,
    income: 5000,
    totalPayments: -3000,
    totalTransfers: 0,
    unaccounted: 0,
    transactions: [],
    ...overrides,
  };
  return {name, stmt};
}

describe('summary-sort', () => {
  describe('updateSummarySortOrder', () => {
    it('starts with sensible default directions and toggles on repeat click', () => {
      let order: SummarySortCriterion[] = [];
      order = updateSummarySortOrder(order, 'account');
      expect(order).to.deep.equal([{column: 'account', direction: 'asc'}]);

      order = updateSummarySortOrder(order, 'account');
      expect(order).to.deep.equal([{column: 'account', direction: 'desc'}]);

      order = updateSummarySortOrder(order, 'endBalance');
      expect(order).to.deep.equal([
        {column: 'endBalance', direction: 'desc'},
        {column: 'account', direction: 'desc'},
      ]);
    });

    it('remembers previous sort order as secondary and restores prior direction on re-promotion', () => {
      let order: SummarySortCriterion[] = [];
      order = updateSummarySortOrder(order, 'change'); // change desc
      order = updateSummarySortOrder(order, 'change'); // change asc
      order = updateSummarySortOrder(order, 'unaccounted'); // unaccounted desc, change asc
      expect(order).to.deep.equal([
        {column: 'unaccounted', direction: 'desc'},
        {column: 'change', direction: 'asc'},
      ]);

      // Promoting change back keeps its previous 'asc' direction and demotes unaccounted to secondary
      order = updateSummarySortOrder(order, 'change');
      expect(order).to.deep.equal([
        {column: 'change', direction: 'asc'},
        {column: 'unaccounted', direction: 'desc'},
      ]);
    });
  });

  describe('sortSummaryEntries', () => {
    const entries: StatementEntry[] = [
      makeEntry('alex/internal/Savings', {
        startBalance: {amount: '50000', date: '2026-09-01', desc: '', type: 'CONFIRMED'},
        endBalance: {amount: '51000', date: '2026-10-01', desc: '', type: 'CONFIRMED'},
        change: '1000',
        percentChange: 2.0,
        annualizedPercentChange: 24.0,
        inFlows: '1000',
        outFlows: '0',
        addSub: '1000',
        income: '1000',
        totalPayments: '0',
        totalTransfers: '0',
        unaccounted: '0',
      }),
      makeEntry('alex/internal/checking', {
        startBalance: {amount: '20000', date: '2026-09-05', desc: '', type: 'CONFIRMED'},
        endBalance: {amount: '25000', date: '2026-10-05', desc: '', type: 'CONFIRMED'},
        change: '5000',
        percentChange: 25.0,
        annualizedPercentChange: 300.0,
        inFlows: '8000',
        outFlows: '-3000',
        addSub: '5000',
        income: '7000',
        totalPayments: '-2500',
        totalTransfers: '500',
        unaccounted: '100',
      }),
      makeEntry('alex/internal/Brokerage', {
        startBalance: {amount: '100000', date: '2026-08-31', desc: '', type: 'CONFIRMED'},
        endBalance: {amount: '95000', date: '2026-09-30', desc: '', type: 'CONFIRMED'},
        change: '-5000',
        percentChange: -5.0,
        annualizedPercentChange: -60.0,
        inFlows: '0',
        outFlows: '-1000',
        addSub: '-1000',
        income: '0',
        totalPayments: '-500',
        totalTransfers: '-500',
        unaccounted: '-4000',
      }),
    ];

    it('preserves original order when sortOrder is empty', () => {
      const result = sortSummaryEntries(entries, []);
      expect(result.map((e) => e.name)).to.deep.equal([
        'alex/internal/Savings',
        'alex/internal/checking',
        'alex/internal/Brokerage',
      ]);
    });

    const cases: Array<{column: SummarySortColumn; direction: 'asc' | 'desc'; expected: string[]}> = [
      {column: 'account', direction: 'asc', expected: ['Brokerage', 'checking', 'Savings']},
      {column: 'account', direction: 'desc', expected: ['Savings', 'checking', 'Brokerage']},
      {column: 'startDate', direction: 'desc', expected: ['checking', 'Savings', 'Brokerage']},
      {column: 'startBalance', direction: 'desc', expected: ['Brokerage', 'Savings', 'checking']},
      {column: 'endDate', direction: 'asc', expected: ['Brokerage', 'Savings', 'checking']},
      {column: 'endBalance', direction: 'desc', expected: ['Brokerage', 'Savings', 'checking']},
      {column: 'change', direction: 'desc', expected: ['checking', 'Savings', 'Brokerage']},
      {column: 'percentChange', direction: 'desc', expected: ['checking', 'Savings', 'Brokerage']},
      {column: 'annualizedPercentChange', direction: 'asc', expected: ['Brokerage', 'Savings', 'checking']},
      {column: 'inFlows', direction: 'desc', expected: ['checking', 'Savings', 'Brokerage']},
      {column: 'outFlows', direction: 'asc', expected: ['checking', 'Brokerage', 'Savings']},
      {column: 'addSub', direction: 'desc', expected: ['checking', 'Savings', 'Brokerage']},
      {column: 'income', direction: 'desc', expected: ['checking', 'Savings', 'Brokerage']},
      {column: 'expenses', direction: 'asc', expected: ['checking', 'Brokerage', 'Savings']},
      {column: 'transfers', direction: 'desc', expected: ['checking', 'Savings', 'Brokerage']},
      {column: 'unaccounted', direction: 'desc', expected: ['checking', 'Savings', 'Brokerage']},
    ];

    for (const {column, direction, expected} of cases) {
      it(`sorts by ${column} (${direction})`, () => {
        const result = sortSummaryEntries(entries, [{column, direction}]);
        expect(result.map((e) => e.name.split('/').pop())).to.deep.equal(expected);
      });
    }

    it('uses secondary sort criterion to break ties', () => {
      const tied = [
        makeEntry('alex/internal/zeta', {unaccounted: 0, endBalance: {amount: 1000, date: '2026-10-01', desc: '', type: 'CONFIRMED'}}),
        makeEntry('alex/internal/alpha', {unaccounted: 0, endBalance: {amount: 5000, date: '2026-10-01', desc: '', type: 'CONFIRMED'}}),
        makeEntry('alex/internal/beta', {unaccounted: 0, endBalance: {amount: 3000, date: '2026-10-01', desc: '', type: 'CONFIRMED'}}),
      ];
      const byUnaccountedThenEndBal = sortSummaryEntries(tied, [
        {column: 'unaccounted', direction: 'desc'},
        {column: 'endBalance', direction: 'desc'},
      ]);
      expect(byUnaccountedThenEndBal.map((e) => e.name.split('/').pop())).to.deep.equal([
        'alpha',
        'beta',
        'zeta',
      ]);
    });

    it('places missing numeric and date values at the bottom regardless of sort direction', () => {
      const withMissing = [
        makeEntry('alex/internal/missing', {
          startBalance: null,
          endBalance: undefined,
          change: undefined,
          percentChange: Number.NaN,
        }),
        makeEntry('alex/internal/low', {
          startBalance: {amount: 100, date: '2026-09-01', desc: '', type: 'CONFIRMED'},
          endBalance: {amount: 200, date: '2026-10-01', desc: '', type: 'CONFIRMED'},
          change: 100,
          percentChange: 10,
        }),
        makeEntry('alex/internal/high', {
          startBalance: {amount: 500, date: '2026-09-15', desc: '', type: 'CONFIRMED'},
          endBalance: {amount: 900, date: '2026-10-15', desc: '', type: 'CONFIRMED'},
          change: 400,
          percentChange: 80,
        }),
      ];

      expect(
        sortSummaryEntries(withMissing, [{column: 'startBalance', direction: 'asc'}]).map((e) =>
          e.name.split('/').pop()
        )
      ).to.deep.equal(['low', 'high', 'missing']);

      expect(
        sortSummaryEntries(withMissing, [{column: 'startBalance', direction: 'desc'}]).map((e) =>
          e.name.split('/').pop()
        )
      ).to.deep.equal(['high', 'low', 'missing']);

      expect(
        sortSummaryEntries(withMissing, [{column: 'startDate', direction: 'asc'}]).map((e) =>
          e.name.split('/').pop()
        )
      ).to.deep.equal(['low', 'high', 'missing']);

      expect(
        sortSummaryEntries(withMissing, [{column: 'percentChange', direction: 'desc'}]).map((e) =>
          e.name.split('/').pop()
        )
      ).to.deep.equal(['high', 'low', 'missing']);
    });

    it('parses formatted percentage strings and breaks identical basenames by full path', () => {
      const mixed = [
        makeEntry('alex/bankB/checking', {percentChange: '5.5%' as unknown as number}),
        makeEntry('alex/bankA/checking', {percentChange: '12.0%' as unknown as number}),
      ];
      expect(
        sortSummaryEntries(mixed, [{column: 'percentChange', direction: 'desc'}]).map((e) => e.name)
      ).to.deep.equal(['alex/bankA/checking', 'alex/bankB/checking']);

      expect(
        sortSummaryEntries(mixed, [{column: 'account', direction: 'asc'}]).map((e) => e.name)
      ).to.deep.equal(['alex/bankA/checking', 'alex/bankB/checking']);
    });
  });
});
