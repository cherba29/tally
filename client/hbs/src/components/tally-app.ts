import {LitElement, css, html, nothing} from 'lit';
import {customElement} from 'lit/decorators.js';
import {BackendClient} from '../api';
import {HeadingPopupData, PopupData, PopupMonthData, PopupMonthSummaryData, Rows} from '../utils';
import {styleMap, StyleInfo} from 'lit/directives/style-map.js';

import './account-tooltip';
import './balance-tooltip';
import './balance-summary-tooltip';
import './summary-table';
import {CellClickEventData} from './summary-table';
import {Month} from '@tally/lib';

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
  private timerId: NodeJS.Timer | undefined = undefined;

  // Rendered values.
  private startMonth: Month = Month.fromDate(new Date()).previous(12);
  private endMonth: Month = Month.fromDate(new Date()).next(3);
  private errorMessage: string = '';
  private minutes: string = '00';
  private seconds: string = '00';
  private months: string[] = [];
  private rows: Rows = {};
  private lastReloadTimestamp = new Date();
  private popupData: PopupData | undefined = undefined;
  private popupOffset = {top: 0, left: 0};
  private currentOwner: string | undefined = undefined;
  private owners: string[] = [];

  override connectedCallback() {
    super.connectedCallback();
    this.timerId = setInterval(() => {
      const diffTimeSec = Math.round(
        (new Date().getTime() - this.lastReloadTimestamp.getTime()) / 1000
      );
      this.seconds = pad(diffTimeSec % 60);
      this.minutes = pad(Math.floor(diffTimeSec / 60));
      this.requestUpdate();
    }, 1000);
    this.startMonth = Month.fromDate(new Date()).previous(12);
    this.endMonth = Month.fromDate(new Date()).next(3);
    console.log('###', this.startMonth, this.endMonth);
    this.reloadTable();
  }
  override disconnectedCallback() {
    super.disconnectedCallback();
    clearInterval(this.timerId);
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
        ${this.endMonth}
        <button @click="${() => this.updateRange(0, -1)}">&gt;</button>
        <button @click="${() => this.updateRange(0, -12)}">&gt;&gt;</button>
        &mdash;
        <button @click="${() => this.updateRange(12, 0)}">&lt;&lt;</button>
        <button @click="${() => this.updateRange(1, 0)}">&lt;</button>
        ${this.startMonth}
        <button @click="${() => this.updateRange(-1, 0)}">&gt;</button>
        <button @click="${() => this.updateRange(-12, 0)}">&gt;&gt;</button>

        <button @click="${() => this.updateRange(12, 12)}">&lt;&lt;</button>
        <button @click="${() => this.updateRange(1, 1)}">&lt;</button>
        <button @click="${() => this.updateRange(-1, -1)}">&gt;</button>
        <button @click="${() => this.updateRange(-12, -12)}">&gt;&gt;</button>
        <button @click="${this.reloadTable}">Reload Table</button>
        <label id="minutes">${this.minutes}</label>:<label id="seconds">${this.seconds}</label>
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
        .month=${this.popupData.month}
        .statementEntries=${this.popupData.statements || []}
        .summary=${this.popupData.summary}
        @close=${this.closePopup}
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

  private onCellClick(e: CustomEvent<CellClickEventData>) {
    if (e.detail.isSummary) {
      this.backendClient
        .loadSummaryData(this.currentOwner ?? '', e.detail.accountName ?? '', e.detail.month ?? '')
        .then((result) => {
          console.log(`PopupData for ${e.detail}`, result);
          const summaryStatement = result.data.summary?.total;
          const statements = result.data.summary?.statements;
          const popupData: PopupMonthSummaryData = {
            accountName: e.detail.accountName ?? '',
            month: e.detail.month ?? '',
            summary: summaryStatement ?? undefined,
            statements: (statements || []).map((stmt) => ({
              name: stmt?.name ?? '',
              stmt: stmt!,
            })),
          };
          this.popupData = popupData;
          this.popupOffset = {
            top: e.detail.mouseEvent.pageY + 10,
            left: e.detail.mouseEvent.pageX,
          };
          this.requestUpdate();
        });
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

  private reloadTable() {
    console.log('tally-app Loading graphql table');
    this.backendClient
      .loadTable(this.currentOwner ?? '', this.startMonth.toString(), this.endMonth.toString())
      .then((result) => {
        this.lastReloadTimestamp = new Date();
        console.log(result);
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

function pad(n: number) {
  return n > 9 ? n.toString() : '0' + n.toString();
}
