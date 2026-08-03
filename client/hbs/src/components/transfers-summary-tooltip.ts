import { LitElement, css, html, nothing } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import { currency } from '../format';
import { type GqlMonthTransferSummary } from '../gql_types';
import { Month } from '@tally/lib/core/month';

import './period-buttons';

export interface TransfersSummaryMonthRangeChange {
  accountPath: string;
  startMonth: Month | undefined;
  endMonth: Month;
}


@customElement('transfers-summary-tooltip')
export class TransfersSummaryTooltip extends LitElement {
  static override styles = css`
    td {
      border-left: 1px solid #c3c3c3;
      border-top: 1px solid #c3c3c3;
      padding: 1px;
      vertical-align: middle;
    }
  `;

  @property({attribute: false})
  accountPath: string = '';

  __months: string[] = [];
  @property()
  set months(value: string[]) {
    const oldValue = this.__months;
    this.__months = value;
    this.requestUpdate('months', oldValue);
  }
  get months() {
    return this.__months;
  }


  private __monthlyData: GqlMonthTransferSummary[] = [];

  @property()
  set monthlyData(value: GqlMonthTransferSummary[]) {
    const oldValue = this.__monthlyData;
    this.__monthlyData = value;
    this.requestUpdate('monthlyData', oldValue);
  }
  get monthlyData() {
    return this.__monthlyData;
  }
  
  onCloseButton() {
    this.dispatchEvent(new CustomEvent('close'));
  }

  switchView(e: CustomEvent) {
    console.log(`### switchView changed month range ${e.detail.startMonth} - ${e.detail.endMonth}`);
    this.dispatchEvent(
      new CustomEvent<TransfersSummaryMonthRangeChange>('month-range-change', {
        detail: {
          accountPath: this.accountPath,
          startMonth: e.detail.startMonth,
          endMonth: e.detail.endMonth,
        },
      })
    );
  }

  override render() {
    if (!this.months) {
      return nothing;
    }
    return html`
      <span @click="${this.onCloseButton}">XXX</span>
      <period-buttons
        .currentMonth=${Month.fromString(this.months[0]!)}
        @button-click=${this.switchView}
      ></period-buttons>
      <span style="float:right;">${this.accountPath}</span>
      <table>
        <thead>
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
          <th style="min-width:60px">Unaccounted</th>
        </thead>
        <tbody>
        ${this.monthlyData.map(
          (data, index) =>
            html`<tr class="highlight">
              <td align="middle">${index + 1}</td>
              <td>${this.months[index]}</td>
              <td align="right">${currency(data.internalTransfers)}</td>
              <td align="right">${currency(data.externalTransfers)}</td>
              <td align="right">${currency(data.totalMonthTransfers)}</td>
              <td align="right">${currency(data.totalInternalTransfers)}</td>
              <td align="right">${data.totalInternalTransfersPrct}</td>
              <td align="right">${data.totalInternalTransfersAnnualPrct}</td>
              <td align="right">${currency(data.totalExternalTransfers)}</td>
              <td align="right">${data.totalExternalTransfersPrct}</td>
              <td align="right">${data.totalExternalTransfersAnnualPrct}</td>
              <td align="right">${currency(data.totalTransfers)}</td>
              <td align="right">${currency(data.unaccounted)}</td>
            </tr>`
        )}
        </tbody>
      </table>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'transfers-summary-tooltip': TransfersSummaryTooltip;
  }
}
