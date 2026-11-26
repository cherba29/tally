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
});
