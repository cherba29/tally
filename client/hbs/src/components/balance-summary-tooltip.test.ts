import { assert } from '@esm-bundle/chai';
import { html, fixture, expect } from '@open-wc/testing';
import { type StatementEntry } from '../utils';
import { type GqlSummaryStatement } from '../gql_types';
import { BalanceSummaryTooltip } from './balance-summary-tooltip';

describe('BalanceSummaryTooltip component', () => {
  it('is defined', () => {
    const el = document.createElement('balance-summary-tooltip');
    assert.instanceOf(el, BalanceSummaryTooltip);
  });

  it('renders with default property values', async () => {
    const accountName = 'test-account1';
    const startMonth = 'May2026';
    const endMonth = 'Jul2026';
    const closePopup = () => {};
    const monthRangeChange = () => {};
    const summaryEntries: StatementEntry[] = [];
    const summary: GqlSummaryStatement = {
      accounts: [],
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
      `<span>XXX</span>
      <button key="MONTH_1">1m</button>
      <button key="MONTH_3">3m</button>
      <button key="MONTH_6">6m</button>
      <button key="MONTH_9">9m</button>
      <button key="YTD">ytd</button>
      <button key="YEAR_1">1y</button>
      <button key="YEAR_2">2y</button>
      <button key="YEAR_3">3y</button>
      <button key="YEAR_5">5y</button>
      <button key="YEAR_7">7y</button>
      <button key="YEAR_10">10y</button>
      <button key="YEAR_15">15y</button>
      <button key="YEAR_20">20y</button>
      <button key="YEAR_30">30y</button>
      <button key="YEAR_MAX">Max</button>
      <span style="float:right;">${accountName}  -  ()</span>
      <table>
        <thead>
          <tr>
            <th style="min-width:170px">Account</th>
            <th style="min-width:60px">Start Date</th>
            <th style="min-width:50px">Start<br>Balance</th>
            <th style="min-width:60px">End Date</th>
            <th style="min-width:50px">End<br>Balance</th>
            <th style="min-width:50px">Change</th>
            <th style="min-width:50px">Prct<br>Change</th>
            <th style="min-width:50px">An Prct<br>Change</th>
            <th style="min-width:50px">Inflows</th>
            <th style="min-width:50px">OutFlows</th>
            <th style="min-width:50px">AddSub</th>
            <th style="min-width:50px">Income</th>
            <th style="min-width:50px">Expenses</th>
            <th style="min-width:50px">Transfers</th>
            <th style="min-width:50px">Unaccounted</th>
          </tr>
        </thead>
        <tbody>
          <tr>
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
  });
});
