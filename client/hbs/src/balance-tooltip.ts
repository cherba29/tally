import {LitElement, css, html, nothing} from 'lit';
import {styleMap, StyleInfo} from 'lit/directives/style-map.js';
import {customElement} from 'lit/decorators.js';
import {dateFormat, currency, isProjected} from './format';
import {Statement, Transaction} from './base';

@customElement('balance-tooltip')
export class BalanceTooltip extends LitElement {
  static styles = css`
    td {
      border-left: 1px solid #c3c3c3;
      border-top: 1px solid #c3c3c3;
      padding: 1px;
      vertical-align: middle;
    }
    .highlight:hover {
      background-color: #40c340;
    }
  `;

  constructor(
    readonly accountName: string,
    readonly month: string,
    readonly stmt: Statement,
    readonly onCloseButton: () => void
  ) {
    super();
  }

  render() {
    const transactionColor = (t: Transaction): StyleInfo => ({
      backgroundColor: t.isExpense ? '#caa' : t.isIncome ? '#aca' : null,
    });
    const transactionCode = (t: Transaction): string =>
      t.isExpense ? 'E' : t.isIncome ? 'I' : 'T';
    return html`
      <span @click="${this.onCloseButton}">XXX</span>
      <span style="float:right;">${this.accountName} - ${this.month}</span>
      <table style="table-layout: auto; width: 500px">
        <tr>
          <th align="left">Ending Balance</th>
          <th>Type</th>
          <th></th>
          <th>${dateFormat(this.stmt.endBalance?.date)}</th>
          <th align="right">${currency(this.stmt.endBalance?.amount)}</th>
          <th></th>
        </tr>
        ${this.stmt.transactions?.map(
          (t, index) =>
            html`<tr class="highlight">
              <td style="min-width:175px">
                ${index + 1} ${isProjected(t.balance) ? html`**` : nothing}${t.toAccountName}
                <span style="font-size:80%">${t.description}</span>
              </td>
              <td align="middle" style=${styleMap(transactionColor(t))}>${transactionCode(t)}</td>
              <td align="right" style="min-width:40px">${currency(t.balance.amount)}</td>
              <td align="middle" style="min-width:60px">${dateFormat(t.balance.date)}</td>
              <td align="right" style="min-width:50px">${currency(t.balanceFromEnd)}</td>
              <td align="right" style="min-width:50px">${currency(t.balanceFromStart)}</td>
            </tr>`
        )}
        <tr>
          <th align="left">Starting Balance</td>
          <th></th>
          <th></th>
          <th>${dateFormat(this.stmt.startBalance?.date)}</th>
          <th></th>
          <th align="right" style="width:50px">${currency(this.stmt.startBalance?.amount)}</th>
        </tr>
        <tr>
          <td align="right"><b>Income</b></td>
          <td></td>
          <td align="right">${currency(this.stmt.income)}</td>
          <td></td>
          <td></td>
          <td></td>
        </tr>
        <tr>
          <td align="right"><b>Expenses</b></td>
          <td></td>
          <td align="right">${currency(this.stmt.totalPayments)}</td>
          <td></td>
          <td></td>
          <td></td>
        </tr>
        <tr>
          <td align="right"><b>Transfers</b></td>
          <td></td>
          <td align="right">${currency(this.stmt.totalTransfers)}</td>
          <td></td>
          <td></td>
          <td></td>
        </tr>
        <tr>
          <td align="right"><b>Inflows</b></td>
          <td></td>
          <td align="right">${currency(this.stmt.inFlows)}</td>
          <td></td>
          <td></td>
          <td></td>
        </tr>
        <tr>
          <td align="right"><b>Outflows</b></td>
          <td></td>
          <td align="right">${currency(this.stmt.outFlows)}</td>
          <td></td>
          <td></td>
          <td></td>
        </tr>
        <tr>
          <td align="right"><b>Total</b></td>
          <td></td>
          <td align="right">${currency(this.stmt.addSub)}</td>
          <td></td>
          <td></td>
          <td></td>
        </tr>
        <tr>
          <td align="right"><b>Unaccounted</b></td>
          <td></td>
          <td align="right">${currency(this.stmt.unaccounted)}</td>
          <td></td>
          <td></td>
          <td></td>
        </tr>
      </table>
   `;
  }
}
