import './expand-button';

import {LitElement, css, html, nothing} from 'lit';
import {styleMap, StyleInfo} from 'lit/directives/style-map.js';
import {customElement, property} from 'lit/decorators.js';
import {currency} from '../format';
import {GqlAccount, GqlTableRow, GqlTableCell} from 'src/gql_types';
import {classMap, ClassInfo} from 'lit/directives/class-map.js';
import {Month} from '@tally/lib/core/month';

export type CellClickEventData = {
  mouseEvent: MouseEvent;
  accountName?: string;
  account?: GqlAccount | null;
  month?: string;
  isSummary?: boolean;
};

// Represents tree structure of table rows.
type RowInfo = {
  index: number;
  indent: number;
  expanded: boolean;
  childrenIdxs: Array<number>;
};

@customElement('summary-table')
export class SummaryTable extends LitElement {
  static override styles = css`
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

  private __months: Month[] = [];
  private __rows: GqlTableRow[] = [];
  // All rows rendered in the table.
  private childTableRows: Array<RowInfo> = [];

  @property()
  set months(values: Month[]) {
    const oldMonths = this.__months;
    this.__months = values;
    this.requestUpdate('months', oldMonths);
  }
  get months() {
    return this.__months;
  }

  @property()
  set rows(values: GqlTableRow[]) {
    const oldRows = this.__rows;
    this.__rows = values;
    this.requestUpdate('rows', oldRows);
  }
  get rows(): GqlTableRow[] {
    return this.__rows;
  }

  onCellClick(
    mouseEvent: MouseEvent,
    accountName: string | undefined | null,
    month: string,
    isSummary: boolean
  ) {
    const options: CustomEventInit<CellClickEventData> = {
      detail: {
        mouseEvent,
        accountName: accountName ?? undefined,
        month,
        isSummary,
      },
      bubbles: true,
      composed: true,
    };
    this.dispatchEvent(new CustomEvent('cellclick', options));
  }

  onTitleCellClick(e: MouseEvent, account: GqlAccount | undefined | null) {
    const options: CustomEventInit<CellClickEventData> = {
      detail: {
        mouseEvent: e,
        account,
      },
      bubbles: true,
      composed: true,
    };
    this.dispatchEvent(new CustomEvent('cellclick', options));
  }

  // Taggle child row visibility
  private toggleChildRows(rowIdx: number, expanded: boolean) {
    const rowInfo = this.childTableRows[rowIdx];
    if (!rowInfo) {
      throw new Error(`No such row ${rowIdx} have been registered.`);
    }
    rowInfo.expanded = expanded;
    this.setChildrenRowsVisibility(rowInfo, expanded);
  }

  // Recursively show/hide child table rows.
  private setChildrenRowsVisibility(rowInfo: RowInfo, visible: boolean) {
    // Child is visible if its parent is also expanded.
    const childVisibility = visible && rowInfo.expanded;
    for (var childIdx of rowInfo.childrenIdxs) {
      const childRowInfo = this.childTableRows[childIdx];
      if (childRowInfo) {
        this.setChildrenRowsVisibility(childRowInfo, childVisibility);
      }
      const childElement = this.shadowRoot?.getElementById(`row-${childIdx}`);
      if (childElement) {
        childElement.style.display = childVisibility ? 'table-row' : 'none';
      }
    }
  }

  // Register row in the tree by row number and its indent.
  private addRow(index: number, indent: number) {
    this.childTableRows[index] = {
      index,
      indent,
      expanded: true,
      childrenIdxs: []
    };
    if (indent <= 0) { return; }
    // Look for parent based on indent, ie first row with smaller indent.
    const findParent = (index: number, indent: number): RowInfo | undefined => {
      for (let i = index - 1; i >= 0; i--) {
        const rowInfo = this.childTableRows[i];
        if ((rowInfo?.indent ?? 0) < indent) {
          return rowInfo;
        }
      }
      return undefined;
    }
    const parentInfo = findParent(index, indent);
    if (!parentInfo) {  // This should never happen.
      throw new Error(`Failed to find parent for row ${index} with indent ${indent}`);
    }
    parentInfo.childrenIdxs.push(index);
  }

  override render() {
    const projectedClass = (c: GqlTableCell): ClassInfo => {
      return {projected: c.isProjected ?? false};
    };
    const unaccountedClass = (c: GqlTableCell): ClassInfo => {
      return {accounted: c.balanced ?? false, unaccounted: !c.balanced};
    };
    const coveredStyle = (c: GqlTableCell): StyleInfo => ({
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
              () =>
                html`<th>+/-</th>
                  <th>$Bal</th>
                  <th>Chg%</th>
                  <th>?</th>`
            )}
          </tr>
        </thead>
        <tbody>
          ${this.rows.map((r, rowIdx) => {
            this.addRow(rowIdx, r.indent ?? 0);
            if (r.isSpace) {
              return html`<tr id="row-${rowIdx}">
                <td class="account_type">${r.title}</td>
                ${this.months.map(
                  () => html`<td colspan="4" style="border-right:2px double #a00"></td>`
                )}
              </tr>`;
            } else if (r.isTotal) {
              return html`<tr id="row-${rowIdx}" bgcolor="#ffd">
                <td>
                <expand-button @toggle="${(e: CustomEvent)=>this.toggleChildRows(rowIdx, e.detail.expanded)}"></expand-button> 
                ${Array(r.indent ?? 0).fill(0).map(_=>html`&nbsp;`)}<b>${r.title}</b></td>
                ${(r.cells ?? []).map((c) => {
                  if (!c || c.isClosed) {
                    return html`<td
                      colspan="4"
                      class="closed"
                      style="border-right:2px double #a00"
                    ></td>`;
                  }
                  return html`
                    <td class="add_sub">${currency(c.addSub)}</td>
                    <td
                      @click="${(e: MouseEvent) => this.onCellClick(e, r.account?.name ?? r.title, c.month, true)}"
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
              const account = r.account!;
              return html`<tr id="row-${rowIdx}">
                <td
                  id="${account.name}"
                  @click="${(e: MouseEvent) => this.onTitleCellClick(e, r.account)}"
                >
                  ${Array(r.indent ?? 0).fill(0).map(_=>html`&nbsp;`)}
                  ${account.url
                    ? html`<a href="${account.url}" target="_blank">${account.name}</a>`
                    : account.name}
                </td>
                ${(r.cells ?? []).map((c) => {
                  if (!c || c.isClosed) {
                    return html`<td
                      colspan="4"
                      class="closed"
                      style="border-right:2px double #a00"
                    ></td>`;
                  }
                  return html`<td class="add_sub">${currency(c.addSub)}</td>
                    <td
                      @click="${(e: MouseEvent) => this.onCellClick(e, r.title, c.month, false)}"
                      class="balance ${classMap(projectedClass(c))}"
                      style=${styleMap(coveredStyle(c))}
                    >
                      ${c.hasProjectedTransfer ? html`&#9730;` : nothing}${currency(c.balance)}
                    </td>
                    <td class="change">${c.percentChange ?? html`&#8734;`}</td>
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
