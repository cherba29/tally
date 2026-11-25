import { assert } from '@esm-bundle/chai';
import { html, fixture, expect, oneEvent } from '@open-wc/testing';
import { BalanceTooltip } from './balance-tooltip';
import { type GqlStatement } from '../gql_types';

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

  it('renders transactions with colors, codes, projected/confirmed classes, and supports close & clipboard copy', async () => {
    const stmt: GqlStatement = {
      name: 'checking',
      month: 'Nov2026',
      isClosed: false,
      isCovered: true,
      isProjectedCovered: true,
      hasProjectedTransfer: true,
      startBalance: {amount: 100000, date: '2026-11-01', desc: '', type: 'CONFIRMED'},
      endBalance: {amount: 385000, date: '2026-12-01', desc: '', type: 'PROJECTED'},
      income: 260000,
      totalPayments: -5000,
      totalTransfers: 30000,
      inFlows: 335000,
      outFlows: -50000,
      addSub: 285000,
      change: 285000,
      percentChange: 285,
      annualizedPercentChange: 0,
      unaccounted: 0,
      transactions: [
        {
          toAccountName: 'expenses',
          description: 'Groceries',
          isExpense: true,
          isIncome: false,
          balance: {amount: -5000, date: '2026-11-05', desc: '', type: 'CONFIRMED'},
          balanceFromStart: 95000,
        },
        {
          toAccountName: 'payroll',
          description: 'Paycheck',
          isExpense: false,
          isIncome: true,
          balance: {amount: 260000, date: '2026-11-15', desc: '', type: 'PROJECTED'},
          balanceFromStart: 355000,
        },
        {
          toAccountName: 'savings',
          description: 'Transfer in',
          isExpense: false,
          isIncome: false,
          balance: {amount: 75000, date: '2026-11-18', desc: '', type: 'CONFIRMED'},
          balanceFromStart: 430000,
        },
        {
          toAccountName: 'credit-card',
          description: 'Card payment',
          isExpense: false,
          isIncome: false,
          balance: {amount: -45000, date: '2026-11-20', desc: '', type: 'CONFIRMED'},
          balanceFromStart: 385000,
        },
      ],
    };

    const element = await fixture<BalanceTooltip>(
      html`<balance-tooltip
        .accountName=${'alex/internal/checking'}
        .month=${'Nov2026'}
        .stmt=${stmt}
      ></balance-tooltip>`
    );

    const rows = Array.from(element.shadowRoot!.querySelectorAll('tr.highlight'));
    expect(rows).to.have.lengthOf(4);

    // Expense (confirmed)
    const expCells = rows[0]!.querySelectorAll('td');
    expect(expCells[1]!.textContent!.trim()).to.equal('E →');
    expect(expCells[1]!.style.backgroundColor).to.equal('rgb(204, 170, 170)');
    expect(expCells[2]!.classList.contains('confirmed')).to.equal(true);
    expect(expCells[2]!.textContent!.trim()).to.equal('-50.00');

    // Income (projected)
    const incCells = rows[1]!.querySelectorAll('td');
    expect(incCells[1]!.textContent!.trim()).to.equal('I ←');
    expect(incCells[1]!.style.backgroundColor).to.equal('rgb(170, 204, 170)');
    expect(incCells[2]!.classList.contains('projected')).to.equal(true);
    expect(incCells[2]!.textContent!.trim()).to.equal('2,600.00');

    // Transfer in (positive)
    const trInCells = rows[2]!.querySelectorAll('td');
    expect(trInCells[1]!.textContent!.trim()).to.equal('T ←');
    expect(trInCells[1]!.style.backgroundColor).to.equal('');

    // Transfer out (negative)
    const trOutCells = rows[3]!.querySelectorAll('td');
    expect(trOutCells[1]!.textContent!.trim()).to.equal('T →');
    expect(trOutCells[1]!.style.backgroundColor).to.equal('');

    // Close button dispatches 'close' event
    const closeSpan = element.shadowRoot!.querySelector('span')!;
    setTimeout(() => closeSpan.click());
    const closeEvent = await oneEvent(element, 'close');
    expect(closeEvent.type).to.equal('close');

    // Clicking running balance cell copies formatted amount to clipboard
    const copied: string[] = [];
    const origClipboardDesc = Object.getOwnPropertyDescriptor(navigator, 'clipboard');
    Object.defineProperty(navigator, 'clipboard', {
      value: {
        writeText: (text: string) => {
          copied.push(text);
          return Promise.resolve();
        },
      },
      configurable: true,
    });
    try {
      expCells[4]!.click();
      expect(copied).to.deep.equal(['950.00']);

      element.copyToClipBoard(null);
      element.copyToClipBoard(undefined);
      expect(copied).to.deep.equal(['950.00']);
    } finally {
      if (origClipboardDesc) {
        Object.defineProperty(navigator, 'clipboard', origClipboardDesc);
      }
    }
  });
});
