import {LitElement, css, html} from 'lit';
import {customElement} from 'lit/decorators.js';
import {Month} from '@tally/lib/core/month';

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
  static override styles = css``;

  onCloseButton() {
    this.dispatchEvent(new CustomEvent('close'));
  }

  switchView(e: Event) {
    const viewType = (e.target as Element).getAttribute('key') as keyof typeof SummaryView;
    this.dispatchEvent(new CustomEvent('button-click', {
      detail: {
       period: SummaryView[viewType]
      }
    }));
  }

  override render() {
    return html`
      ${Object.keys(SummaryView).map(
        (key) =>
          html`<button key="${key}" @click="${this.switchView}">
            ${SummaryView[key as keyof typeof SummaryView]}
          </button>`
      )}
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'period-buttons': PeriodButtons;
  }
}
