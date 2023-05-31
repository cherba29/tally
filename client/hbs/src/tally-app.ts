import {LitElement, css, html, nothing} from 'lit';
import {customElement} from 'lit/decorators.js';
import {Row} from './row';
import {BackendClient} from './api';
import {PopupData} from './utils';
import {transformGqlBudgetData} from './gql_utils';
import {styleMap, StyleInfo} from 'lit/directives/style-map.js';

import './account-tooltip';
import './balance-tooltip';
import './balance-summary-tooltip';
import './summary-table';
import {CellClickEventData} from './summary-table';

@customElement('tally-app')
export class TallyApp extends LitElement {
  static styles = css`
    .toolTip {
      position: absolute;
      padding: 5px;
      border: 1px solid #000;
      background-color: #ffff80;
      font: 10px/12px Arial, Helvetica, sans-serif;
    }
  `;

  private backendClient = new BackendClient();
  private popupMap = new Map<string, PopupData>();
  private timerId: NodeJS.Timer | undefined = undefined;

  // Rendered values.
  private errorMessage: string = '';
  private minutes: string = '00';
  private seconds: string = '00';
  private months: string[] = [];
  private rows: Row[] = [];
  private lastReloadTimestamp = new Date();
  private popupData: PopupData | undefined = undefined;
  private popupOffset = {top: 0, left: 0};

  connectedCallback() {
    super.connectedCallback();
    this.timerId = setInterval(() => {
      const diffTimeSec = Math.round(
        (new Date().getTime() - this.lastReloadTimestamp.getTime()) / 1000
      );
      this.seconds = pad(diffTimeSec % 60);
      this.minutes = pad(Math.floor(diffTimeSec / 60));
      this.requestUpdate();
    }, 1000);
    this.reloadGql();
  }
  disconnectedCallback() {
    super.disconnectedCallback();
    clearInterval(this.timerId);
  }

  render() {
    const popupStyle: StyleInfo = {
      display: this.popupData ? null : 'none',
      top: this.popupOffset.top + 'px',
      left: this.popupOffset.left + 'px',
    };

    return html`
      <div style="position: fixed;font-size: 80%;">
        <button @click="${this.reloadGql}">Reload Data</button>
        <label id="minutes">${this.minutes}</label>:<label id="seconds">${this.seconds}</label>
      </div>
      <div style="padding-top: 20px; color: red; font-size: 16px">
        <pre>${this.errorMessage}</pre>
      </div>
      <div class="toolTip" style=${styleMap(popupStyle)}>${this.tooltipFragment()}</div>
      <summary-table style="font-size: 80%;"
        .months=${this.months}
        .rows=${this.rows}
        @cellclick=${this.onCellClick}
      ></summary-table>
    `;
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
      throw new Error(`Unknown type of popup data for ${this.popupData.id} ${this.popupData}.`);
    }
  }

  private closePopup() {
    this.popupData = undefined;
    this.requestUpdate();
  }

  private onCellClick(e: CustomEvent<CellClickEventData>) {
    this.popupData = this.popupMap.get(e.detail.cellId);
    if (!this.popupData) {
      throw new Error(`Popup data for ${e.detail.cellId} not found.`);
    }
    this.popupOffset = {top: e.detail.mouseEvent.pageY + 10, left: e.detail.mouseEvent.pageX};
    this.requestUpdate();
  }

  private reloadGql() {
    console.log('tally-app Loading graphql');
    this.backendClient
      .loadData()
      .then((result) => {
        this.lastReloadTimestamp = new Date();
        console.log(result);
        if (result.errors) {
          this.errorMessage = result.errors.map((e) => e.message).join('\n');
        } else {
          this.popupData = undefined; // Closes any open popups.
          this.errorMessage = '';

          const dataView = transformGqlBudgetData(result.data.budget || undefined);
          this.months = dataView.months;
          this.rows = dataView.rows;
          this.popupMap = new Map(dataView.popupCells.map((c) => [c.id, c]));
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
