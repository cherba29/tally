import { LitElement, css, html, nothing } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import { currency } from '../format';
import { type GqlMonthTransferSummary } from '../gql_types';

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

  @property({attribute: false})
  months: string[] = [];

  @property({attribute: false})
  monthlyData: GqlMonthTransferSummary[] = [];

  onCloseButton() {
    this.dispatchEvent(new CustomEvent('close'));
  }

  override render() {
    if (!this.months) {
      return nothing;
    }
    return html`
      <span @click="${this.onCloseButton}">XXX</span>
      <span style="float:right;">${this.accountPath}</span>
      <table>
        <thead>
          <th style="min-width:20px"></th>
          <th style="min-width:50px">Month</th>
          <th style="min-width:50px">Internal</th>
          <th style="min-width:50px">External</th>
          <th style="min-width:50px">Total</th>
          <th style="min-width:60px">Tot Internal</th>
          <th style="min-width:60px">Tot External</th>
          <th style="min-width:60px">Total</th>
        </thead>
        <tbody>
        ${this.monthlyData.map(
          (data, index) =>
            html`<tr class="highlight">
              <td align="middle">${index + 1}</td>
              <td>${this.months[index]}</td>
              <td align="right">${currency(data.internalTransfers)}</td>
              <td align="right">${currency(data.externalTransfers)}</td>
              <td align="right">${currency(parseInt(data.externalTransfers) + parseInt(data.internalTransfers))}</td>
              <td align="right">${currency(data.totalInternalTransfers)}</td>
              <td align="right">${currency(data.totalExternalTransfers)}</td>
              <td align="right">${currency(parseInt(data.totalExternalTransfers) + parseInt(data.totalInternalTransfers))}</td>
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
