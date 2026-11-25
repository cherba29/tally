import './expand-button';

import {LitElement, css, html, nothing, type PropertyValues} from 'lit';
import {styleMap, type StyleInfo} from 'lit/directives/style-map.js';
import {customElement, property, state} from 'lit/decorators.js';
import {currency} from '../format';
import {type GqlAccount, type GqlTableRow, type GqlTableCell} from '../gql_types';
import {classMap, type ClassInfo} from 'lit/directives/class-map.js';
import {Month} from '../month';

export type CellClickEventData = {
  mouseEvent: MouseEvent;
  anchorRect?: DOMRect;
  rowId: string;
  account?: GqlAccount | null;
  month?: Month;
  isSummary?: boolean;
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
      border-collapse: separate;
      border-spacing: 0;
    }
    thead {
      position: sticky;
      top: 0;
      z-index: 2;
    }
    table td {
      border-left: 1px solid #c3c3c3;
      border-top: 1px solid #c3c3c3;
      padding: 1px;
      vertical-align: middle;
    }
    tbody td:first-child {
      position: sticky;
      left: 0;
      z-index: 1;
      background-color: #fff;
      border-left: none;
      border-right: 2px solid #c3c3c3;
      white-space: nowrap;
    }
    tbody tr[bgcolor='#ffd'] td:first-child {
      background-color: #ffd;
    }
    table td.balance,
    td.unaccounted,
    td.add_sub,
    td.accounted,
    td.change {
      text-align: right;
    }
    table th {
      background-color: #f6f8fa;
      border-left: 1px solid #c3c3c3;
      border-bottom: 1px solid #c3c3c3;
      font-family: 'Times New Roman', Times, serif;
      font-size: 75%;
      padding: 2px;
      vertical-align: middle;
    }
    thead th:first-child {
      position: sticky;
      left: 0;
      z-index: 3;
      border-left: none;
      border-right: 2px solid #c3c3c3;
    }
  `;

  @property({attribute: false}) months: Array<Month | string> = [];
  @property({attribute: false}) rows: GqlTableRow[] = [];

  @state() private collapsedIds: Set<string> = new Set();
  @state() private expandedIds: Set<string> = new Set();
  private seenTotalIds: Set<string> = new Set();

  override willUpdate(changedProperties: PropertyValues<this>) {
    if (changedProperties.has('rows')) {
      let updatedCollapsed: Set<string> | undefined;
      this.rows.forEach((r, rowIdx) => {
        if (!r.isTotal) return;
        const key = this.getRowKey(r, rowIdx);
        if (!this.seenTotalIds.has(key)) {
          this.seenTotalIds.add(key);
          if (r.isInactive) {
            updatedCollapsed ??= new Set(this.collapsedIds);
            updatedCollapsed.add(key);
          }
        }
      });
      if (updatedCollapsed) {
        this.collapsedIds = updatedCollapsed;
      }
    }
  }

  private getRowKey(r: GqlTableRow, rowIdx: number): string {
    return r.id ?? r.account?.name ?? r.title ?? String(rowIdx);
  }

  onCellClick(
    mouseEvent: MouseEvent,
    rowId: string,
    month: string,
    isSummary: boolean
  ) {
    const anchorRect = (mouseEvent.currentTarget as HTMLElement | null)?.getBoundingClientRect();
    const options: CustomEventInit<CellClickEventData> = {
      detail: {
        mouseEvent,
        anchorRect,
        rowId: rowId,
        month: Month.fromString(month),
        isSummary,
      },
      bubbles: true,
      composed: true,
    };
    this.dispatchEvent(new CustomEvent('cellclick', options));
  }

  onAddSubCellClick(
    mouseEvent: MouseEvent,
    rowId: string,
    month: string,
    isSummary: boolean
  ) {
    const anchorRect = (mouseEvent.currentTarget as HTMLElement | null)?.getBoundingClientRect();
    const options: CustomEventInit<CellClickEventData> = {
      detail: {
        mouseEvent,
        anchorRect,
        rowId: rowId,
        month: Month.fromString(month),
        isSummary,
      },
      bubbles: true,
      composed: true,
    };
    this.dispatchEvent(new CustomEvent('addSubCellClick', options));
  }

  onTitleCellClick(e: MouseEvent, rowId: string, account: GqlAccount | undefined | null) {
    const anchorRect = (e.currentTarget as HTMLElement | null)?.getBoundingClientRect();
    const options: CustomEventInit<CellClickEventData> = {
      detail: {
        mouseEvent: e,
        anchorRect,
        rowId: rowId,
        account,
      },
      bubbles: true,
      composed: true,
    };
    this.dispatchEvent(new CustomEvent('cellclick', options));
  }

  copyToClipBoard(amount: number | null | undefined) {
    if (amount != null || amount != undefined) {
      const text = (amount / 100.0).toFixed(2);
      navigator.clipboard.writeText(text);
      this.dispatchEvent(
        new CustomEvent('copied', {
          detail: {text},
          bubbles: true,
          composed: true,
        })
      );
    }
  }

  private toggleRow(rowKey: string, expanded: boolean) {
    const nextCollapsed = new Set(this.collapsedIds);
    const nextExpanded = new Set(this.expandedIds);
    if (expanded) {
      nextCollapsed.delete(rowKey);
      nextExpanded.add(rowKey);
    } else {
      nextCollapsed.add(rowKey);
      nextExpanded.delete(rowKey);
    }
    this.collapsedIds = nextCollapsed;
    this.expandedIds = nextExpanded;
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

    const ancestorStack: Array<{indent: number; key: string; collapsed: boolean}> = [];

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
            const indent = r.indent ?? 0;
            const rowKey = this.getRowKey(r, rowIdx);
            while (
              ancestorStack.length > 0 &&
              ancestorStack[ancestorStack.length - 1]!.indent >= indent
            ) {
              ancestorStack.pop();
            }
            const parent = ancestorStack[ancestorStack.length - 1];
            const ancestorCollapsed = ancestorStack.some((a) => a.collapsed);

            if (r.isSpace) {
              return html`<tr id="row-${rowIdx}" ?hidden=${ancestorCollapsed}>
                <td class="account_type">${r.title}</td>
                ${this.months.map(
                  () => html`<td colspan="4" style="border-right:2px double #a00"></td>`
                )}
              </tr>`;
            } else if (r.isTotal) {
              const isExpanded = !this.collapsedIds.has(rowKey);
              ancestorStack.push({indent, key: rowKey, collapsed: !isExpanded});
              return html`<tr id="row-${rowIdx}" bgcolor="#ffd" ?hidden=${ancestorCollapsed}>
                <td>
                  <expand-button
                    .expanded="${isExpanded}"
                    @toggle="${(e: CustomEvent) => this.toggleRow(rowKey, e.detail.expanded)}"
                  ></expand-button>
                  ${Array(indent)
                    .fill(0)
                    .map((_) => html`&nbsp;`)}<b>${r.title}</b>
                </td>
                ${(r.cells ?? []).map((c: GqlTableCell) => {
                  if (!c || c.isClosed) {
                    return html`<td
                      colspan="4"
                      class="closed"
                      style="border-right:2px double #a00"
                    ></td>`;
                  }
                  return html`
                    <td 
                      @click="${(e: MouseEvent) =>
                          this.onAddSubCellClick(e, r.id ?? r.account?.name ?? r.title, c.month, true)}"
                      class="add_sub"
                    >
                      ${currency(c.addSub)}
                    </td>
                    <td
                      @click="${(e: MouseEvent) =>
                        this.onCellClick(e, r.id ?? r.account?.name ?? r.title, c.month, true)}"
                      style="font-weight:700;font-size:75%;"
                      class="balance ${classMap(projectedClass(c))}"
                    >
                      ${currency(c.balance)}
                    </td>
                    <td class="change">${c.percentChange}</td>
                    <td
                      class="${classMap(unaccountedClass(c))}"
                      style="border-right:2px double #a00"
                      @click="${()=>this.copyToClipBoard(c.unaccounted)}"
                    >
                      ${currency(c.unaccounted)}
                    </td>
                  `;
                })}
              </tr>`;
            } else if (r.isNormal) {
              const account = r.account!;
              const hiddenByInactive = r.isInactive && !(parent && this.expandedIds.has(parent.key));
              const isHidden = ancestorCollapsed || hiddenByInactive;
              return html`<tr id="row-${rowIdx}" ?hidden=${isHidden}>
                <td
                  id="${account.name}"
                  @click="${(e: MouseEvent) => this.onTitleCellClick(e, r.id, r.account)}"
                >
                  ${Array(indent)
                    .fill(0)
                    .map((_) => html`&nbsp;`)}
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
                  return html`<td
                      @click="${(e: MouseEvent) => this.onAddSubCellClick(e, r.id ?? r.title, c.month, false)}"
                      class="add_sub"
                    >
                      ${currency(c.addSub)}
                    </td>
                    <td
                      @click="${(e: MouseEvent) => this.onCellClick(e, r.id ?? r.title, c.month, false)}"
                      class="balance ${classMap(projectedClass(c))}"
                      style=${styleMap(coveredStyle(c))}
                    >
                      ${c.hasProjectedTransfer ? html`&#9730;` : nothing}${currency(c.balance)}
                    </td>
                    <td class="change">${c.percentChange ?? html`&#8734;`}</td>
                    <td
                      class="${classMap(unaccountedClass(c))}"
                      style="border-right:2px double #a00"
                      @click="${()=>this.copyToClipBoard(c.unaccounted)}"
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
