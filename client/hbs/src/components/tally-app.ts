import {LitElement, css, html, nothing} from 'lit';
import {customElement} from 'lit/decorators.js';
import {BackendClient} from '../api';
import {HeadingPopupData, PopupData, PopupMonthData, PopupMonthSummaryData, Rows} from '../utils';
import {styleMap, StyleInfo} from 'lit/directives/style-map.js';

import './account-tooltip';
import './balance-tooltip';
import './balance-summary-tooltip';
import './summary-table';
import {MonthRangeChange} from './balance-summary-tooltip';
import {CellClickEventData} from './summary-table';
import {Month} from '@tally/lib/core/month';

@customElement('tally-app')
export class TallyApp extends LitElement {
  static override styles = css`
    .toolTip {
      position: absolute;
      padding: 5px;
      border: 1px solid #000;
      background-color: #ffff80;
      font: 10px/12px Arial, Helvetica, sans-serif;
    }
    .tabcontent {
      border-top: none;
    }
  `;

  private backendClient = new BackendClient();
  private popupMap = new Map<string, PopupData>();

  // Rendered values.
  private startMonth: Month = Month.fromDate(new Date()).previous(12);
  private endMonth: Month = Month.fromDate(new Date()).next(2);
  private errorMessage: string = '';
  private months: string[] = [];
  private rows: Rows = {};
  private popupData: PopupData | undefined = undefined;
  private popupOffset = {top: 0, left: 0};
  private currentOwner: string | undefined = undefined;
  private owners: string[] = [];

  override connectedCallback() {
    super.connectedCallback();
    this.startMonth = Month.fromDate(new Date()).previous(12);
    this.endMonth = Month.fromDate(new Date()).next(2);
    console.log('###', this.startMonth, this.endMonth);
    this.reloadTable();
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
      <div style="position: fixed;font-size: 80%;">
        <button @click="${() => this.updateRange(0, 12)}">&lt;&lt;</button>
        <button @click="${() => this.updateRange(0, 1)}">&lt;</button>
        <span style="font-family: monospace;">${this.endMonth}</span>
        <button @click="${() => this.updateRange(0, -1)}">&gt;</button>
        <button @click="${() => this.updateRange(0, -12)}">&gt;&gt;</button>
        &mdash;
        <button @click="${() => this.updateRange(12, 0)}">&lt;&lt;</button>
        <button @click="${() => this.updateRange(1, 0)}">&lt;</button>
        <span style="font-family: monospace;">${this.startMonth}</span>
        <button @click="${() => this.updateRange(-1, 0)}">&gt;</button>
        <button @click="${() => this.updateRange(-12, 0)}">&gt;&gt;</button>

        <button @click="${() => this.updateRange(12, 12)}">&lt;&lt;</button>
        <button @click="${() => this.updateRange(1, 1)}">&lt;</button>
        <button @click="${() => this.updateRange(-1, -1)}">&gt;</button>
        <button @click="${() => this.updateRange(-12, -12)}">&gt;&gt;</button>
        <button @click="${this.reloadTable}">Reload Table</button>
      </div>
      <div style="padding-top: 20px; color: red; font-size: 16px">
        <pre>${this.errorMessage}</pre>
      </div>
      <div class="toolTip" style=${styleMap(popupStyle)}>${this.tooltipFragment()}</div>
      <div>
        ${this.owners.map(
          (owner) => html`<button @click=${() => this.tabClick(owner)}>${owner[0]}</button>`
        )}
      </div>
      ${Object.entries(this.rows).map(([owner, rows]) => {
        return html`<div class="tabcontent" style=${styleMap(tabStyle(owner))}>
          <summary-table
            style="font-size: 80%;"
            .months=${this.months}
            .rows=${rows}
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

  tabClick(owner: string) {
    this.currentOwner = owner;
    if (this.popupMap.size === 0) {
      // If data was loading individually.
      this.reloadTable();
    } else {
      this.requestUpdate();
    }
  }

  tooltipFragment() {
    if (!this.popupData) {
      return nothing;
    } else if ('summary' in this.popupData) {
      return html` <balance-summary-tooltip
        .accountName=${this.popupData.accountName}
        .startMonth=${this.popupData.month}
        .endMonth=${this.popupData.month}
        .statementEntries=${this.popupData.statements || []}
        .summary=${this.popupData.summary}
        @close=${this.closePopup}
        @month-range-change=${this.popupMonthRangeChange}
      >
      </balance-summary-tooltip>`;
    } else if ('account' in this.popupData) {
      return html`<account-tooltip
        .account=${this.popupData.account}
        @close=${this.closePopup}
      ></account-tooltip>`;
    } else if ('stmt' in this.popupData) {
      return html` <balance-tooltip
        .accountName=${this.popupData.accountName}
        .month=${this.popupData.month}
        .stmt=${this.popupData.stmt}
        @close=${this.closePopup}
      >
      </balance-tooltip>`;
    } else {
      throw new Error(`Unknown type of popup data ${this.popupData}.`);
    }
  }

  private closePopup() {
    this.popupData = undefined;
    this.requestUpdate();
  }

  private popupMonthRangeChange(e: CustomEvent<MonthRangeChange>) {
    console.log('### popup month range change', e.detail.startMonth, '-->', e.detail.endMonth);
    this.reloadPopupSummaryStatement(
      e.detail.accountName,
      e.detail.startMonth?.toString(),
      e.detail.endMonth.toString()
    );
  }

  private onCellClick(e: CustomEvent<CellClickEventData>) {
    if (e.detail.isSummary) {
      this.reloadPopupSummaryStatement(
        e.detail.accountName ?? '',
        e.detail.month ?? '',
        e.detail.month ?? ''
      );
      this.popupOffset = {
        top: e.detail.mouseEvent.pageY + 10,
        left: e.detail.mouseEvent.pageX,
      };
    } else if (e.detail.month) {
      this.backendClient
        .loadStatement(this.currentOwner ?? '', e.detail.accountName ?? '', e.detail.month ?? '')
        .then((result) => {
          console.log(`PopupData for ${e.detail}`, result);
          const statement = result.data.statement;
          const popupData: PopupMonthData = {
            accountName: e.detail.accountName ?? '',
            month: e.detail.month ?? '',
            stmt: statement!,
          };
          this.popupData = popupData;
          this.popupOffset = {
            top: e.detail.mouseEvent.pageY + 10,
            left: e.detail.mouseEvent.pageX,
          };
          this.requestUpdate();
        });
    } else {
      const popupData: HeadingPopupData = {
        account: e.detail.account!,
      };
      this.popupData = popupData;
      this.popupOffset = {top: e.detail.mouseEvent.pageY + 10, left: e.detail.mouseEvent.pageX};
      this.requestUpdate();
    }
  }

  private reloadPopupSummaryStatement(
    accountName: string,
    startMonth: string | undefined,
    endMonth: string
  ) {
    this.backendClient
      .loadSummaryData(this.currentOwner ?? '', accountName, startMonth, endMonth)
      .then((result) => {
        console.log(`PopupData for ${accountName} ${startMonth}-${endMonth}`, result);
        const summaryStatement = result.data.summary?.total;
        const statements = result.data.summary?.statements;
        const popupData: PopupMonthSummaryData = {
          accountName,
          month: endMonth,
          summary: summaryStatement ?? undefined,
          statements: (statements || []).map((stmt) => ({
            name: stmt?.name ?? '',
            stmt: stmt!,
          })),
        };
        this.popupData = popupData;
        this.requestUpdate();
      });
  }

  private reloadTable() {
    console.log(`tally-app Loading graphql table ${this.startMonth} - ${this.endMonth}`);
    this.backendClient
      .loadTable(this.currentOwner ?? '', this.startMonth.toString(), this.endMonth.toString())
      .then((result) => {
        console.log('backend response', result);
        if (result.errors) {
          this.errorMessage = result.errors.map((e) => e.message).join('\n');
        } else {
          this.popupData = undefined; // Closes any open popups.
          this.errorMessage = '';
          const table = result.data.table;
          this.currentOwner = table?.currentOwner ?? undefined;
          this.owners = table?.owners?.filter((owner) => !!owner).map((owner) => owner!) ?? [];
          this.months = table?.months?.map((value) => value.toString()) || [];
          this.rows = {
            [this.currentOwner ?? '']: (table?.rows || []).map((r) => r!),
          };
          this.popupMap = new Map<string, PopupData>();
        }
        this.requestUpdate();
      })
      .catch((error) => {
        this.errorMessage = error.message;
        this.requestUpdate();
        throw error;
      });
  }
}
