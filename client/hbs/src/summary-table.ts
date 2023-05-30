import {LitElement, css, html, nothing} from 'lit';
import {styleMap, StyleInfo} from 'lit/directives/style-map.js';
import {customElement, property} from 'lit/decorators.js';
import {dateFormat, currency, isProjected} from './format';
import {Cell} from './cell';
import {Row, Type as RowType} from './row';
import {Account} from '@tally/lib/core/account';
import {classMap, ClassInfo} from 'lit/directives/class-map.js';

@customElement('summary-table')
export class SummaryTable extends LitElement {
  static styles = css`
    body {
      font-size: 80%;
    }
    .account_type {
      font-weight: bold;
    }
    .change {
      font-family: 'Courier New', 'Lucida Console';
      font-size: 75%;
    }
    .balance {
      font-family: 'Courier New', 'Lucida Console';
    }
    .unaccounted {
      font-family: 'Courier New', 'Lucida Console';
      font-size: 66%;
      color: #f00;
    }
    .accounted {
      font-family: 'Courier New', 'Lucida Console';
      font-size: 66%;
      color: #0a0;
    }
    .add_sub {
      font-family: 'Courier New', 'Lucida Console';
      font-size: 66%;
    }
    .closed {
      background-color: #ccc;
    }
    .projected {
      background-color: #ddf;
    }
    table {
      border: 5px solid #c3c3c3;
      border-collapse: collapse;
    }
    table td {
      border-left: 1px solid #c3c3c3;
      border-top: 1px solid #c3c3c3;
      padding: 1px;
      vertical-align: middle;
    }
    table td.balance,
    td.unaccounted,
    td.add_sub,
    td.accounted,
    td.change {
      text-align: right;
    }
    table th {
      border-left-width: 1px;
      font-family: 'Times New Roman', Times, serif;
      font-size: 75%;
      padding: 2px;
      vertical-align: middle;
    }
  `;

  @property({attribute: false})
  months: string[] = [];

  @property({attribute: false})
  rows: Row[] = [];

  @property({attribute: false})
  onCellClick: (e: MouseEvent, id: string) => void = (e: MouseEvent, id: string) => {};

  render() {
    const projectedClass = (c: Cell): ClassInfo => {
      return {projected: c.isProjected};
    };
    const unaccountedClass = (c: Cell): ClassInfo => {
      return {accounted: c.balanced, unaccounted: !c.balanced};
    };
    const coveredStyle = (c: Cell): StyleInfo => ({
      color: c.isCovered ? null : c.isProjectedCovered ? '#fa0' : '#f00',
      fontWeight: c.isCovered ? null : 700,
    });

    return html`
      <table>
        <thead>
          <tr>
            <th>Account</th>
            ${this.months.map((m) => html`<th colspan="4">${m}</th>`)}
          </tr>
          <tr>
            <th></th>
            ${this.months.map(
              (m) =>
                html`<th>+/-</th>
                  <th>$Bal</th>
                  <th>Chg%</th>
                  <th>?</th>`
            )}
          </tr>
        </thead>
        <tbody>
          ${this.rows.map((r) => {
            if (r.isSpace) {
              return html`<tr>
                <td class="account_type">${r.title}</td>
                ${this.months.map(
                  (m) => html`<td colspan="4" style="border-right:2px double #a00"></td>`
                )}
              </tr>`;
            } else if (r.isTotal) {
              return html`<tr>
                <td><b>${r.title}/Total</b></td>
                ${r.cells.map((c) => {
                  if (c.isClosed) {
                    return html`<td
                      colspan="4"
                      class="closed"
                      style="border-right:2px double #a00"
                    ></td>`;
                  }
                  return html`
                    <td class="add_sub">${currency(c.addSub)}</td>
                    <td
                      id="${c.id}"
                      @click="${(e: MouseEvent) => this.onCellClick(e, c.id)}"
                      style="font-weight:700;font-size:75%;"
                      class="balance ${classMap(projectedClass(c))}"
                    >
                      ${currency(c.balance)}
                    </td>
                    <td class="change">${c.percentChange}</td>
                    <td
                      class="${classMap(unaccountedClass(c))}"
                      style="border-right:2px double #a00"
                    >
                      ${currency(c.unaccounted)}
                    </td>
                  `;
                })}
              </tr>`;
            } else if (r.isNormal) {
              const account = r.title as Account;
              return html`<tr>
                <td
                  id="${account.name}"
                  @click="${(e: MouseEvent) => this.onCellClick(e, account.name)}"
                >
                  ${account.url
                    ? html`<a href="${account.url}" target="_blank">${account.name}</a>`
                    : account.name}
                </td>
                ${r.cells.map((c) => {
                  if (c.isClosed) {
                    return html`<td
                      colspan="4"
                      class="closed"
                      style="border-right:2px double #a00"
                    ></td>`;
                  }
                  return html`<td class="add_sub">${currency(c.addSub)}</td>
                    <td
                      id="${c.id}"
                      @click="${(e: MouseEvent) => this.onCellClick(e, c.id)}"
                      class="balance ${classMap(projectedClass(c))}"
                      style=${styleMap(coveredStyle(c))}
                    >
                      ${c.hasProjectedTransfer ? html`**` : nothing}${currency(c.balance)}
                    </td>
                    <td class="change">${c.percentChange}</td>
                    <td
                      class="${classMap(unaccountedClass(c))}"
                      style="border-right:2px double #a00"
                    >
                      ${currency(c.unaccounted)}
                    </td>`;
                })}
              </tr>`;
            } else {
              return nothing;
            }
          })}
        </tbody>
      </table>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'summary-table': SummaryTable;
  }
}
