import { assert } from '@esm-bundle/chai';
import { html, fixture, expect, oneEvent } from '@open-wc/testing';
import { PeriodButtons } from './period-buttons';

describe('PeriodButtons component', () => {
  it('renders with default property values', async () => {
    const onButtonClick = (_: CustomEvent) => { };
    const element = await fixture<PeriodButtons>(
      html`<period-buttons @button-click=${onButtonClick}></period-buttons>`
    );

    // Drill into the Shadow DOM to verify content.
    expect(element.shadowRoot).not.to.be.null;
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
      <button key="YEAR_MAX">Max</button>`);
  
      const button = element.shadowRoot!.querySelector('[key="MONTH_9"]') as HTMLElement;
      setTimeout(() => {
        button.click();
      });
      const { detail } = await oneEvent(element, 'button-click');
      expect(detail.period).to.equal('9m');
    });
});
