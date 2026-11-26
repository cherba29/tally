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
      tr.summary-row td {
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
          <tr class="summary-row">
            <td colspan="5" align="right"><b>Income</b></td>
            <td align="right">${currency(this.stmt?.income)}</td>
            <td colspan="2"></td>
          </tr>
          <tr class="summary-row">
            <td colspan="5" align="right"><b>Expenses</b></td>
            <td align="right">${currency(this.stmt?.totalPayments)}</td>
            <td colspan="2"></td>
          </tr>
          <tr class="summary-row">
            <td colspan="5" align="right"><b>Transfers</b></td>
            <td align="right">${currency(this.stmt?.totalTransfers)}</td>
            <td colspan="2"></td>
          </tr>
          <tr class="summary-row">
            <td colspan="5" align="right"><b>Inflows</b></td>
            <td align="right">${currency(this.stmt?.inFlows)}</td>
            <td colspan="2"></td>
          </tr>
          <tr class="summary-row">
            <td colspan="5" align="right"><b>Outflows</b></td>
            <td align="right">${currency(this.stmt?.outFlows)}</td>
            <td colspan="2"></td>
          </tr>
          <tr class="summary-row">
            <td colspan="5" align="right"><b>Total</b></td>
            <td align="right">${currency(this.stmt?.addSub)}</td>
            <td colspan="2"></td>
          </tr>
          <tr class="summary-row">
            <td colspan="5" align="right"><b>Unaccounted</b></td>
            <td align="right">${currency(this.stmt?.unaccounted)}</td>
            <td colspan="2"></td>
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
