import {LitElement, css, html, nothing} from 'lit';
import {customElement, property} from 'lit/decorators.js';
import { type StatementEntry } from '../utils';
import {dateFormat, currency, isProjected} from '../format';
import {classMap, type ClassInfo} from 'lit/directives/class-map.js';
import {type GqlBalance, type GqlSummaryStatement} from '../gql_types';
import {Month} from '@tally/lib/core/month';

import {mapViewToMonths} from './period-buttons';
import './period-buttons';

export interface MonthRangeChange {
  accountName: string;
  startMonth: Month | undefined;
  endMonth: Month;
}

@customElement('balance-summary-tooltip')
export class BalanceSummaryTooltip extends LitElement {
  static override styles = css`
    td {
      border-left: 1px solid #c3c3c3;
      border-top: 1px solid #c3c3c3;
      padding: 1px;
      vertical-align: middle;
    }
    .highlight:hover {
      background-color: #40c340;
    }
    .projected {
      background-color: #ddf;
      text-align: right;
    }
    .confirmed {
      background-color: #dd4;
      text-align: right;
    }
  `;

  private __startMonth!: Month;
  private __endMonth!: Month;
  private __statementEntries: StatementEntry[] = [];
  private __summary: GqlSummaryStatement | undefined = undefined;

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

  @property({attribute: false})
  period: string = '';

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
    const oldValue = this.__statementEntries;
    this.__summary = value;
    this.requestUpdate('summary', oldValue);
  }
  get summary() {
    return this.__summary;
  }

  switchView(e: CustomEvent) {
    const numberOfMonths = mapViewToMonths(e.detail.period, this.endMonth);
    const startMonth =
      numberOfMonths !== undefined ? this.endMonth.previous(numberOfMonths - 1) : undefined;
    this.startMonth = startMonth ? startMonth : this.endMonth;
    const period = this.endMonth.distance(startMonth ?? this.endMonth) + 1;
    const years = Math.floor(period / 12);
    const months = period - 12 * years;
    this.period = (years ? years + 'y' : '') + (months ? months + 'm' : '');
    this.dispatchEvent(
      new CustomEvent<MonthRangeChange>('month-range-change', {
        detail: {
          accountName: this.accountName,
          startMonth,
          endMonth: this.endMonth,
        },
      })
    );
  }

  onCloseButton() {
    this.dispatchEvent(new CustomEvent('close'));
  }

  override render() {
    const projectedClass = (b: GqlBalance | undefined | null): ClassInfo => {
      const projected = isProjected(b);
      return {
        projected,
        confirmed: !projected,
      };
    };

    return html`
      <span @click="${this.onCloseButton}">XXX</span>
      <period-buttons @button-click=${this.switchView}></period-buttons>
      <span style="float:right;"
        >${this.accountName} ${this.startMonth} - ${this.endMonth} (${this.period})</span
      >
      <table>
        <thead>
          <th style="min-width:170px">Account</th>
          <th style="min-width:60px">Start Date</th>
          <th style="min-width:50px">Start<br />Balance</th>
          <th style="min-width:60px">End Date</th>
          <th style="min-width:50px">End<br />Balance</th>
          <th style="min-width:50px">Change</th>
          <th style="min-width:50px">Prct<br />Change</th>
          <th style="min-width:50px">An Prct<br />Change</th>
          <th style="min-width:50px">Inflows</th>
          <th style="min-width:50px">OutFlows</th>
          <th style="min-width:50px">AddSub</th>
          <th style="min-width:50px">Income</th>
          <th style="min-width:50px">Expenses</th>
          <th style="min-width:50px">Transfers</th>
          <th style="min-width:50px">Unaccounted</th>
        </thead>
        <tbody>
          ${this.statementEntries.map(
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
          <tr>
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
