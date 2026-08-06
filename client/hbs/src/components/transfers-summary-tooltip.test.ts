import { assert } from '@esm-bundle/chai';
import { html, fixture, expect, oneEvent } from '@open-wc/testing';
import { type GqlMonthTransferSummary } from '../gql_types';
import { TransfersSummaryTooltip } from './transfers-summary-tooltip';

describe('TransferSummaryTooltip component', () => {
  it('is defined', () => {
    const el = document.createElement('transfers-summary-tooltip');
    assert.instanceOf(el, TransfersSummaryTooltip);
  });

  it('renders with default property values', async () => {
    const closePopup = () => {};
    const accountPath = 'john/internal/test-account1';
    const months: string[] = ['Jul2026', 'Jun2026'];
    const monthlyData: GqlMonthTransferSummary[] = [{
      externalTransfers: 100,
      internalTransfers: 50,
      totalExternalTransfers: 200,
      totalInternalTransfers: 300,
      totalExternalTransfersAnnualPrct: 0,
      totalExternalTransfersPrct: 0,
      totalInternalTransfersAnnualPrct: 0,
      totalInternalTransfersPrct: 0,
      totalMonthTransfers: 11,
      totalTransfers: 160,
      totalAnnualPrct: 1.2,
      unaccounted: 15
    }, {
      externalTransfers: 101,
      internalTransfers: 51,
      totalExternalTransfers: 201,
      totalInternalTransfers: 301,
      totalExternalTransfersAnnualPrct: 20,
      totalExternalTransfersPrct: 0.1,
      totalInternalTransfersAnnualPrct: 0,
      totalInternalTransfersPrct: 0,
      totalMonthTransfers: 152,
      totalTransfers: 152,
      totalAnnualPrct: 1.4,
      unaccounted: 10
    }];
    
    const monthRangeChange = () => {};

    const element = await fixture<TransfersSummaryTooltip>(
      html`<transfers-summary-tooltip
        .accountPath=${accountPath}
        .months=${months}
        .monthlyData=${monthlyData}
        @month-range-change=${monthRangeChange}
        @close=${closePopup}
      ></transfers-summary-tooltip>`
    );

    expect(element.shadowRoot).not.to.be.null;
    assert.shadowDom.equal(
      element, 
      `<span>XXX</span>
       <period-buttons></period-buttons>
       <span style="float:right;">john/internal/test-account1</span>
      <table>
        <thead>
          <tr>
            <th style="min-width:20px"></th>
            <th style="min-width:40px">Month</th>
            <th style="min-width:50px">Internal</th>
            <th style="min-width:50px">External</th>
            <th style="min-width:50px">Total</th>
            <th style="min-width:60px">Tot Internal</th>
            <th style="min-width:40px">Internal %</th>
            <th style="min-width:40px">Internal Annl %</th>
            <th style="min-width:60px">Tot External</th>
            <th style="min-width:40px">External %</th>
            <th style="min-width:40px">External Annl %</th>
            <th style="min-width:60px">Total</th>
            <th style="min-width:40px">Tot Annl %</th>
            <th style="min-width:60px">Unaccounted</th>
           </tr>
        </thead>
        <tbody>
          <tr class="highlight">
            <td align="middle">1</td>
            <td>Jul2026</td>
            <td align="right">0.50</td>
            <td align="right">1.00</td>
            <td align="right">0.11</td>
            <td align="right">3.00</td>
            <td align="right">0</td>
            <td align="right">0</td>
            <td align="right">2.00</td>
            <td align="right">0</td>
            <td align="right">0</td>
            <td align="right">1.60</td>
            <td align="right">1.2</td>
            <td align="right">0.15</td>
          </tr>
          <tr class="highlight">
            <td align="middle">2</td>
            <td>Jun2026</td>
            <td align="right">0.51</td>
            <td align="right">1.01</td>
            <td align="right">1.52</td>
            <td align="right">3.01</td>
            <td align="right">0</td>
            <td align="right">0</td>
            <td align="right">2.01</td>
            <td align="right">0.1</td>
            <td align="right">20</td>
            <td align="right">1.52</td>
            <td align="right">1.4</td>
            <td align="right">0.10</td>
          </tr>
        </tbody>
      </table>`
    );
    const buttons = element.shadowRoot!.querySelector('period-buttons');
    expect(buttons).to.exist;
    await buttons!.updateComplete;
    const button = buttons!.shadowRoot!.querySelector('[key="MONTH_9"]') as HTMLElement;
    expect(button).to.exist;
    setTimeout(() => {
      button.click();
    });
    const { detail } = await oneEvent(element, 'month-range-change');
    // 9 months back from Jul2026 is Nov2025.
    expect(detail.startMonth.toString()).to.equal('Nov2025');
    expect(detail.endMonth.toString()).to.equal('Jul2026');
  });
});
