import {LitElement, css, html} from 'lit';
import {customElement, state, property} from 'lit/decorators.js';
import {classMap} from 'lit/directives/class-map.js';
import {Month} from '../month';

export enum SummaryView {
  MONTH_1 = '1m',
  MONTH_3 = '3m',
  MONTH_6 = '6m',
  MONTH_9 = '9m',
  YTD = 'ytd',
  YEAR_1 = '1y',
  YEAR_2 = '2y',
  YEAR_3 = '3y',
  YEAR_5 = '5y',
  YEAR_7 = '7y',
  YEAR_10 = '10y',
  YEAR_15 = '15y',
  YEAR_20 = '20y',
  YEAR_30 = '30y',
  YEAR_MAX = 'Max',
}

export function mapViewToMonths(viewMonthRange: SummaryView, currentMonth: Month): number | undefined {
  switch (viewMonthRange) {
    case SummaryView.MONTH_1:
      return 1;
    case SummaryView.MONTH_3:
      return 3;
    case SummaryView.MONTH_6:
      return 6;
    case SummaryView.MONTH_9:
      return 9;
    case SummaryView.YTD:
      return currentMonth.month + 1;
    case SummaryView.YEAR_1:
      return 12;
    case SummaryView.YEAR_2:
      return 24;
    case SummaryView.YEAR_3:
      return 36;
    case SummaryView.YEAR_5:
      return 60;
    case SummaryView.YEAR_7:
      return 84;
    case SummaryView.YEAR_10:
      return 120;
    case SummaryView.YEAR_15:
      return 180;
    case SummaryView.YEAR_20:
      return 240;
    case SummaryView.YEAR_30:
      return 360;
    case SummaryView.YEAR_MAX:
      return undefined;
  }
}

@customElement('period-buttons')
export class PeriodButtons extends LitElement {
  static override styles = css`
    :host {
      display: inline-flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 2px;
    }
    button {
      padding: 1px 5px;
      font-size: 10px;
      border: 1px solid #c5ccd3;
      border-radius: 3px;
      background-color: #fff;
      color: #1f2328;
      cursor: pointer;
    }
    button:hover {
      background-color: #eaeef2;
    }
    button.active {
      background-color: #1f6feb;
      border-color: #0969da;
      color: #fff;
      font-weight: 700;
      box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.25);
    }
    button.active:hover {
      background-color: #1a62d6;
    }
  `;

  private __currentMonth!: Month;

  @property()
  set currentMonth(value: Month) {
    const oldValue = this.__currentMonth;
    this.__currentMonth = value;
    this.requestUpdate('currentMonth', oldValue);
  }
  get currentMonth() {
    return this.__currentMonth;
  }

  @state()
  private selectedView: keyof typeof SummaryView = 'MONTH_1';

  switchView(e: Event) {
    const viewType = (e.currentTarget as Element).getAttribute('key') as keyof typeof SummaryView;
    const numberOfMonths = mapViewToMonths(SummaryView[viewType], this.currentMonth);
    const startMonth =
      numberOfMonths !== undefined ? this.currentMonth?.previous(numberOfMonths - 1) : undefined;

    this.selectedView = viewType;

    this.dispatchEvent(
      new CustomEvent('button-click', {
        detail: {
          startMonth: startMonth ?? this.currentMonth,
          endMonth: this.currentMonth,
        },
      })
    );
  }

  override render() {
    return html`
      ${Object.keys(SummaryView).map((key) => {
        const isActive = key === this.selectedView;
        return html`<button
          type="button"
          key="${key}"
          class=${classMap({active: isActive})}
          aria-pressed="${isActive ? 'true' : 'false'}"
          @click="${this.switchView}"
        >
          ${SummaryView[key as keyof typeof SummaryView]}
        </button>`;
      })}
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'period-buttons': PeriodButtons;
  }
}
