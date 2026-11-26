import { assert } from '@esm-bundle/chai';
import { html, fixture, expect, oneEvent } from '@open-wc/testing';
import { BalanceTooltip, type RecurringClickEventData } from './balance-tooltip';
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
      `<div class="header">
        <button aria-label="Close" class="close-btn" title="Close statement" type="button">×</button>
        <span class="title">${accountName}</span>
        <span class="meta-pill">${month}</span>
      </div>
      <table>
        <thead>
          <tr>
            <th>#</th>
            <th align="left">Account</th>
            <th align="left">Description</th>
            <th>Recurrence</th>
            <th>Type</th>
            <th align="right">Amount</th>
            <th>Date</th>
            <th align="right">Balance</th>
          </tr>
        </thead>
        <tbody>
          <tr class="balance-row">
            <th align="left" colspan="6">Ending Balance</th>
            <th>n/a</th>
            <th align="right">—</th>
          </tr>
          <tr class="balance-row">
            <th align="left" colspan="6">Starting Balance</th>
            <th>n/a</th>
            <th align="right">—</th>
          </tr>
          <tr class="summary-row">
            <td align="right" colspan="5"><b>Income</b></td>
            <td align="right">—</td>
            <td colspan="2"></td>
          </tr>
          <tr class="summary-row">
            <td align="right" colspan="5"><b>Expenses</b></td>
            <td align="right">—</td>
            <td colspan="2"></td>
          </tr>
          <tr class="summary-row">
            <td align="right" colspan="5"><b>Transfers</b></td>
            <td align="right">—</td>
            <td colspan="2"></td>
          </tr>
          <tr class="summary-row">
            <td align="right" colspan="5"><b>Inflows</b></td>
            <td align="right">—</td>
            <td colspan="2"></td>
          </tr>
          <tr class="summary-row">
            <td align="right" colspan="5"><b>Outflows</b></td>
            <td align="right">—</td>
            <td colspan="2"></td>
          </tr>
          <tr class="summary-row">
            <td align="right" colspan="5"><b>Total</b></td>
            <td align="right">—</td>
            <td colspan="2"></td>
          </tr>
          <tr class="summary-row">
            <td align="right" colspan="5"><b>Unaccounted</b></td>
            <td align="right">—</td>
            <td colspan="2"></td>
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

    // Expense (confirmed) — columns: [0]=#, [1]=Account, [2]=Description, [3]=Recurrence, [4]=Type, [5]=Amount, [6]=Date, [7]=Balance
    const expCells = rows[0]!.querySelectorAll('td');
    expect(expCells).to.have.lengthOf(8);
    expect(expCells[0]!.textContent!.trim()).to.equal('1');
    expect(expCells[1]!.textContent!.trim()).to.equal('expenses');
    expect(expCells[2]!.textContent!.trim()).to.equal('Groceries');
    expect(expCells[3]!.textContent!.trim()).to.equal('');
    expect(expCells[4]!.textContent!.trim()).to.equal('E →');
    expect(expCells[4]!.style.backgroundColor).to.equal('rgb(204, 170, 170)');
    expect(expCells[5]!.classList.contains('confirmed')).to.equal(true);
    expect(expCells[5]!.textContent!.trim()).to.equal('-50.00');
    expect(expCells[6]!.textContent!.trim()).to.equal('2026-11-05');
    expect(expCells[7]!.textContent!.trim()).to.equal('950.00');

    // Income (projected)
    const incCells = rows[1]!.querySelectorAll('td');
    expect(incCells[4]!.textContent!.trim()).to.equal('I ←');
    expect(incCells[4]!.style.backgroundColor).to.equal('rgb(170, 204, 170)');
    expect(incCells[5]!.classList.contains('projected')).to.equal(true);
    expect(incCells[5]!.textContent!.trim()).to.equal('2,600.00');

    // Transfer in (positive)
    const trInCells = rows[2]!.querySelectorAll('td');
    expect(trInCells[4]!.textContent!.trim()).to.equal('T ←');
    expect(trInCells[4]!.style.backgroundColor).to.equal('');

    // Transfer out (negative)
    const trOutCells = rows[3]!.querySelectorAll('td');
    expect(trOutCells[4]!.textContent!.trim()).to.equal('T →');
    expect(trOutCells[4]!.style.backgroundColor).to.equal('');

    // Close button dispatches 'close' event
    const closeBtn = element.shadowRoot!.querySelector<HTMLButtonElement>('button.close-btn')!;
    setTimeout(() => closeBtn.click());
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
      expCells[7]!.click();
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

  it('renders recurring badge only for recurring transactions and dispatches recurring-click with anchorRect', async () => {
    const stmt: GqlStatement = {
      name: 'checking',
      month: 'Nov2026',
      isClosed: false,
      isCovered: true,
      isProjectedCovered: true,
      hasProjectedTransfer: false,
      startBalance: {amount: 100000, date: '2026-11-01', desc: '', type: 'CONFIRMED'},
      endBalance: {amount: 82500, date: '2026-12-01', desc: '', type: 'CONFIRMED'},
      income: 0,
      totalPayments: -17500,
      totalTransfers: 0,
      inFlows: 0,
      outFlows: -17500,
      addSub: -17500,
      change: -17500,
      percentChange: -17.5,
      annualizedPercentChange: 0,
      unaccounted: 0,
      transactions: [
        {
          toAccountName: 'expenses',
          description: 'Fiber internet',
          isExpense: true,
          isIncome: false,
          balance: {amount: -7500, date: '2026-11-18', desc: '', type: 'CONFIRMED'},
          balanceFromStart: 92500,
          recurringGroupId: 'grp-internet',
          recurringCount: 5,
          recurringCadence: 'MONTHLY',
        },
        {
          toAccountName: 'expenses',
          description: 'One-off jacket',
          isExpense: true,
          isIncome: false,
          balance: {amount: -10000, date: '2026-11-10', desc: '', type: 'CONFIRMED'},
          balanceFromStart: 82500,
          recurringGroupId: null,
          recurringCount: null,
          recurringCadence: null,
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
    const firstRowCells = rows[0]!.querySelectorAll('td');
    const badge = firstRowCells[3]!.querySelector<HTMLButtonElement>('button.recurring-badge');
    expect(badge).to.not.equal(null);
    expect(badge!.textContent?.trim()).to.equal('↻ 5');
    expect(badge!.getAttribute('title')).to.include('Monthly');
    expect(badge!.getAttribute('title')).to.include('5 occurrences');

    // Non-recurring row has no badge in Recurrence column
    expect(rows[1]!.querySelectorAll('td')[3]!.querySelector('button.recurring-badge')).to.equal(null);

    // Clicking badge dispatches bubbling recurring-click event and stops native click propagation
    let parentClicked = false;
    element.addEventListener('click', () => {
      parentClicked = true;
    });
    setTimeout(() => badge!.click());
    const ev = (await oneEvent(element, 'recurring-click')) as CustomEvent<RecurringClickEventData>;
    expect(ev.detail.groupId).to.equal('grp-internet');
    expect(ev.detail.accountPath).to.equal('alex/internal/checking');
    expect(ev.detail.selectedDate).to.equal('2026-11-18');
    expect(ev.detail.selectedAmount).to.equal(-7500);
    expect(ev.detail.selectedDescription).to.equal('Fiber internet');
    expect(ev.detail.anchorRect).to.not.equal(undefined);
    expect(parentClicked).to.equal(false);
  });
});
