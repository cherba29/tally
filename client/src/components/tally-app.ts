import {LitElement, css, html, nothing} from 'lit';
import {customElement, state} from 'lit/decorators.js';
import {BackendClient} from '../api';
import {
  type HeadingPopupData, 
  type PopupData, 
  type PopupMonthData, 
  type PopupMonthSummaryData, 
  type PopupTransferSummaryData,
  type Rows
} from '../utils';
import {styleMap, type StyleInfo} from 'lit/directives/style-map.js';

import './account-tooltip';
import './balance-tooltip';
import './balance-summary-tooltip';
import './summary-table';
import './transfers-summary-tooltip';

import {type MonthRangeChange} from './balance-summary-tooltip';
import {type TransfersSummaryMonthRangeChange} from './transfers-summary-tooltip';
import {type CellClickEventData} from './summary-table';
import {type GqlTableRow} from '../gql_types';
import {Month} from '../month';

type ActivePopupRequest =
  | {type: 'summary'; accountName: string; startMonth: Month | undefined; endMonth: Month}
  | {
      type: 'transfers-summary';
      accountPath: string;
      startMonth: Month | undefined;
      endMonth: Month;
    }
  | {type: 'statement'; accountName: string; month: Month}
  | {type: 'account'; rowId: string};

@customElement('tally-app')
export class TallyApp extends LitElement {
  static override styles = css`
    :host {
      display: block;
      font-family: Arial, Helvetica, sans-serif;
    }
    .toolbar {
      position: sticky;
      top: 0;
      z-index: 20;
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 8px;
      padding: 6px 8px;
      background-color: #f6f8fa;
      border-bottom: 1px solid #d0d7de;
      font-size: 12px;
    }
    .control-group {
      display: inline-flex;
      align-items: center;
      gap: 2px;
    }
    .group-label {
      color: #57606a;
      font-weight: 600;
      margin-right: 3px;
    }
    .month-label {
      font-family: monospace;
      font-weight: 700;
      padding: 0 4px;
      min-width: 56px;
      text-align: center;
    }
    .toolbar button {
      padding: 2px 6px;
      font-size: 11px;
      border: 1px solid #c5ccd3;
      border-radius: 3px;
      background-color: #fff;
      cursor: pointer;
    }
    .toolbar button:hover {
      background-color: #eaeef2;
    }
    .owner-tabs {
      display: flex;
      flex-wrap: wrap;
      gap: 4px;
      padding: 6px 8px 0;
      border-bottom: 1px solid #d0d7de;
      background-color: #fff;
    }
    .owner-tab {
      padding: 4px 10px;
      font-size: 12px;
      border: 1px solid #d0d7de;
      border-bottom: none;
      border-radius: 4px 4px 0 0;
      background-color: #f6f8fa;
      color: #57606a;
      cursor: pointer;
    }
    .owner-tab:hover {
      background-color: #eaeef2;
    }
    .owner-tab.active {
      background-color: #fff;
      color: #1f2328;
      font-weight: 700;
      border-color: #8c959f;
      margin-bottom: -1px;
      padding-bottom: 5px;
    }
    .error-banner {
      padding: 6px 8px;
      color: #b42318;
      background-color: #fef3f2;
      border-bottom: 1px solid #fecdca;
      font-size: 14px;
    }
    .error-banner pre {
      margin: 0;
      white-space: pre-wrap;
    }
    .toolTip {
      position: absolute;
      z-index: 30;
      padding: 5px;
      border: 1px solid #000;
      background-color: #ffff80;
      font:
        10px/12px Arial,
        Helvetica,
        sans-serif;
    }
    .tabcontent {
      border-top: none;
      padding: 4px 8px 8px;
      overflow: auto;
      max-height: calc(100vh - 76px);
    }
  `;

  backendClient = new BackendClient();
  private unsubscribeUpdates?: () => void;
  private activePopup: ActivePopupRequest | undefined = undefined;
  private popupRequestId = 0;

  // Rendered reactive state.
  @state() private startMonth: Month = Month.fromDate(new Date()).previous(12);
  @state() private endMonth: Month = Month.fromDate(new Date()).next(2);
  @state() private errorMessage: string = '';
  @state() private months: string[] = [];
  @state() private rows: Rows = {};
  @state() private popupData: PopupData | undefined = undefined;
  @state() private popupOffset = {top: 0, left: 0};
  @state() private currentOwner: string | undefined = undefined;
  @state() private owners: string[] = [];

  override connectedCallback() {
    super.connectedCallback();
    this.startMonth = Month.fromDate(new Date()).previous(12);
    this.endMonth = Month.fromDate(new Date()).next(2);
    this.reloadTable();
    this.unsubscribeUpdates = this.backendClient.subscribeToUpdates(() => this.reloadTable());
  }

  override disconnectedCallback() {
    super.disconnectedCallback();
    this.unsubscribeUpdates?.();
  }

  override render() {
    const popupStyle: StyleInfo = {
      display: this.popupData ? null : 'none',
      top: this.popupOffset.top + 'px',
      left: this.popupOffset.left + 'px',
    };
    const tabStyle = (owner: string): StyleInfo => ({
      display: this.currentOwner === owner ? null : 'none',
    });

    return html`
      <header class="toolbar">
        <span class="control-group" data-group="end">
          <span class="group-label">End:</span>
          <button title="Move end month forward 1 year" @click="${() => this.updateRange(0, 12)}">+1y</button>
          <button title="Move end month forward 1 month" @click="${() => this.updateRange(0, 1)}">+1m</button>
          <span class="month-label">${this.endMonth}</span>
          <button title="Move end month back 1 month" @click="${() => this.updateRange(0, -1)}">-1m</button>
          <button title="Move end month back 1 year" @click="${() => this.updateRange(0, -12)}">-1y</button>
        </span>
        <span class="control-group" data-group="start">
          <span class="group-label">Start:</span>
          <button title="Move start month forward 1 year" @click="${() => this.updateRange(12, 0)}">+1y</button>
          <button title="Move start month forward 1 month" @click="${() => this.updateRange(1, 0)}">+1m</button>
          <span class="month-label">${this.startMonth}</span>
          <button title="Move start month back 1 month" @click="${() => this.updateRange(-1, 0)}">-1m</button>
          <button title="Move start month back 1 year" @click="${() => this.updateRange(-12, 0)}">-1y</button>
        </span>
        <span class="control-group" data-group="shift">
          <span class="group-label">Shift:</span>
          <button title="Shift window forward 1 year" @click="${() => this.updateRange(12, 12)}">+1y</button>
          <button title="Shift window forward 1 month" @click="${() => this.updateRange(1, 1)}">+1m</button>
          <button title="Shift window back 1 month" @click="${() => this.updateRange(-1, -1)}">-1m</button>
          <button title="Shift window back 1 year" @click="${() => this.updateRange(-12, -12)}">-1y</button>
        </span>
        <span class="control-group" data-group="actions">
          <button title="Reset range to default window around today" @click="${this.resetRange}">Today</button>
          <button title="Reload table data from server" @click="${this.reloadTable}">Reload Table</button>
        </span>
      </header>
      ${this.errorMessage
        ? html`<div class="error-banner"><pre>${this.errorMessage}</pre></div>`
        : nothing}
      <div class="toolTip" style=${styleMap(popupStyle)}>${this.tooltipFragment()}</div>
      ${this.owners.length > 0
        ? html`<nav class="owner-tabs" role="tablist">
            ${this.owners.map(
              (owner) => html`<button
                role="tab"
                class="owner-tab ${this.currentOwner === owner ? 'active' : ''}"
                aria-selected="${this.currentOwner === owner ? 'true' : 'false'}"
                @click=${() => this.tabClick(owner)}
              >
                ${owner}
              </button>`
            )}
          </nav>`
        : nothing}
      ${Object.entries(this.rows).map(([owner, rows]) => {
        return html`<div class="tabcontent" style=${styleMap(tabStyle(owner))}>
          <summary-table
            style="font-size: 80%;"
            .months=${this.months}
            .rows=${rows}
            @addSubCellClick=${this.onAddSubCellClick}
            @cellclick=${this.onCellClick}
          ></summary-table>
        </div>`;
      })}
    `;
  }

  updateRange(startDelta: number, endDelta: number) {
    this.startMonth = this.startMonth.next(startDelta);
    this.endMonth = this.endMonth.next(endDelta);
    this.reloadTable();
  }

  resetRange() {
    const today = Month.fromDate(new Date());
    this.startMonth = today.previous(12);
    this.endMonth = today.next(2);
    this.reloadTable();
  }

  tabClick(owner: string) {
    if (this.currentOwner !== owner) {
      this.closePopup();
    }
    this.currentOwner = owner;
    this.reloadTable();
  }

  tooltipFragment() {
    if (!this.popupData) {
      return nothing;
    }
    switch (this.popupData.kind) {
      case 'summary':
        return html` <balance-summary-tooltip
          .accountName=${this.popupData.accountName}
          .startMonth=${this.popupData.startMonth}
          .endMonth=${this.popupData.endMonth}
          .statementEntries=${this.popupData.statements || []}
          .summary=${this.popupData.summary}
          @close=${this.closePopup}
          @month-range-change=${this.popupMonthRangeChange}
        >
        </balance-summary-tooltip>`;
      case 'account':
        return html`<account-tooltip
          .account=${this.popupData.account}
          @close=${this.closePopup}
        ></account-tooltip>`;
      case 'statement':
        return html` <balance-tooltip
          .accountName=${this.popupData.accountName}
          .month=${this.popupData.month}
          .stmt=${this.popupData.stmt}
          @close=${this.closePopup}
        >
        </balance-tooltip>`;
      case 'transfers-summary':
        return html`<transfers-summary-tooltip
          .accountPath=${this.popupData.accountPath}
          .months=${this.popupData.months}
          .monthlyData=${this.popupData.monthlyData}
          @close=${this.closePopup}
          @month-range-change=${this.popupTransfersSummaryMonthRangeChange}
        >
        </transfers-summary-tooltip>`;
    }
  }

  private closePopup() {
    this.popupRequestId++;
    this.activePopup = undefined;
    this.popupData = undefined;
  }

  private popupMonthRangeChange(e: CustomEvent<MonthRangeChange>) {
    this.activePopup = {
      type: 'summary',
      accountName: e.detail.accountName,
      startMonth: e.detail.startMonth,
      endMonth: e.detail.endMonth,
    };
    this.reloadPopupSummaryStatement(
      e.detail.accountName,
      e.detail.startMonth,
      e.detail.endMonth
    );
  }

  private popupTransfersSummaryMonthRangeChange(e: CustomEvent<TransfersSummaryMonthRangeChange>) {
    this.activePopup = {
      type: 'transfers-summary',
      accountPath: e.detail.accountPath,
      startMonth: e.detail.startMonth,
      endMonth: e.detail.endMonth,
    };
    this.reloadPopupTransfersSummary(
      e.detail.accountPath,
      e.detail.startMonth,
      e.detail.endMonth
    );
  }

  private onAddSubCellClick(e: CustomEvent<CellClickEventData>) {
    this.popupOffset = {
      top: e.detail.mouseEvent.pageY + 10,
      left: e.detail.mouseEvent.pageX,
    };
    this.activePopup = {
      type: 'transfers-summary',
      accountPath: e.detail.rowId,
      startMonth: this.startMonth,
      endMonth: e.detail.month!,
    };
    this.reloadPopupTransfersSummary(e.detail.rowId, this.startMonth, e.detail.month!);
  }

  private onCellClick(e: CustomEvent<CellClickEventData>) {
    this.popupOffset = {
      top: e.detail.mouseEvent.pageY + 10,
      left: e.detail.mouseEvent.pageX,
    };
    if (e.detail.isSummary) {
      const accountName = e.detail.rowId ?? '';
      this.activePopup = {
        type: 'summary',
        accountName,
        startMonth: e.detail.month,
        endMonth: e.detail.month!,
      };
      this.reloadPopupSummaryStatement(accountName, e.detail.month, e.detail.month!);
    } else if (e.detail.month) {
      const accountName = e.detail.rowId ?? '';
      this.activePopup = {
        type: 'statement',
        accountName,
        month: e.detail.month,
      };
      this.reloadPopupStatement(accountName, e.detail.month);
    } else {
      this.popupRequestId++;
      this.activePopup = {
        type: 'account',
        rowId: e.detail.rowId,
      };
      const popupData: HeadingPopupData = {
        kind: 'account',
        account: e.detail.account!,
      };
      this.popupData = popupData;
    }
  }

  private reloadPopupStatement(accountName: string, month: Month) {
    const requestId = ++this.popupRequestId;
    this.backendClient
      .loadStatement(accountName, month.toString())
      .then((result) => {
        if (requestId !== this.popupRequestId) return;
        const statement = result.data?.statement;
        if (result.error || !statement) {
          this.popupData = undefined;
        } else {
          const popupData: PopupMonthData = {
            kind: 'statement',
            accountName,
            month,
            stmt: statement,
          };
          this.popupData = popupData;
        }
      })
      .catch(() => {
        if (requestId !== this.popupRequestId) return;
        this.popupData = undefined;
      });
  }

  private reloadPopupSummaryStatement(
    accountName: string,
    startMonth: Month | undefined,
    endMonth: Month
  ) {
    const requestId = ++this.popupRequestId;
    this.backendClient
      .loadSummaryData(accountName, startMonth?.toString(), endMonth.toString())
      .then((result) => {
        if (requestId !== this.popupRequestId) return;
        const summary = result.data?.summary;
        if (result.error || !summary) {
          this.popupData = undefined;
        } else {
          const popupData: PopupMonthSummaryData = {
            kind: 'summary',
            accountName,
            startMonth: startMonth ?? endMonth,
            endMonth: endMonth,
            summary: summary.total ?? undefined,
            statements: (summary.statements || []).map((stmt) => ({
              name: stmt?.name ?? '',
              stmt: stmt!,
            })),
          };
          this.popupData = popupData;
        }
      })
      .catch(() => {
        if (requestId !== this.popupRequestId) return;
        this.popupData = undefined;
      });
  }

  private reloadPopupTransfersSummary(
    accountPath: string,
    startMonth: Month | undefined,
    endMonth: Month
  ) {
    const requestId = ++this.popupRequestId;
    this.backendClient
      .loadTransferSummaryData(accountPath, startMonth?.toString(), endMonth.toString())
      .then((result) => {
        if (requestId !== this.popupRequestId) return;
        const transfersSummary = result.data?.transfersSummary;
        if (result.error || !transfersSummary) {
          this.popupData = undefined;
        } else {
          const popupData: PopupTransferSummaryData = {
            kind: 'transfers-summary',
            accountPath: accountPath,
            months: transfersSummary.months,
            monthlyData: transfersSummary.data,
          };
          this.popupData = popupData;
        }
      })
      .catch(() => {
        if (requestId !== this.popupRequestId) return;
        this.popupData = undefined;
      });
  }

  private refreshActivePopup(rows: GqlTableRow[]) {
    if (!this.activePopup) {
      this.popupData = undefined;
      return;
    }
    switch (this.activePopup.type) {
      case 'summary':
        this.reloadPopupSummaryStatement(
          this.activePopup.accountName,
          this.activePopup.startMonth,
          this.activePopup.endMonth
        );
        break;
      case 'transfers-summary':
        this.reloadPopupTransfersSummary(
          this.activePopup.accountPath,
          this.activePopup.startMonth,
          this.activePopup.endMonth
        );
        break;
      case 'statement':
        this.reloadPopupStatement(this.activePopup.accountName, this.activePopup.month);
        break;
      case 'account': {
        this.popupRequestId++;
        const {rowId} = this.activePopup;
        const row = rows.find((r) => r.id === rowId);
        if (row?.account) {
          this.popupData = {kind: 'account', account: row.account};
        } else {
          this.activePopup = undefined;
          this.popupData = undefined;
        }
        break;
      }
    }
  }

  private reloadTable() {
    this.backendClient
      .loadTable(this.currentOwner ?? '', this.startMonth.toString(), this.endMonth.toString())
      .then((result) => {
        if (result.error) {
          this.errorMessage = result.error.message;
          this.popupData = undefined;
        } else {
          this.errorMessage = '';
          const table = result.data!.table;
          this.currentOwner = table?.currentOwner ?? undefined;
          this.owners = table?.owners?.filter((owner) => !!owner).map((owner) => owner!) ?? [];
          this.months = table?.months?.map((value) => value.toString()) || [];
          const freshRows = (table?.rows || []).map((r) => r!);
          this.rows = {
            [this.currentOwner ?? '']: freshRows,
          };
          this.refreshActivePopup(freshRows);
        }
      })
      .catch((error: Error) => {
        this.errorMessage = error.message;
        this.popupData = undefined;
      });
  }
}
