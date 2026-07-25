import { assert } from '@esm-bundle/chai';
import { html, fixture, expect } from '@open-wc/testing';
import { type GqlMonthTransferSummary } from '../gql_types';
import { TransfersSummaryTooltip } from './transfers-summary-tooltip';

describe('BalanceTooltip component', () => {
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
      totalMonthTransfers: undefined,
      totalTransfers: undefined,
      unaccounted: undefined
    }, {
      externalTransfers: 101,
      internalTransfers: 51,
      totalExternalTransfers: 201,
      totalInternalTransfers: 301,
      totalExternalTransfersAnnualPrct: 0,
      totalExternalTransfersPrct: 0,
      totalInternalTransfersAnnualPrct: 0,
      totalInternalTransfersPrct: 0,
      totalMonthTransfers: undefined,
      totalTransfers: undefined,
      unaccounted: undefined
    }];
    
    const element = await fixture<TransfersSummaryTooltip>(
      html`<transfers-summary-tooltip
        .accountPath=${accountPath}
        .months=${months}
        .monthlyData=${monthlyData}
        @close=${closePopup}
      ></transfers-summary-tooltip>`
    );

    expect(element.shadowRoot).not.to.be.null;
    assert.shadowDom.equal(
      element, 
      `<span>XXX</span>
       <span style="float:right;">john/internal/test-account1</span>
      <table>
        <thead>
          <tr>
            <th style="min-width:20px"></th>
            <th style="min-width:50px">Month</th>
            <th style="min-width:50px">Internal</th>
            <th style="min-width:50px">External</th>
            <th style="min-width:50px">Total</th>
            <th style="min-width:60px">Tot Internal</th>
            <th style="min-width:60px">Tot External</th>
            <th style="min-width:60px">Total</th>
           </tr>
        </thead>
        <tbody>
          <tr class="highlight">
            <td align="middle">1</td>
            <td>Jul2026</td>
            <td align="right">0.50</td>
            <td align="right">1.00</td>
            <td align="right">1.50</td>
            <td align="right">3.00</td>
            <td align="right">2.00</td>
            <td align="right">5.00</td>
          </tr>
          <tr class="highlight">
            <td align="middle">2</td>
            <td>Jun2026</td>
            <td align="right">0.51</td>
            <td align="right">1.01</td>
            <td align="right">1.52</td>
            <td align="right">3.01</td>
            <td align="right">2.01</td>
            <td align="right">5.02</td>
          </tr>
        </tbody>
      </table>`
    );
  });
});
