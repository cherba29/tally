import {LitElement, css, html, nothing, type TemplateResult} from 'lit';
import {customElement, property, state} from 'lit/decorators.js';
import {type StatementEntry} from '../utils';
import {dateFormat, currency, isProjected} from '../format';
import {classMap, type ClassInfo} from 'lit/directives/class-map.js';
import {type GqlBalance, type GqlSummaryStatement} from '../gql_types';
import {Month} from '../month';
import {
  sortSummaryEntries,
  updateSummarySortOrder,
  type SummarySortColumn,
  type SummarySortCriterion,
} from '../summary-sort';

import './period-buttons';
import {tooltipSharedStyles} from './tooltip-styles';

export interface MonthRangeChange {
  accountName: string;
  startMonth: Month | undefined;
  endMonth: Month;
}

@customElement('balance-summary-tooltip')
export class BalanceSummaryTooltip extends LitElement {
  static override styles = [
    tooltipSharedStyles,
    css`
      tr.total-row td {
        background-color: #f6f8fa;
        font-weight: 700;
      }
      tr.total-row td.projected {
        background-color: #ddf;
      }
      tr.total-row td.confirmed {
        background-color: #dd4;
      }
    `,
  ];

  private __startMonth!: Month;
  private __endMonth!: Month;
  private __statementEntries: StatementEntry[] = [];
  private __summary: GqlSummaryStatement | undefined = undefined;

  @state()
  private sortOrder: SummarySortCriterion[] = [];

  @property({attribute: false})
  accountName: string = '';

  @property()
  set startMonth(value: Month) {
    const oldValue = this.__startMonth;
    this.__startMonth = value;
    this.requestUpdate('startMonth', oldValue);
  }
  get startMonth() {
    return this.__startMonth;
  }

  @property()
  set endMonth(value: Month) {
    const oldValue = this.__endMonth;
    this.__endMonth = value;
    this.requestUpdate('endMonth', oldValue);
  }
  get endMonth() {
    return this.__endMonth;
  }

  @property()
  set statementEntries(value: StatementEntry[]) {
    const oldValue = this.__statementEntries;
    this.__statementEntries = value;
    this.requestUpdate('statementEntries', oldValue);
  }
  get statementEntries() {
    return this.__statementEntries;
  }

  @property()
  set summary(value: GqlSummaryStatement | undefined) {
    const oldValue = this.__summary;
    this.__summary = value;
    this.requestUpdate('summary', oldValue);
  }
  get summary() {
    return this.__summary;
  }

  switchView(e: CustomEvent) {
    this.dispatchEvent(
      new CustomEvent<MonthRangeChange>('month-range-change', {
        detail: {
          accountName: this.accountName,
          startMonth: e.detail.startMonth,
          endMonth: e.detail.endMonth,
        },
      })
    );
  }

  onCloseButton() {
    this.dispatchEvent(new CustomEvent('close'));
  }

  private onSortColumnClick(column: SummarySortColumn) {
    this.sortOrder = updateSummarySortOrder(this.sortOrder, column);
  }

  private renderSortableHeader(
    column: SummarySortColumn,
    label: string | TemplateResult,
    plainLabel: string,
    minWidth: string
  ) {
    const priorityIdx = this.sortOrder.findIndex((c) => c.column === column);
    const criterion = priorityIdx >= 0 ? this.sortOrder[priorityIdx] : undefined;
    const ariaSort =
      priorityIdx === 0 && criterion
        ? criterion.direction === 'asc'
          ? 'ascending'
          : 'descending'
        : 'none';
    const arrow = criterion ? (criterion.direction === 'asc' ? '▲' : '▼') : '';
    const title = criterion
      ? priorityIdx === 0
        ? `Sorted by ${plainLabel} (${criterion.direction === 'asc' ? 'ascending' : 'descending'}) — click to reverse`
        : `Secondary sort #${priorityIdx + 1} (${criterion.direction === 'asc' ? 'ascending' : 'descending'}) — click to sort primarily by ${plainLabel}`
      : `Click to sort by ${plainLabel}`;

    return html`<th
      class="sortable"
      data-sort-col="${column}"
      style="min-width:${minWidth}"
      aria-sort="${ariaSort}"
      title="${title}"
      @click=${() => this.onSortColumnClick(column)}
    >
      ${label}${criterion && priorityIdx === 0
        ? html`<span class="sort-indicator primary">${arrow}</span>`
        : criterion && priorityIdx === 1
          ? html`<span class="sort-indicator secondary">${arrow}<span class="sort-priority">2</span></span>`
          : nothing}
    </th>`;
  }

  override render() {
    const projectedClass = (b: GqlBalance | undefined | null): ClassInfo => {
      const projected = isProjected(b);
      return {
        projected,
        confirmed: !projected,
      };
    };
    const sortedEntries = sortSummaryEntries(this.statementEntries, this.sortOrder);
    return html`
      <div class="header">
        <button
          type="button"
          class="close-btn"
          title="Close summary"
          aria-label="Close"
          @click="${this.onCloseButton}"
        >
          &times;
        </button>
        <span class="title">${this.accountName}</span>
        <span class="meta-pill">${this.startMonth} &ndash; ${this.endMonth}</span>
      </div>
      <div class="controls-bar">
        <period-buttons .currentMonth=${this.endMonth} @button-click=${this.switchView}></period-buttons>
      </div>
      <table>
        <thead>
          <tr>
            ${this.renderSortableHeader('account', 'Account', 'Account', '170px')}
            ${this.renderSortableHeader('startDate', 'Start Date', 'Start Date', '60px')}
            ${this.renderSortableHeader('startBalance', html`Start<br />Balance`, 'Start Balance', '50px')}
            ${this.renderSortableHeader('endDate', 'End Date', 'End Date', '60px')}
            ${this.renderSortableHeader('endBalance', html`End<br />Balance`, 'End Balance', '50px')}
            ${this.renderSortableHeader('change', 'Change', 'Change', '50px')}
            ${this.renderSortableHeader('percentChange', html`Prct<br />Change`, 'Prct Change', '50px')}
            ${this.renderSortableHeader(
              'annualizedPercentChange',
              html`An Prct<br />Change`,
              'An Prct Change',
              '50px'
            )}
            ${this.renderSortableHeader('inFlows', 'Inflows', 'Inflows', '50px')}
            ${this.renderSortableHeader('outFlows', 'OutFlows', 'OutFlows', '50px')}
            ${this.renderSortableHeader('addSub', 'AddSub', 'AddSub', '50px')}
            ${this.renderSortableHeader('income', 'Income', 'Income', '50px')}
            ${this.renderSortableHeader('expenses', 'Expenses', 'Expenses', '50px')}
            ${this.renderSortableHeader('transfers', 'Transfers', 'Transfers', '50px')}
            ${this.renderSortableHeader('unaccounted', 'Unaccounted', 'Unaccounted', '50px')}
          </tr>
        </thead>
        <tbody>
          ${sortedEntries.map(
            (e, index) =>
              html`<tr class="highlight">
                <td>${index + 1} ${e.name.split('/').pop()}</td>
                <td align="middle">${dateFormat(e.stmt.startBalance?.date)}</td>
                <td class=${classMap(projectedClass(e.stmt.startBalance))}>
                  ${currency(e.stmt.startBalance?.amount)}
                </td>
                <td align="middle">${dateFormat(e.stmt.endBalance?.date)}</td>
                <td class=${classMap(projectedClass(e.stmt.endBalance))}>
                  ${currency(e.stmt.endBalance?.amount)}
                </td>
                <td align="right">${currency(e.stmt.change)}</td>
                <td align="right">${e.stmt.percentChange}</td>
                <td align="right">${e.stmt.annualizedPercentChange}</td>
                <td align="right">${currency(e.stmt.inFlows)}</td>
                <td align="right">${currency(e.stmt.outFlows)}</td>
                <td align="right">${currency(e.stmt.addSub)}</td>
                <td align="right">${currency(e.stmt.income)}</td>
                <td align="right">${currency(e.stmt.totalPayments)}</td>
                <td align="right">${currency(e.stmt.totalTransfers)}</td>
                <td align="right">${currency(e.stmt.unaccounted)}</td>
              </tr>`
          )}
          <tr class="total-row">
            <td><b>Total</b></td>
            <td align="middle">${dateFormat(this.summary?.startBalance?.date)}</td>
            <td class=${classMap(projectedClass(this.summary?.startBalance))}>
              ${currency(this.summary?.startBalance?.amount)}
            </td>
            <td align="middle">${dateFormat(this.summary?.endBalance?.date)}</td>
            <td class=${classMap(projectedClass(this.summary?.endBalance))}>
              ${currency(this.summary?.endBalance?.amount)}
            </td>
            <td align="right">${currency(this.summary?.change)}</td>
            <td align="right">${this.summary?.percentChange ?? nothing}</td>
            <td align="right">${this.summary?.annualizedPercentChange ?? nothing}</td>
            <td align="right">${currency(this.summary?.inFlows)}</td>
            <td align="right">${currency(this.summary?.outFlows)}</td>
            <td align="right">${currency(this.summary?.addSub)}</td>
            <td align="right">${currency(this.summary?.income)}</td>
            <td align="right">${currency(this.summary?.totalPayments)}</td>
            <td align="right">${currency(this.summary?.totalTransfers)}</td>
            <td align="right">${currency(this.summary?.unaccounted)}</td>
          </tr>
        </tbody>
      </table>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'balance-summary-tooltip': BalanceSummaryTooltip;
  }
}
