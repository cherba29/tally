import { assert } from '@esm-bundle/chai';
import { html, fixture, expect, oneEvent } from '@open-wc/testing';
import { mapViewToMonths, PeriodButtons, SummaryView } from './period-buttons';
import { Month } from '../month';

describe('PeriodButtons component', () => {
  it('maps every SummaryView enum value to expected month count', () => {
    const jul2026 = new Month(2026, 6);
    const jan2026 = new Month(2026, 0);

    expect(mapViewToMonths(SummaryView.MONTH_1, jul2026)).to.equal(1);
    expect(mapViewToMonths(SummaryView.MONTH_3, jul2026)).to.equal(3);
    expect(mapViewToMonths(SummaryView.MONTH_6, jul2026)).to.equal(6);
    expect(mapViewToMonths(SummaryView.MONTH_9, jul2026)).to.equal(9);
    expect(mapViewToMonths(SummaryView.YTD, jul2026)).to.equal(7);
    expect(mapViewToMonths(SummaryView.YTD, jan2026)).to.equal(1);
    expect(mapViewToMonths(SummaryView.YEAR_1, jul2026)).to.equal(12);
    expect(mapViewToMonths(SummaryView.YEAR_2, jul2026)).to.equal(24);
    expect(mapViewToMonths(SummaryView.YEAR_3, jul2026)).to.equal(36);
    expect(mapViewToMonths(SummaryView.YEAR_5, jul2026)).to.equal(60);
    expect(mapViewToMonths(SummaryView.YEAR_7, jul2026)).to.equal(84);
    expect(mapViewToMonths(SummaryView.YEAR_10, jul2026)).to.equal(120);
    expect(mapViewToMonths(SummaryView.YEAR_15, jul2026)).to.equal(180);
    expect(mapViewToMonths(SummaryView.YEAR_20, jul2026)).to.equal(240);
    expect(mapViewToMonths(SummaryView.YEAR_30, jul2026)).to.equal(360);
    expect(mapViewToMonths(SummaryView.YEAR_MAX, jul2026)).to.equal(undefined);
  });

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

  it('formats exact-year, mixed year+month, and Max periods and preserves period on currentMonth update', async () => {
    const currentMonth = new Month(2026, 6);
    const element = await fixture<PeriodButtons>(
      html`<period-buttons .currentMonth=${currentMonth}></period-buttons>`
    );
    const span = element.shadowRoot!.querySelector('span') as HTMLElement;

    // 1y (exact years, 0 remaining months)
    const year1Btn = element.shadowRoot!.querySelector('[key="YEAR_1"]') as HTMLElement;
    setTimeout(() => year1Btn.click());
    const year1Event = await oneEvent(element, 'button-click');
    expect(year1Event.detail.startMonth.toString()).to.equal('Aug2025');
    expect(year1Event.detail.endMonth.toString()).to.equal('Jul2026');
    await element.updateComplete;
    expect(span.textContent).to.equal('(1y)');

    // Updating currentMonth after period is already set preserves existing period
    element.currentMonth = new Month(2026, 7);
    await element.updateComplete;
    expect(span.textContent).to.equal('(1y)');

    // Mixed years and months (15m = 1y3m)
    element.updatePeriod(15);
    await element.updateComplete;
    expect(span.textContent).to.equal('(1y3m)');

    // YEAR_MAX (undefined months -> falls back to currentMonth / 1m)
    const maxBtn = element.shadowRoot!.querySelector('[key="YEAR_MAX"]') as HTMLElement;
    setTimeout(() => maxBtn.click());
    const maxEvent = await oneEvent(element, 'button-click');
    expect(maxEvent.detail.startMonth.toString()).to.equal('Aug2026');
    expect(maxEvent.detail.endMonth.toString()).to.equal('Aug2026');
    await element.updateComplete;
    expect(span.textContent).to.equal('(1m)');
  });
});
