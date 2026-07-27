import { assert } from '@esm-bundle/chai';
import { html, fixture, expect, oneEvent } from '@open-wc/testing';
import { PeriodButtons } from './period-buttons';
import { Month } from '@tally/lib/core/month';

describe('PeriodButtons component', () => {
  it('renders with default property values', async () => {
    const onButtonClick = (_: CustomEvent) => { };
    const currentMonth = new Month(2026, 6);
    const element = await fixture<PeriodButtons>(
      html`<period-buttons .currentMonth=${currentMonth} @button-click=${onButtonClick}></period-buttons>`
    );

    // Drill into the Shadow DOM to verify content.
    expect(element.shadowRoot).not.to.be.null;
    assert.shadowDom.equal(
      element, 
      `<button key="MONTH_1">1m</button>
      <button key="MONTH_3">3m</button>
      <button key="MONTH_6">6m</button>
      <button key="MONTH_9">9m</button>
      <button key="YTD">ytd</button>
      <button key="YEAR_1">1y</button>
      <button key="YEAR_2">2y</button>
      <button key="YEAR_3">3y</button>
      <button key="YEAR_5">5y</button>
      <button key="YEAR_7">7y</button>
      <button key="YEAR_10">10y</button>
      <button key="YEAR_15">15y</button>
      <button key="YEAR_20">20y</button>
      <button key="YEAR_30">30y</button>
      <button key="YEAR_MAX">Max</button>
      <span>(1m)</span>`
    );
  
      const button = element.shadowRoot!.querySelector('[key="MONTH_9"]') as HTMLElement;
      setTimeout(() => {
        button.click();
      });
      const { detail } = await oneEvent(element, 'button-click');
      // 9 months back from Jul2026 is Nov2025.
      expect(detail.startMonth.toString()).to.equal('Nov2025');
      expect(detail.endMonth.toString()).to.equal('Jul2026');
      const span = element.shadowRoot!.querySelector('span') as HTMLElement;
      expect(span.textContent).to.equal('(9m)');
    });
});
