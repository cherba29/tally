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

  it('renders with default pressed 1m button and no text status span', async () => {
    const onButtonClick = (_: CustomEvent) => {};
    const currentMonth = new Month(2026, 6);
    const element = await fixture<PeriodButtons>(
      html`<period-buttons .currentMonth=${currentMonth} @button-click=${onButtonClick}></period-buttons>`
    );

    expect(element.shadowRoot).not.to.be.null;
    assert.shadowDom.equal(
      element,
      `<button aria-pressed="true" class="active" key="MONTH_1" type="button">1m</button>
      <button aria-pressed="false" key="MONTH_3" type="button">3m</button>
      <button aria-pressed="false" key="MONTH_6" type="button">6m</button>
      <button aria-pressed="false" key="MONTH_9" type="button">9m</button>
      <button aria-pressed="false" key="YTD" type="button">ytd</button>
      <button aria-pressed="false" key="YEAR_1" type="button">1y</button>
      <button aria-pressed="false" key="YEAR_2" type="button">2y</button>
      <button aria-pressed="false" key="YEAR_3" type="button">3y</button>
      <button aria-pressed="false" key="YEAR_5" type="button">5y</button>
      <button aria-pressed="false" key="YEAR_7" type="button">7y</button>
      <button aria-pressed="false" key="YEAR_10" type="button">10y</button>
      <button aria-pressed="false" key="YEAR_15" type="button">15y</button>
      <button aria-pressed="false" key="YEAR_20" type="button">20y</button>
      <button aria-pressed="false" key="YEAR_30" type="button">30y</button>
      <button aria-pressed="false" key="YEAR_MAX" type="button">Max</button>`
    );

    const month1Btn = element.shadowRoot!.querySelector<HTMLButtonElement>('[key="MONTH_1"]')!;
    const month9Btn = element.shadowRoot!.querySelector<HTMLButtonElement>('[key="MONTH_9"]')!;
    setTimeout(() => {
      month9Btn.click();
    });
    const {detail} = await oneEvent(element, 'button-click');
    // 9 months back from Jul2026 is Nov2025.
    expect(detail.startMonth.toString()).to.equal('Nov2025');
    expect(detail.endMonth.toString()).to.equal('Jul2026');
    await element.updateComplete;
    expect(month1Btn.classList.contains('active')).to.be.false;
    expect(month1Btn.getAttribute('aria-pressed')).to.equal('false');
    expect(month9Btn.classList.contains('active')).to.be.true;
    expect(month9Btn.getAttribute('aria-pressed')).to.equal('true');
    expect(element.shadowRoot!.querySelector('span')).to.be.null;
  });

  it('highlights pressed button across 1y, YTD, and Max and preserves selection on currentMonth update', async () => {
    const currentMonth = new Month(2026, 6);
    const element = await fixture<PeriodButtons>(
      html`<period-buttons .currentMonth=${currentMonth}></period-buttons>`
    );

    const getActiveKey = () =>
      element.shadowRoot!.querySelector<HTMLButtonElement>('button.active')?.getAttribute('key');

    expect(getActiveKey()).to.equal('MONTH_1');

    // 1y
    const year1Btn = element.shadowRoot!.querySelector<HTMLButtonElement>('[key="YEAR_1"]')!;
    setTimeout(() => year1Btn.click());
    const year1Event = await oneEvent(element, 'button-click');
    expect(year1Event.detail.startMonth.toString()).to.equal('Aug2025');
    expect(year1Event.detail.endMonth.toString()).to.equal('Jul2026');
    await element.updateComplete;
    expect(getActiveKey()).to.equal('YEAR_1');

    // Updating currentMonth preserves existing pressed button
    element.currentMonth = new Month(2026, 7);
    await element.updateComplete;
    expect(getActiveKey()).to.equal('YEAR_1');

    // YTD
    const ytdBtn = element.shadowRoot!.querySelector<HTMLButtonElement>('[key="YTD"]')!;
    setTimeout(() => ytdBtn.click());
    const ytdEvent = await oneEvent(element, 'button-click');
    expect(ytdEvent.detail.startMonth.toString()).to.equal('Jan2026');
    expect(ytdEvent.detail.endMonth.toString()).to.equal('Aug2026');
    await element.updateComplete;
    expect(getActiveKey()).to.equal('YTD');

    // YEAR_MAX highlights Max button
    const maxBtn = element.shadowRoot!.querySelector<HTMLButtonElement>('[key="YEAR_MAX"]')!;
    setTimeout(() => maxBtn.click());
    const maxEvent = await oneEvent(element, 'button-click');
    expect(maxEvent.detail.startMonth.toString()).to.equal('Aug2026');
    expect(maxEvent.detail.endMonth.toString()).to.equal('Aug2026');
    await element.updateComplete;
    expect(getActiveKey()).to.equal('YEAR_MAX');
    expect(element.shadowRoot!.querySelectorAll('button.active').length).to.equal(1);
  });
});
