import {LitElement, css, html, nothing, type TemplateResult} from 'lit';
import {classMap, type ClassInfo} from 'lit/directives/class-map.js';
import {styleMap, type StyleInfo} from 'lit/directives/style-map.js';
import {customElement, property} from 'lit/decorators.js';

import {
  currency,
  dateFormat,
  formatCadence,
  formatOrdinalDay,
  isProjected,
} from '../format';
import {
  type GqlBalance,
  type GqlRecurringGroup,
  type GqlRecurringTransaction,
} from '../gql_types';
import {type RecurringHighlightKey} from '../utils';
import {tooltipSharedStyles} from './tooltip-styles';

@customElement('recurring-tooltip')
export class RecurringTooltip extends LitElement {
  static override styles = [
    tooltipSharedStyles,
    css`
      .summary-bar {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
        padding: 3px 5px;
        margin-bottom: 4px;
        background-color: #f6f8fa;
        border: 1px solid #d0d7de;
        border-radius: 3px;
        color: #24292f;
        font-size: 10px;
      }
      .summary-item b {
        color: #1f2328;
      }
      table {
        table-layout: auto;
        width: auto;
        min-width: 390px;
        max-width: 470px;
      }
      th {
        text-align: left;
      }
      tr.current-occurrence {
        background-color: #fff3b0;
        font-weight: 700;
      }
      tr.current-occurrence td:first-child {
        border-left: 3px solid #0969da;
      }
      .other-account-tag {
        display: inline-block;
        margin-left: 4px;
        padding: 0 4px;
        border-radius: 3px;
        background-color: #eaeef2;
        border: 1px solid #c5ccd3;
        color: #57606a;
        font-size: 85%;
        font-weight: 400;
      }
      .projected,
      .confirmed {
        cursor: pointer;
      }
    `,
  ];

  @property({attribute: false})
  group: GqlRecurringGroup | undefined = undefined;

  @property({attribute: false})
  highlightKey: RecurringHighlightKey | undefined = undefined;

  onCloseButton() {
    this.dispatchEvent(new CustomEvent('close'));
  }

  copyToClipBoard(amount: number | null | undefined) {
    if (amount != null) {
      const text = (amount / 100.0).toFixed(2);
      void navigator.clipboard?.writeText(text)?.catch(() => {});
      this.dispatchEvent(
        new CustomEvent('copied', {
          detail: {text},
          bubbles: true,
          composed: true,
        })
      );
    }
  }

  private isHighlighted(t: GqlRecurringTransaction): boolean {
    if (!this.highlightKey) return false;
    return (
      dateFormat(t.balance?.date) === this.highlightKey.date &&
      (t.balance?.amount ?? 0) === this.highlightKey.amount &&
      (t.description ?? '') === this.highlightKey.description
    );
  }

  override render() {
    if (!this.group) {
      return nothing;
    }

    const transactionColor = (t: GqlRecurringTransaction): StyleInfo => ({
      backgroundColor: t.isExpense ? '#caa' : t.isIncome ? '#aca' : null,
    });
    const transactionCode = (t: GqlRecurringTransaction): TemplateResult => {
      if (t.isExpense) return html`E &#x2192;`;
      if (t.isIncome) return html`I &#x2190;`;
      if ((t.balance?.amount ?? 0) > 0) return html`T &#x2190;`;
      return html`T &#x2192;`;
    };
    const projectedClass = (b: GqlBalance | null | undefined): ClassInfo => {
      const projected = isProjected(b);
      return {
        projected,
        confirmed: !projected,
      };
    };

    const cadenceLabel = formatCadence(this.group.cadence);

    return html`
      <div class="header">
        <button
          type="button"
          class="close-btn"
          title="Close recurring transactions"
          aria-label="Close"
          @click="${this.onCloseButton}"
        >
          &times;
        </button>
        <span class="title">${this.group.label}</span>
        <span class="cadence-pill meta-pill">${cadenceLabel} &middot; ${this.group.occurrenceCount}</span>
      </div>
      <div class="summary-bar">
        ${this.group.isFixedAmount
          ? html`<span class="summary-item"><b>Fixed:</b> ${currency(this.group.averageAmount)}</span>`
          : html`<span class="summary-item">
              <b>Avg:</b> ${currency(this.group.averageAmount)}
              (${currency(this.group.minAmount)} &ndash; ${currency(this.group.maxAmount)})
            </span>`}
        <span class="summary-item"><b>Total:</b> ${currency(this.group.totalAmount)}</span>
        ${this.group.typicalDayOfMonth != null
          ? html`<span class="summary-item"><b>Typical:</b> ~${formatOrdinalDay(this.group.typicalDayOfMonth)}</span>`
          : nothing}
      </div>
      <table>
        <thead>
          <tr>
            <th>#</th>
            <th>Account / Description</th>
            <th>Type</th>
            <th style="text-align:right">Amount</th>
            <th>Date</th>
            <th>Month</th>
          </tr>
        </thead>
        <tbody>
          ${this.group.transactions.map((t, index) => {
            const rowClasses: ClassInfo = {
              highlight: true,
              'current-occurrence': this.isHighlighted(t),
            };
            return html`<tr class=${classMap(rowClasses)}>
              <td>${index + 1}</td>
              <td>
                ${t.counterpartyAccountName}
                <span style="font-size:85%">${t.description}</span>
                ${!t.isCurrentAccount
                  ? html`<span class="other-account-tag">${t.fromAccountName} &#x2192; ${t.toAccountName}</span>`
                  : nothing}
              </td>
              <td align="middle" style=${styleMap(transactionColor(t))}>${transactionCode(t)}</td>
              <td
                class=${classMap(projectedClass(t.balance))}
                title="Click to copy amount"
                @click="${() => this.copyToClipBoard(t.balance?.amount)}"
              >
                ${currency(t.balance?.amount)}
              </td>
              <td align="middle">${dateFormat(t.balance?.date)}</td>
              <td align="middle">${String(t.month)}</td>
            </tr>`;
          })}
        </tbody>
      </table>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'recurring-tooltip': RecurringTooltip;
  }
}
