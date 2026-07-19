import { assert } from '@esm-bundle/chai';
import { html, fixture, expect } from '@open-wc/testing';
import { BalanceTooltip } from './balance-tooltip';

describe('BalanceTooltip component', () => {
  it('is defined', () => {
    const el = document.createElement('balance-tooltip');
    assert.instanceOf(el, BalanceTooltip);
  });

  it('renders with default property values', async () => {
    const accountName = 'test-account1';
    const month = 'May2026';
    const statement = undefined;
    const closePopup = () => {};

    const element = await fixture<BalanceTooltip>(
      html`<balance-tooltip 
        .accountName=${accountName}
        .month=${month}
        .stmt=${statement}
        @close=${closePopup}
      ></balance-tooltip>`
    );

    expect(element.shadowRoot).not.to.be.null;
    assert.shadowDom.equal(
      element, 
      `<span>XXX</span>
      <span style="float:right;">${accountName} - ${month}</span>
      <table style="table-layout: auto; width: 500px">
        <tbody>
          <tr>
            <th align="left">Ending Balance</th>
            <th>Type</th>
            <th></th>
            <th>n/a</th>
            <th align="right">—</th>
          </tr>
          <tr>
            <th align="left">Starting Balance</th>
            <th></th>
            <th></th>
            <th>n/a</th>
            <th align="right" style="width:50px">—</th>
          </tr>
          <tr>
            <td align="right"><b>Income</b></td>
            <td></td>
            <td align="right">—</td>
            <td></td>
            <td></td>
          </tr>
          <tr>
            <td align="right"><b>Expenses</b></td>
            <td></td>
            <td align="right">—</td>
            <td></td>
            <td></td>
          </tr>
          <tr>
            <td align="right"><b>Transfers</b></td>
            <td></td>
            <td align="right">—</td>
            <td></td>
            <td></td>
          </tr>
          <tr>
            <td align="right"><b>Inflows</b></td>
            <td></td>
            <td align="right">—</td>
            <td></td>
            <td></td>
          </tr>
          <tr>
            <td align="right"><b>Outflows</b></td>
            <td></td>
            <td align="right">—</td>
            <td></td>
            <td></td>
          </tr>
          <tr>
            <td align="right"><b>Total</b></td>
            <td></td>
            <td align="right">—</td>
            <td></td>
            <td></td>
          </tr>
          <tr>
            <td align="right"><b>Unaccounted</b></td>
            <td></td>
            <td align="right">—</td>
            <td></td>
            <td></td>
          </tr>
        </tbody>
      </table>`
    );
  });
});
