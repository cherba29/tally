import {assert} from '@esm-bundle/chai';
import {expect, fixture, html, oneEvent} from '@open-wc/testing';
import './recurring-tooltip';
import {RecurringTooltip} from './recurring-tooltip';
import {type GqlRecurringGroup} from '../gql_types';

describe('RecurringTooltip component', () => {
  it('is defined and renders empty when group is undefined', async () => {
    const el = await fixture<RecurringTooltip>(html`<recurring-tooltip></recurring-tooltip>`);
    assert.instanceOf(el, RecurringTooltip);
    expect(el.shadowRoot!.querySelector('table')).to.equal(null);
  });

  it('renders fixed recurring group, highlights matching row, marks other-account rows, supports close and copy', async () => {
    const group: GqlRecurringGroup = {
      id: 'grp-123',
      label: 'Streaming & cloud subscriptions',
      cadence: 'MONTHLY',
      occurrenceCount: 4,
      averageAmount: 4500,
      minAmount: 4500,
      maxAmount: 4500,
      totalAmount: 18000,
      isFixedAmount: true,
      typicalDayOfMonth: 22,
      transactions: [
        {
          month: 'Nov2026',
          fromAccountName: 'checking',
          fromAccountPath: 'alex/internal/checking',
          toAccountName: 'expenses',
          toAccountPath: 'alex/external/expenses',
          counterpartyAccountName: 'expenses',
          isIncome: false,
          isExpense: true,
          isCurrentAccount: true,
          description: 'Streaming & cloud subscriptions',
          balance: {amount: -4500, date: '2026-11-22', desc: '', type: 'CONFIRMED'},
        },
        {
          month: 'Oct2026',
          fromAccountName: 'credit-card',
          fromAccountPath: 'alex/internal/credit-card',
          toAccountName: 'expenses',
          toAccountPath: 'alex/external/expenses',
          counterpartyAccountName: 'expenses',
          isIncome: false,
          isExpense: true,
          isCurrentAccount: false,
          description: 'Streaming & cloud subscriptions',
          balance: {amount: -4500, date: '2026-10-22', desc: '', type: 'PROJECTED'},
        },
        {
          month: 'Sep2026',
          fromAccountName: 'payroll',
          fromAccountPath: 'alex/external/payroll',
          toAccountName: 'checking',
          toAccountPath: 'alex/internal/checking',
          counterpartyAccountName: 'payroll',
          isIncome: true,
          isExpense: false,
          isCurrentAccount: true,
          description: 'Income test',
          balance: {amount: 4500, date: '2026-09-22', desc: '', type: 'CONFIRMED'},
        },
        {
          month: 'Aug2026',
          fromAccountName: 'checking',
          fromAccountPath: 'alex/internal/checking',
          toAccountName: 'savings',
          toAccountPath: 'alex/internal/savings',
          counterpartyAccountName: 'savings',
          isIncome: false,
          isExpense: false,
          isCurrentAccount: true,
          description: 'Transfer out test',
          balance: {amount: -4500, date: '2026-08-22', desc: '', type: 'CONFIRMED'},
        },
      ],
    };

    const el = await fixture<RecurringTooltip>(
      html`<recurring-tooltip
        .group=${group}
        .highlightKey=${{
          date: '2026-11-22',
          amount: -4500,
          description: 'Streaming & cloud subscriptions',
        }}
      ></recurring-tooltip>`
    );

    expect(el.shadowRoot!.querySelector('.title')!.textContent).to.equal(
      'Streaming & cloud subscriptions'
    );
    expect(el.shadowRoot!.querySelector('.cadence-pill')!.textContent).to.include('Monthly');
    expect(el.shadowRoot!.querySelector('.cadence-pill')!.textContent).to.include('4');

    const summaryText = el.shadowRoot!.querySelector('.summary-bar')!.textContent!;
    expect(summaryText).to.include('Fixed:');
    expect(summaryText).to.include('45.00');
    expect(summaryText).to.include('Total:');
    expect(summaryText).to.include('180.00');
    expect(summaryText).to.include('~22nd');

    const rows = Array.from(el.shadowRoot!.querySelectorAll('tbody tr'));
    expect(rows).to.have.lengthOf(4);
    expect(rows[0]!.classList.contains('current-occurrence')).to.equal(true);
    expect(rows[1]!.classList.contains('current-occurrence')).to.equal(false);
    expect(rows[1]!.querySelector('.other-account-tag')!.textContent).to.include(
      'credit-card → expenses'
    );
    expect(rows[0]!.querySelectorAll('td')[2]!.textContent!.trim()).to.equal('E →');
    expect(rows[1]!.querySelectorAll('td')[3]!.classList.contains('projected')).to.equal(true);
    expect(rows[2]!.querySelectorAll('td')[2]!.textContent!.trim()).to.equal('I ←');
    expect(rows[3]!.querySelectorAll('td')[2]!.textContent!.trim()).to.equal('T →');

    // Close button dispatches close event
    const closeBtn = el.shadowRoot!.querySelector<HTMLButtonElement>('.close-btn')!;
    setTimeout(() => closeBtn.click());
    const closeEv = await oneEvent(el, 'close');
    expect(closeEv.type).to.equal('close');

    // Clicking amount copies to clipboard and dispatches copied event
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
      const amountCell = rows[0]!.querySelectorAll('td')[3]!;
      setTimeout(() => amountCell.click());
      const copiedEv = (await oneEvent(el, 'copied')) as CustomEvent<{text: string}>;
      expect(copiedEv.detail.text).to.equal('-45.00');
      expect(copied).to.deep.equal(['-45.00']);

      el.copyToClipBoard(null);
      el.copyToClipBoard(undefined);
      expect(copied).to.deep.equal(['-45.00']);
    } finally {
      if (origClipboardDesc) {
        Object.defineProperty(navigator, 'clipboard', origClipboardDesc);
      }
    }
  });

  it('renders variable amount range and transfer-in code when isFixedAmount is false and typicalDayOfMonth is null', async () => {
    const group: GqlRecurringGroup = {
      id: 'grp-var',
      label: 'Electric & gas utility',
      cadence: 'BIWEEKLY',
      occurrenceCount: 2,
      averageAmount: 12000,
      minAmount: 10500,
      maxAmount: 13500,
      totalAmount: 24000,
      isFixedAmount: false,
      typicalDayOfMonth: null,
      transactions: [
        {
          month: 'Nov2026',
          fromAccountName: 'checking',
          fromAccountPath: 'alex/internal/checking',
          toAccountName: 'credit-card',
          toAccountPath: 'alex/internal/credit-card',
          counterpartyAccountName: 'checking',
          isIncome: false,
          isExpense: false,
          isCurrentAccount: true,
          description: 'Card transfer in',
          balance: {amount: 13500, date: '2026-11-10', desc: '', type: 'CONFIRMED'},
        },
      ],
    };

    const el = await fixture<RecurringTooltip>(
      html`<recurring-tooltip .group=${group}></recurring-tooltip>`
    );

    const summaryText = el.shadowRoot!.querySelector('.summary-bar')!.textContent!;
    expect(summaryText).to.include('Avg:');
    expect(summaryText).to.include('120.00');
    expect(summaryText).to.include('105.00');
    expect(summaryText).to.include('135.00');
    expect(summaryText).to.not.include('Typical:');
    expect(el.shadowRoot!.querySelectorAll('tbody tr td')[2]!.textContent!.trim()).to.equal('T ←');
  });
});
