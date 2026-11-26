import {LitElement, css, html, nothing, type TemplateResult} from 'lit';
import {classMap, type ClassInfo} from 'lit/directives/class-map.js';
import {styleMap, type StyleInfo} from 'lit/directives/style-map.js';
import {customElement, property} from 'lit/decorators.js';

import {dateFormat, currency, isProjected, formatCadence} from '../format';
import {type GqlBalance, type GqlStatement, type GqlTransaction} from '../gql_types';
import {tooltipSharedStyles} from './tooltip-styles';

export interface RecurringClickEventData {
  groupId: string;
  accountPath: string;
  selectedDate: string;
  selectedAmount: number;
  selectedDescription: string;
  mouseEvent: MouseEvent;
  anchorRect: DOMRect;
}

@customElement('balance-tooltip')
export class BalanceTooltip extends LitElement {
  static override styles = [
    tooltipSharedStyles,
    css`
      table {
        table-layout: auto;
        width: auto;
        min-width: 500px;
      }
      .summary-box {
        margin-bottom: 6px;
        background-color: #f6f8fa;
        border: 1px solid #d0d7de;
        border-radius: 4px;
        overflow: hidden;
        font-variant-numeric: tabular-nums;
      }
      .breakdown-grid {
        display: grid;
        grid-template-columns: 1fr auto;
        align-items: stretch;
      }
      .equations {
        display: flex;
        flex-direction: column;
      }
      .equation-row {
        display: flex;
        align-items: center;
        gap: 4px;
        padding: 3px 6px;
      }
      .equation-row + .equation-row {
        border-top: 1px dashed #d0d7de;
      }
      .metric {
        display: inline-flex;
        align-items: baseline;
        gap: 4px;
        padding: 1px 5px;
        border-radius: 3px;
        background-color: #fff;
        border: 1px solid #d8dee4;
        white-space: nowrap;
      }
      .metric-label {
        font-size: 9px;
        color: #57606a;
        font-weight: 600;
      }
      .metric-value {
        font-weight: 700;
        color: #1f2328;
      }
      .metric.income {
        border-color: #82c982;
        background-color: #eefbee;
      }
      .metric.expenses {
        border-color: #d99b9b;
        background-color: #fdf2f2;
      }
      .op {
        color: #6e7781;
        font-weight: 700;
        font-size: 10px;
      }
      .total-cell {
        display: flex;
        align-items: center;
        gap: 6px;
        padding: 4px 8px;
        border-left: 2px solid #8c959f;
        background-color: #eaeef2;
        white-space: nowrap;
      }
      .total-stack {
        display: flex;
        flex-direction: column;
        align-items: flex-end;
      }
      .total-stack .metric-label {
        font-size: 9px;
        text-transform: uppercase;
        letter-spacing: 0.03em;
      }
      .total-stack .metric-value {
        font-size: 12px;
        line-height: 14px;
      }
      .reconciliation-bar {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 8px;
        padding: 4px 6px;
        border-top: 1px solid #d0d7de;
        background-color: #f0f3f6;
        font-size: 10px;
      }
      .reconciliation-bar.unaccounted-alert {
        background-color: #ffebe9;
        border-top: 1px solid #cf222e;
        color: #a40e26;
      }
      .reconciliation-formula {
        display: inline-flex;
        align-items: center;
        flex-wrap: wrap;
        gap: 4px;
        color: #57606a;
      }
      .reconciliation-bar.unaccounted-alert .reconciliation-formula {
        color: #82071e;
      }
      .reconciliation-formula b {
        color: #1f2328;
      }
      .reconciliation-bar.unaccounted-alert .reconciliation-formula b {
        color: #82071e;
      }
      .unaccounted-pill {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        padding: 1px 7px;
        border-radius: 999px;
        font-weight: 700;
        white-space: nowrap;
      }
      .unaccounted-pill.balanced {
        color: #1a7f37;
        background-color: #dafbe1;
        border: 1px solid #4ac26b;
      }
      .unaccounted-pill.nonzero {
        color: #fff;
        background-color: #cf222e;
        border: 1px solid #a40e26;
        cursor: pointer;
        box-shadow: 0 1px 3px rgba(207, 34, 46, 0.35);
      }
      .unaccounted-pill.nonzero:hover {
        background-color: #a40e26;
      }
      .recurring-badge {
        display: inline-flex;
        align-items: center;
        gap: 2px;
        padding: 0 5px;
        font-size: 9px;
        line-height: 13px;
        font-weight: 700;
        color: #0969da;
        background-color: #ddf4ff;
        border: 1px solid #54aeff;
        border-radius: 999px;
        cursor: pointer;
        vertical-align: middle;
        white-space: nowrap;
      }
      .recurring-badge:hover {
        background-color: #b6e3ff;
        border-color: #0969da;
      }
      td.nowrap {
        white-space: nowrap;
      }
      td.desc-col {
        min-width: 140px;
        max-width: 240px;
      }
      tr.balance-row th {
        background-color: #f6f8fa;
      }
      td.copyable {
        cursor: pointer;
        white-space: nowrap;
      }
    `,
  ];

  private __month: string = '';
  private __stmt: GqlStatement | undefined = undefined;

  @property({attribute: false})
  accountName: string = '';

  @property()
  set month(value: string) {
    const oldValue = this.__month;
    this.__month = value;
    this.requestUpdate('month', oldValue);
  }
  get month() {
    return this.__month;
  }

  @property()
  set stmt(value: GqlStatement | undefined) {
    const oldValue = this.__stmt;
    this.__stmt = value;
    this.requestUpdate('stmt', oldValue);
  }
  get stmt() {
    return this.__stmt;
  }

  onCloseButton() {
    this.dispatchEvent(new CustomEvent('close'));
  }

  private onRecurringClick(e: MouseEvent, t: GqlTransaction) {
    if (!t.recurringGroupId) return;
    e.stopPropagation();
    const target = e.currentTarget as HTMLElement;
    const anchorRect = target.getBoundingClientRect();
    this.dispatchEvent(
      new CustomEvent<RecurringClickEventData>('recurring-click', {
        detail: {
          groupId: t.recurringGroupId,
          accountPath: this.accountName,
          selectedDate: dateFormat(t.balance?.date),
          selectedAmount: t.balance?.amount ?? 0,
          selectedDescription: t.description ?? '',
          mouseEvent: e,
          anchorRect,
        },
        bubbles: true,
        composed: true,
      })
    );
  }

  private recurringBadge(t: GqlTransaction | undefined | null) {
    if (!t?.recurringGroupId) {
      return nothing;
    }
    const cadenceLabel = formatCadence(t.recurringCadence);
    const countPart = t.recurringCount ? ` · ${t.recurringCount} occurrences` : '';
    const title = `Recurring (${cadenceLabel}${countPart}) — click to view all`;
    return html`<button
      type="button"
      class="recurring-badge"
      title="${title}"
      aria-label="${title}"
      @click=${(e: MouseEvent) => this.onRecurringClick(e, t)}
    >&#x21BB;${t.recurringCount ? ` ${t.recurringCount}` : ''}</button>`;
  }

  copyToClipBoard(amount: number | null | undefined) {
    if (amount != null || amount != undefined) {
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

  override render() {
    const transactionColor = (t: GqlTransaction | undefined | null): StyleInfo => ({
      backgroundColor: t?.isExpense ? '#caa' : t?.isIncome ? '#aca' : null,
    });
    const transactionCode = (t: GqlTransaction | undefined | null): TemplateResult => {
      if (t?.isExpense) return html`E &#x2192;`;
      if (t?.isIncome) return html`I &#x2190;`;
      if ((t?.balance?.amount ?? 0) > 0) return html`T &#x2190;`;
      return html`T &#x2192;`;
    };
    const projectedClass = (b: GqlBalance | null | undefined): ClassInfo => {
      const projected = isProjected(b);
      return {
        projected,
        confirmed: !projected,
      };
    };
    const unaccountedValue = Number(this.stmt?.unaccounted ?? 0);
    const hasUnaccounted = !Number.isNaN(unaccountedValue) && unaccountedValue !== 0;
    return html`
      <div class="header">
        <button
          type="button"
          class="close-btn"
          title="Close statement"
          aria-label="Close"
          @click="${this.onCloseButton}"
        >
          &times;
        </button>
        <span class="title">${this.accountName}</span>
        <span class="meta-pill">${this.month}</span>
      </div>
      <div class="summary-box">
        <div class="breakdown-grid">
          <div class="equations">
            <div class="equation-row" data-equation="category">
              <span class="metric income">
                <span class="metric-label">Income</span>
                <span class="metric-value">${currency(this.stmt?.income)}</span>
              </span>
              <span class="op">+</span>
              <span class="metric expenses">
                <span class="metric-label">Expenses</span>
                <span class="metric-value">${currency(this.stmt?.totalPayments)}</span>
              </span>
              <span class="op">+</span>
              <span class="metric">
                <span class="metric-label">Transfers</span>
                <span class="metric-value">${currency(this.stmt?.totalTransfers)}</span>
              </span>
            </div>
            <div class="equation-row" data-equation="flow">
              <span class="metric">
                <span class="metric-label">Inflows</span>
                <span class="metric-value">${currency(this.stmt?.inFlows)}</span>
              </span>
              <span class="op">+</span>
              <span class="metric">
                <span class="metric-label">Outflows</span>
                <span class="metric-value">${currency(this.stmt?.outFlows)}</span>
              </span>
            </div>
          </div>
          <div class="total-cell">
            <span class="op">=</span>
            <div class="total-stack">
              <span class="metric-label">Total</span>
              <span class="metric-value">${currency(this.stmt?.addSub)}</span>
            </div>
          </div>
        </div>
        <div
          class=${classMap({
            'reconciliation-bar': true,
            'unaccounted-alert': hasUnaccounted,
          })}
        >
          <span class="reconciliation-formula">
            <span>End &minus; Start:</span>
            <b>${currency(this.stmt?.change)}</b>
            <span class="op">&minus;</span>
            <span>Total:</span>
            <b>${currency(this.stmt?.addSub)}</b>
            <span class="op">=</span>
          </span>
          ${hasUnaccounted
            ? html`<span
                class="unaccounted-pill nonzero"
                role="alert"
                title="Balance change differs from total transactions — click to copy unaccounted amount"
                @click="${() => this.copyToClipBoard(this.stmt?.unaccounted)}"
              >
                &#x26A0; Unaccounted: ${currency(this.stmt?.unaccounted)}
              </span>`
            : html`<span class="unaccounted-pill balanced">
                &#x2713; Unaccounted: ${currency(this.stmt?.unaccounted)}
              </span>`}
        </div>
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
            <th colspan="6" align="left">Ending Balance</th>
            <th>${dateFormat(this.stmt?.endBalance?.date)}</th>
            <th align="right">${currency(this.stmt?.endBalance?.amount)}</th>
          </tr>
          ${this.stmt?.transactions?.map(
            (t: GqlTransaction, index: number) =>
              html`<tr class="highlight">
                <td align="middle" class="nowrap">${index + 1}</td>
                <td class="nowrap">${t?.toAccountName}</td>
                <td class="desc-col">${t?.description}</td>
                <td align="middle" class="nowrap">${this.recurringBadge(t)}</td>
                <td align="middle" class="nowrap" style=${styleMap(transactionColor(t))}>
                  ${transactionCode(t)}
                </td>
                <td class=${classMap({...projectedClass(t?.balance), nowrap: true})} style="min-width:45px">
                  ${currency(t?.balance?.amount)}
                </td>
                <td align="middle" class="nowrap" style="min-width:60px">${dateFormat(t?.balance?.date)}</td>
                <td
                  class="copyable"
                  align="right"
                  style="min-width:50px"
                  title="Click to copy running balance"
                  @click="${() => this.copyToClipBoard(t?.balanceFromStart)}"
                >
                  ${currency(t?.balanceFromStart)}
                </td>
              </tr>`
          )}
          <tr class="balance-row">
            <th colspan="6" align="left">Starting Balance</th>
            <th>${dateFormat(this.stmt?.startBalance?.date)}</th>
            <th align="right">${currency(this.stmt?.startBalance?.amount)}</th>
          </tr>
        </tbody>
      </table>
   `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'balance-tooltip': BalanceTooltip;
  }
}
