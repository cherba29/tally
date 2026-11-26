import { assert } from '@esm-bundle/chai';
import { html, fixture, expect, oneEvent } from '@open-wc/testing';
import { type StatementEntry } from '../utils';
import { type GqlSummaryStatement } from '../gql_types';
import { BalanceSummaryTooltip } from './balance-summary-tooltip';
import { Month } from '../month';

describe('BalanceSummaryTooltip component', () => {

  it('renders with default property values', async () => {
    const accountName = 'test-account1';
    const startMonth = new Month(2026, 4);
    const endMonth = new Month(2026, 6);
    const closePopup = () => {};
    const monthRangeChange = () => {};
    const summaryEntries: StatementEntry[] = [];
    const summary: GqlSummaryStatement = {
      addSub: undefined,
      annualizedPercentChange: 0,
      change: undefined,
      inFlows: undefined,
      income: undefined,
      month: undefined,
      name: '',
      outFlows: undefined,
      percentChange: 0,
      totalPayments: undefined,
      totalTransfers: undefined,
      unaccounted: undefined
    };

    const element = await fixture<BalanceSummaryTooltip>(
      html`<balance-summary-tooltip 
        .accountName=${accountName}>
        .startMonth=${startMonth}
        .endMonth=${endMonth}
        .statementEntries=${summaryEntries}
        .summary=${summary}
        @close=${closePopup}
        @month-range-change=${monthRangeChange}
      </balance-summary-tooltip>`
    );

    expect(element.shadowRoot).not.to.be.null;
    assert.shadowDom.equal(
      element,
      `<div class="header">
        <button aria-label="Close" class="close-btn" title="Close summary" type="button">×</button>
        <span class="title">${accountName}</span>
        <span class="meta-pill">–</span>
      </div>
      <div class="controls-bar">
        <period-buttons></period-buttons>
      </div>
      <table>
        <thead>
          <tr>
            <th aria-sort="none" class="sortable" data-sort-col="account" style="min-width:170px" title="Click to sort by Account">Account</th>
            <th aria-sort="none" class="sortable" data-sort-col="startDate" style="min-width:60px" title="Click to sort by Start Date">Start Date</th>
            <th aria-sort="none" class="sortable" data-sort-col="startBalance" style="min-width:50px" title="Click to sort by Start Balance">Start<br>Balance</th>
            <th aria-sort="none" class="sortable" data-sort-col="endDate" style="min-width:60px" title="Click to sort by End Date">End Date</th>
            <th aria-sort="none" class="sortable" data-sort-col="endBalance" style="min-width:50px" title="Click to sort by End Balance">End<br>Balance</th>
            <th aria-sort="none" class="sortable" data-sort-col="change" style="min-width:50px" title="Click to sort by Change">Change</th>
            <th aria-sort="none" class="sortable" data-sort-col="percentChange" style="min-width:50px" title="Click to sort by Prct Change">Prct<br>Change</th>
            <th aria-sort="none" class="sortable" data-sort-col="annualizedPercentChange" style="min-width:50px" title="Click to sort by An Prct Change">An Prct<br>Change</th>
            <th aria-sort="none" class="sortable" data-sort-col="inFlows" style="min-width:50px" title="Click to sort by Inflows">Inflows</th>
            <th aria-sort="none" class="sortable" data-sort-col="outFlows" style="min-width:50px" title="Click to sort by OutFlows">OutFlows</th>
            <th aria-sort="none" class="sortable" data-sort-col="addSub" style="min-width:50px" title="Click to sort by AddSub">AddSub</th>
            <th aria-sort="none" class="sortable" data-sort-col="income" style="min-width:50px" title="Click to sort by Income">Income</th>
            <th aria-sort="none" class="sortable" data-sort-col="expenses" style="min-width:50px" title="Click to sort by Expenses">Expenses</th>
            <th aria-sort="none" class="sortable" data-sort-col="transfers" style="min-width:50px" title="Click to sort by Transfers">Transfers</th>
            <th aria-sort="none" class="sortable" data-sort-col="unaccounted" style="min-width:50px" title="Click to sort by Unaccounted">Unaccounted</th>
          </tr>
        </thead>
        <tbody>
          <tr class="total-row">
            <td><b>Total</b></td>
            <td align="middle">n/a</td>
            <td class="projected">—</td>
            <td align="middle">n/a</td>
            <td class="projected">—</td>
            <td align="right">—</td>
            <td align="right"></td>
            <td align="right"></td>
            <td align="right">—</td>
            <td align="right">—</td>
            <td align="right">—</td>
            <td align="right">—</td>
            <td align="right">—</td>
            <td align="right">—</td>
            <td align="right">—</td>
          </tr>
        </tbody>
      </table>`
    );

    const closeBtn = element.shadowRoot!.querySelector<HTMLButtonElement>('button.close-btn')!;
    setTimeout(() => closeBtn.click());
    const closeEvent = await oneEvent(element, 'close');
    expect(closeEvent.type).to.equal('close');
  });

  it('sorts all columns interactively, remembers secondary order, and keeps Total row pinned at bottom', async () => {
    const startMonth = new Month(2026, 8);
    const endMonth = new Month(2026, 9);
    const makeRow = (name: string, endAmt: number, change: number, unaccounted: number): StatementEntry => ({
      name,
      stmt: {
        name,
        month: 'Oct2026',
        isClosed: false,
        isCovered: true,
        isProjectedCovered: true,
        hasProjectedTransfer: false,
        startBalance: {amount: endAmt - change, date: '2026-09-01', desc: '', type: 'CONFIRMED'},
        endBalance: {amount: endAmt, date: '2026-10-01', desc: '', type: 'CONFIRMED'},
        change,
        percentChange: 10,
        annualizedPercentChange: 120,
        inFlows: change > 0 ? change : 0,
        outFlows: change < 0 ? change : 0,
        addSub: change - unaccounted,
        income: 0,
        totalPayments: 0,
        totalTransfers: 0,
        unaccounted,
        transactions: [],
      },
    });

    const entries: StatementEntry[] = [
      makeRow('john/internal/checking', 20000, 1000, 0),
      makeRow('john/internal/brokerage', 90000, 1000, 500),
      makeRow('john/internal/savings', 50000, 5000, 0),
    ];

    const element = await fixture<BalanceSummaryTooltip>(
      html`<balance-summary-tooltip
        .accountName=${'john/internal'}
        .startMonth=${startMonth}
        .endMonth=${endMonth}
        .statementEntries=${entries}
      ></balance-summary-tooltip>`
    );

    const getRowAccounts = () =>
      Array.from(element.shadowRoot!.querySelectorAll('tbody tr.highlight')).map((tr) =>
        tr.querySelector('td')!.textContent!.trim()
      );

    // Initial order preserves server order with 1-based row numbers.
    expect(getRowAccounts()).to.deep.equal(['1 checking', '2 brokerage', '3 savings']);

    // Click End Balance -> sorts descending; primary indicator appears; Total stays last.
    const endBalHeader = element.shadowRoot!.querySelector<HTMLTableCellElement>('th[data-sort-col="endBalance"]')!;
    endBalHeader.click();
    await element.updateComplete;
    expect(endBalHeader.getAttribute('aria-sort')).to.equal('descending');
    expect(endBalHeader.querySelector('.sort-indicator.primary')?.textContent).to.equal('▼');
    expect(getRowAccounts()).to.deep.equal(['1 brokerage', '2 savings', '3 checking']);

    // Click End Balance again -> reverses to ascending.
    endBalHeader.click();
    await element.updateComplete;
    expect(endBalHeader.getAttribute('aria-sort')).to.equal('ascending');
    expect(endBalHeader.querySelector('.sort-indicator.primary')?.textContent).to.equal('▲');
    expect(getRowAccounts()).to.deep.equal(['1 checking', '2 savings', '3 brokerage']);

    // Click Change -> Change becomes primary (desc), End Balance becomes secondary (asc, badge "2")
    // breaking the tie between checking (20000) and brokerage (90000) both having change=1000.
    const changeHeader = element.shadowRoot!.querySelector<HTMLTableCellElement>('th[data-sort-col="change"]')!;
    changeHeader.click();
    await element.updateComplete;
    expect(changeHeader.getAttribute('aria-sort')).to.equal('descending');
    expect(endBalHeader.getAttribute('aria-sort')).to.equal('none');
    expect(endBalHeader.querySelector('.sort-indicator.secondary')?.textContent).to.equal('▲2');
    expect(getRowAccounts()).to.deep.equal(['1 savings', '2 checking', '3 brokerage']);

    // Total row remains pinned at the bottom of tbody.
    const allBodyRows = Array.from(element.shadowRoot!.querySelectorAll('tbody tr'));
    expect(allBodyRows[allBodyRows.length - 1]!.classList.contains('total-row')).to.be.true;
  });
});
