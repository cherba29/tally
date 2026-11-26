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
      <div class="summary-box">
        <div class="breakdown-grid">
          <div class="equations">
            <div class="equation-row" data-equation="category">
              <span class="income metric">
                <span class="metric-label">Income</span>
                <span class="metric-value">—</span>
              </span>
              <span class="op">+</span>
              <span class="expenses metric">
                <span class="metric-label">Expenses</span>
                <span class="metric-value">—</span>
              </span>
              <span class="op">+</span>
              <span class="metric">
                <span class="metric-label">Transfers</span>
                <span class="metric-value">—</span>
              </span>
            </div>
            <div class="equation-row" data-equation="flow">
              <span class="metric">
                <span class="metric-label">Inflows</span>
                <span class="metric-value">—</span>
              </span>
              <span class="op">+</span>
              <span class="metric">
                <span class="metric-label">Outflows</span>
                <span class="metric-value">—</span>
              </span>
            </div>
          </div>
          <div class="total-cell">
            <span class="op">=</span>
            <div class="total-stack">
              <span class="metric-label">Total</span>
              <span class="metric-value">—</span>
            </div>
          </div>
        </div>
        <div class="reconciliation-bar">
          <span class="reconciliation-formula">
            <span>End − Start:</span>
            <b>—</b>
            <span class="op">−</span>
            <span>Total:</span>
            <b>—</b>
            <span class="op">=</span>
          </span>
          <span class="balanced unaccounted-pill">✓ Unaccounted: —</span>
        </div>
      </div>
      <table>
        <thead>
          <tr>
            <th>#</th>
            <th align="left" aria-sort="none" class="sortable" data-sort-col="account" title="Click to sort by Account">Account</th>
            <th align="left" aria-sort="none" class="sortable" data-sort-col="description" title="Click to sort by Description">Description</th>
            <th aria-sort="none" class="sortable" data-sort-col="recurrence" title="Click to sort by Recurrence">Recurrence</th>
            <th aria-sort="none" class="sortable" data-sort-col="type" title="Click to sort by Type">Type</th>
            <th align="right" aria-sort="none" class="sortable" data-sort-col="amount" title="Secondary sort #2 (descending) — click to sort primarily by Amount">
              Amount<span class="secondary sort-indicator">▼<span class="sort-priority">2</span></span>
            </th>
            <th aria-sort="descending" class="sortable" data-sort-col="date" title="Sorted by Date (descending) — click to reverse">
              Date<span class="primary sort-indicator">▼</span>
            </th>
            <th align="right" aria-sort="none" class="sortable" data-sort-col="balance" title="Click to sort by Balance">Balance</th>
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

    // Default sort is Date descending: [0]=Nov 20 (Card payment), [1]=Nov 18 (Transfer in),
    // [2]=Nov 15 (Paycheck), [3]=Nov 5 (Groceries).
    // Columns: [0]=#, [1]=Account, [2]=Description, [3]=Recurrence, [4]=Type, [5]=Amount, [6]=Date, [7]=Balance
    const trOutCells = rows[0]!.querySelectorAll('td');
    expect(trOutCells).to.have.lengthOf(8);
    expect(trOutCells[0]!.textContent!.trim()).to.equal('1');
    expect(trOutCells[4]!.textContent!.trim()).to.equal('T →');
    expect(trOutCells[4]!.style.backgroundColor).to.equal('');
    expect(trOutCells[7]!.textContent!.trim()).to.equal('3,850.00');

    // Transfer in (positive)
    const trInCells = rows[1]!.querySelectorAll('td');
    expect(trInCells[4]!.textContent!.trim()).to.equal('T ←');
    expect(trInCells[4]!.style.backgroundColor).to.equal('');
    expect(trInCells[7]!.textContent!.trim()).to.equal('4,300.00');

    // Income (projected)
    const incCells = rows[2]!.querySelectorAll('td');
    expect(incCells[4]!.textContent!.trim()).to.equal('I ←');
    expect(incCells[4]!.style.backgroundColor).to.equal('rgb(170, 204, 170)');
    expect(incCells[5]!.classList.contains('projected')).to.equal(true);
    expect(incCells[5]!.textContent!.trim()).to.equal('2,600.00');
    expect(incCells[7]!.textContent!.trim()).to.equal('3,550.00');

    // Expense (confirmed)
    const expCells = rows[3]!.querySelectorAll('td');
    expect(expCells[0]!.textContent!.trim()).to.equal('4');
    expect(expCells[1]!.textContent!.trim()).to.equal('expenses');
    expect(expCells[2]!.textContent!.trim()).to.equal('Groceries');
    expect(expCells[3]!.textContent!.trim()).to.equal('');
    expect(expCells[4]!.textContent!.trim()).to.equal('E →');
    expect(expCells[4]!.style.backgroundColor).to.equal('rgb(204, 170, 170)');
    expect(expCells[5]!.classList.contains('confirmed')).to.equal(true);
    expect(expCells[5]!.textContent!.trim()).to.equal('-50.00');
    expect(expCells[6]!.textContent!.trim()).to.equal('2026-11-05');
    expect(expCells[7]!.textContent!.trim()).to.equal('950.00');

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

  it('displays structured equation summary box and highlights non-zero unaccounted prominently with click-to-copy', async () => {
    const stmt: GqlStatement = {
      name: 'checking',
      month: 'Nov2026',
      isClosed: false,
      isCovered: true,
      isProjectedCovered: true,
      hasProjectedTransfer: false,
      startBalance: {amount: 100000, date: '2026-11-01', desc: '', type: 'CONFIRMED'},
      endBalance: {amount: 125000, date: '2026-12-01', desc: '', type: 'CONFIRMED'},
      income: 50000,
      totalPayments: -20000,
      totalTransfers: -10000,
      inFlows: 50000,
      outFlows: -30000,
      addSub: 20000,
      change: 25000,
      percentChange: 25,
      annualizedPercentChange: 0,
      unaccounted: 5000,
      transactions: [],
    };

    const element = await fixture<BalanceTooltip>(
      html`<balance-tooltip
        .accountName=${'alex/internal/checking'}
        .month=${'Nov2026'}
        .stmt=${stmt}
      ></balance-tooltip>`
    );

    // Table no longer contains summary rows
    expect(element.shadowRoot!.querySelectorAll('table tbody tr')).to.have.lengthOf(2);

    const categoryText = element
      .shadowRoot!.querySelector('[data-equation="category"]')!
      .textContent!.replace(/\s+/g, ' ')
      .trim();
    expect(categoryText).to.equal('Income 500.00 + Expenses -200.00 + Transfers -100.00');

    const flowText = element
      .shadowRoot!.querySelector('[data-equation="flow"]')!
      .textContent!.replace(/\s+/g, ' ')
      .trim();
    expect(flowText).to.equal('Inflows 500.00 + Outflows -300.00');

    const totalText = element
      .shadowRoot!.querySelector('.total-cell')!
      .textContent!.replace(/\s+/g, ' ')
      .trim();
    expect(totalText).to.equal('= Total 200.00');

    // Non-zero unaccounted renders prominent alert bar + clickable badge
    const reconBar = element.shadowRoot!.querySelector('.reconciliation-bar')!;
    expect(reconBar.classList.contains('unaccounted-alert')).to.equal(true);
    const alertPill = element.shadowRoot!.querySelector<HTMLElement>('.unaccounted-pill.nonzero')!;
    expect(alertPill).to.not.equal(null);
    expect(alertPill.getAttribute('role')).to.equal('alert');
    expect(alertPill.textContent).to.include('Unaccounted: 50.00');

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
      setTimeout(() => alertPill.click());
      const copiedEv = (await oneEvent(element, 'copied')) as CustomEvent<{text: string}>;
      expect(copiedEv.detail.text).to.equal('50.00');
      expect(copied).to.deep.equal(['50.00']);
    } finally {
      if (origClipboardDesc) {
        Object.defineProperty(navigator, 'clipboard', origClipboardDesc);
      }
    }

    // String "0" from GraphQL Long scalar is treated as balanced (not non-zero alert)
    element.stmt = {...stmt, unaccounted: '0' as unknown as number};
    await element.updateComplete;
    expect(element.shadowRoot!.querySelector('.reconciliation-bar.unaccounted-alert')).to.equal(null);
    expect(element.shadowRoot!.querySelector('.unaccounted-pill.balanced')!.textContent).to.include(
      'Unaccounted: 0.00'
    );

    // Non-zero string "-1250" from GraphQL Long scalar triggers alert state
    element.stmt = {...stmt, unaccounted: '-1250' as unknown as number};
    await element.updateComplete;
    expect(element.shadowRoot!.querySelector('.reconciliation-bar.unaccounted-alert')).to.not.equal(null);
    expect(element.shadowRoot!.querySelector('.unaccounted-pill.nonzero')!.textContent).to.include(
      'Unaccounted: -12.50'
    );
  });

  it('sorts columns interactively, remembers previous sort order, recomputes running balances, and keeps index non-sortable', async () => {
    const stmt: GqlStatement = {
      name: 'checking',
      month: 'Nov2026',
      isClosed: false,
      isCovered: true,
      isProjectedCovered: true,
      hasProjectedTransfer: false,
      startBalance: {amount: 100000, date: '2026-11-01', desc: '', type: 'CONFIRMED'},
      endBalance: {amount: 120000, date: '2026-12-01', desc: '', type: 'CONFIRMED'},
      income: 50000,
      totalPayments: -30000,
      totalTransfers: 0,
      inFlows: 50000,
      outFlows: -30000,
      addSub: 20000,
      change: 20000,
      percentChange: 20,
      annualizedPercentChange: 0,
      unaccounted: 0,
      transactions: [
        {
          toAccountName: 'expenses',
          description: 'Utility',
          isExpense: true,
          isIncome: false,
          balance: {amount: -10000, date: '2026-11-15', desc: '', type: 'CONFIRMED'},
          balanceFromStart: 140000,
        },
        {
          toAccountName: 'expenses',
          description: 'Groceries',
          isExpense: true,
          isIncome: false,
          balance: {amount: -20000, date: '2026-11-15', desc: '', type: 'CONFIRMED'},
          balanceFromStart: 120000,
        },
        {
          toAccountName: 'payroll',
          description: 'Paycheck',
          isExpense: false,
          isIncome: true,
          balance: {amount: 50000, date: '2026-11-10', desc: '', type: 'CONFIRMED'},
          balanceFromStart: 150000,
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

    const header = (col: string) =>
      element.shadowRoot!.querySelector<HTMLElement>(`th[data-sort-col="${col}"]`)!;
    const descriptions = () =>
      Array.from(element.shadowRoot!.querySelectorAll('tr.highlight')).map(
        (tr) => tr.querySelectorAll('td')[2]!.textContent!.trim()
      );
    const balances = () =>
      Array.from(element.shadowRoot!.querySelectorAll('tr.highlight')).map(
        (tr) => tr.querySelectorAll('td')[7]!.textContent!.trim()
      );

    // Index column is not sortable
    const indexTh = element.shadowRoot!.querySelector('thead th')!;
    expect(indexTh.classList.contains('sortable')).to.equal(false);

    // Default: Date DESC, Amount DESC -> Nov 15 Utility (-100) before Groceries (-200), then Nov 10 Paycheck (+500)
    expect(header('date').getAttribute('aria-sort')).to.equal('descending');
    expect(descriptions()).to.deep.equal(['Utility', 'Groceries', 'Paycheck']);
    // Bottom-to-top from 1,000.00: Paycheck (+500) = 1,500.00 -> Groceries (-200) = 1,300.00 -> Utility (-100) = 1,200.00
    expect(balances()).to.deep.equal(['1,200.00', '1,300.00', '1,500.00']);

    // Click Amount -> Amount DESC primary, Date DESC secondary: Paycheck (+500), Utility (-100), Groceries (-200)
    header('amount').click();
    await element.updateComplete;
    expect(header('amount').getAttribute('aria-sort')).to.equal('descending');
    expect(descriptions()).to.deep.equal(['Paycheck', 'Utility', 'Groceries']);
    // Recomputed bottom-to-top from 1,000.00: Groceries (-200) = 800.00 -> Utility (-100) = 700.00 -> Paycheck (+500) = 1,200.00
    expect(balances()).to.deep.equal(['1,200.00', '700.00', '800.00']);

    // Click Amount again -> toggle to Amount ASC: Groceries (-200), Utility (-100), Paycheck (+500)
    header('amount').click();
    await element.updateComplete;
    expect(header('amount').getAttribute('aria-sort')).to.equal('ascending');
    expect(descriptions()).to.deep.equal(['Groceries', 'Utility', 'Paycheck']);

    // Click Date -> Date DESC primary, remembering Amount ASC as secondary!
    // On Nov 15, Groceries (-200) now comes BEFORE Utility (-100)!
    header('date').click();
    await element.updateComplete;
    expect(header('date').getAttribute('aria-sort')).to.equal('descending');
    expect(header('amount').querySelector('.sort-indicator.secondary')!.textContent).to.equal('▲2');
    expect(descriptions()).to.deep.equal(['Groceries', 'Utility', 'Paycheck']);
    expect(balances()).to.deep.equal(['1,200.00', '1,400.00', '1,500.00']);
  });
});
