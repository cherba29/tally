import { assert } from '@esm-bundle/chai';
import { html, fixture, expect, oneEvent } from '@open-wc/testing';
import { type CellClickEventData, SummaryTable } from './summary-table';
import { type GqlTableRow } from '../gql_types'

describe('SummaryTable component', () => {
  it('is defined', () => {
    const el = document.createElement('summary-table');
    assert.instanceOf(el, SummaryTable);
  });

  it('renders with default property values', async () => {
    const onCellClick = (e: CustomEvent) => { console.log(e); };
    const months: string[] = [];
    const rows: GqlTableRow[] = [];

    const element = await fixture<SummaryTable>(
      html`<summary-table
        .months=${months}
        .rows=${rows}
        @cellclick=${onCellClick}
      ></summary-table>`
    );

    // Drill into the Shadow DOM to verify content.
    expect(element.shadowRoot).not.to.be.null;
    assert.shadowDom.equal(
      element, 
      `<table>
        <thead>
          <tr><th>Account</th></tr>
          <tr><th></th></tr>
        </thead>
        <tbody>
        </tbody>
      </table>`
    );
  });

  it('renders over multiple months', async () => {
    const onCellClick = (e: CustomEvent) => { console.log(e); };
    const months: string[] = ['Aug2026', 'Jul2026'];
    const rows: GqlTableRow[] = [];

    const element = await fixture<SummaryTable>(
      html`<summary-table
        .months=${months}
        .rows=${rows}
        @cellclick=${onCellClick}
      ></summary-table>`
    );

    // Drill into the Shadow DOM to verify content.
    expect(element.shadowRoot).not.to.be.null;
    assert.shadowDom.equal(
      element, 
      `<table>
        <thead>
          <tr>
            <th>Account</th>
            <th colspan="4">Aug2026</th>
            <th colspan="4">Jul2026</th>
          </tr>
          <tr>
            <th></th>
            <th>+/-</th>
            <th>$Bal</th>
            <th>Chg%</th>
            <th>?</th>
            <th>+/-</th>
            <th>$Bal</th>
            <th>Chg%</th>
            <th>?</th>
          </tr>
        </thead>
        <tbody>
        </tbody>
      </table>`
    );
  });

  it('renders over multiple months and rows', async () => {
    const months: string[] = ['Aug2026', 'Jul2026'];
    const rows: GqlTableRow[] = [
      {
        account: {
          __typename: undefined,
          address: '',
          closedOn: undefined,
          description: '',
          external: false,
          name: 'internal/test-account1',
          number: undefined,
          openedOn: undefined,
          owners: [],
          password: '',
          path: [],
          phone: '',
          summary: false,
          url: '',
          userName: ''
        },
        cells: [{
          addSub: undefined,
          annualizedPercentChange: 0,
          balanced: false,
          hasProjectedTransfer: false,
          isClosed: false,
          isCovered: false,
          isProjected: false,
          isProjectedCovered: false,
          month: 'Aug2026',
          percentChange: 0
        }, {
          addSub: undefined,
          annualizedPercentChange: 0,
          balanced: false,
          hasProjectedTransfer: false,
          isClosed: false,
          isCovered: false,
          isProjected: false,
          isProjectedCovered: false,
          month: 'Jul2026',
          percentChange: 0
        }],
        id: 'internal',
        indent: 0,
        isInactive: false,
        isNormal: true,
        isSpace: false,
        isTotal: true,
        title: 'internal'
      },
      {
        account: {
          __typename: undefined,
          address: '',
          closedOn: undefined,
          description: '',
          external: false,
          name: 'test-account1',
          number: undefined,
          openedOn: undefined,
          owners: [],
          password: '',
          path: ['internal'],
          phone: '',
          summary: false,
          url: '',
          userName: ''
        },
        cells: [{
          addSub: undefined,
          annualizedPercentChange: 0,
          balanced: false,
          hasProjectedTransfer: false,
          isClosed: false,
          isCovered: false,
          isProjected: false,
          isProjectedCovered: false,
          month: undefined,
          percentChange: 0
        }, {
          addSub: undefined,
          annualizedPercentChange: 0,
          balanced: false,
          hasProjectedTransfer: false,
          isClosed: false,
          isCovered: false,
          isProjected: false,
          isProjectedCovered: false,
          month: undefined,
          percentChange: 0
        }],
        id: 'internal/test-account1',
        indent: 0,
        isInactive: false,
        isNormal: true,
        isSpace: false,
        isTotal: false,
        title: 'test-account1'
      }
    ];
    let cellClickedData: CellClickEventData | null = null;
    const onCellClick = (e: CustomEvent<CellClickEventData>) => { 
      cellClickedData = e.detail;
     };

    const element = await fixture<SummaryTable>(
      html`<summary-table
        .months=${months}
        .rows=${rows}
        @cellclick=${onCellClick}
      ></summary-table>`
    );

    // Drill into the Shadow DOM to verify content.
    expect(element.shadowRoot).not.to.be.null;
    assert.shadowDom.equal(
      element, 
      `<table>
        <thead>
          <tr>
            <th>Account</th>
            <th colspan="4">Aug2026</th>
            <th colspan="4">Jul2026</th>
          </tr>
          <tr>
            <th></th>
            <th>+/-</th>
            <th>$Bal</th>
            <th>Chg%</th>
            <th>?</th>
            <th>+/-</th>
            <th>$Bal</th>
            <th>Chg%</th>
            <th>?</th>
          </tr>
        </thead>
        <tbody>
          <tr bgcolor="#ffd" id="row-0">
            <td><expand-button></expand-button><b>internal</b></td>
            <td class="add_sub">—</td>
            <td class="balance" style="font-weight:700;font-size:75%;">—</td>
            <td class="change">0</td>
            <td class="unaccounted" style="border-right:2px double #a00">—</td>
            <td class="add_sub">—</td>
            <td class="balance" style="font-weight:700;font-size:75%;">—</td>
            <td class="change">0</td>
            <td class="unaccounted" style="border-right:2px double #a00">—</td>
          </tr>
          <tr id="row-1">
            <td id="test-account1">test-account1</td>
            <td class="add_sub">—</td>
            <td class="balance" style="color:#f00;font-weight:700;">—</td>
            <td class="change">0</td>
            <td class="unaccounted" style="border-right:2px double #a00">—</td>
            <td class="add_sub">—</td>
            <td class="balance" style="color:#f00;font-weight:700;">—</td>
            <td class="change">0</td>
            <td class="unaccounted" style="border-right:2px double #a00">—</td>
          </tr>
        </tbody>
      </table>`
    );
    const balanceCell = element.shadowRoot!.querySelector('td.balance') as HTMLElement;
    setTimeout(() => {
      balanceCell.click();
    });
    const { detail } = await oneEvent(element, 'cellclick');
    expect(cellClickedData!.rowId).to.equal('internal');
    expect(detail.rowId).to.equal('internal');
    expect(detail.month.toString()).to.equal('Aug2026');
    expect(detail.isSummary).to.be.true;
  });

  it('handles space/closed/projected/covered/url/inactive rows, click events, clipboard copy, and nested row folding', async () => {
    const months = ['Aug2026', 'Jul2026'];
    const baseAccount = (name: string, url = '') => ({
      address: '',
      closedOn: undefined,
      description: '',
      external: false,
      name,
      number: undefined,
      openedOn: undefined,
      owners: [],
      password: '',
      path: ['internal'],
      phone: '',
      summary: false,
      url,
      userName: '',
    });

    const rows: GqlTableRow[] = [
      {
        id: 'root',
        title: 'root',
        indent: 0,
        isSpace: false,
        isTotal: true,
        isNormal: false,
        isInactive: false,
        account: null,
        cells: [
          {
            month: 'Aug2026',
            isClosed: true,
            addSub: 0,
            balance: 0,
            isProjected: false,
            isCovered: true,
            isProjectedCovered: true,
            hasProjectedTransfer: false,
            percentChange: 0,
            annualizedPercentChange: 0,
            unaccounted: 0,
            balanced: true,
          },
          {
            month: 'Jul2026',
            isClosed: false,
            addSub: 10000,
            balance: 50000,
            isProjected: true,
            isCovered: true,
            isProjectedCovered: true,
            hasProjectedTransfer: false,
            percentChange: 10,
            annualizedPercentChange: 0,
            unaccounted: 250,
            balanced: false,
          },
        ],
      },
      {
        id: 'root/sub',
        title: 'sub',
        indent: 1,
        isSpace: false,
        isTotal: true,
        isNormal: false,
        isInactive: false,
        account: null,
        cells: [
          {
            month: 'Aug2026',
            isClosed: false,
            addSub: 5000,
            balance: 50000,
            isProjected: false,
            isCovered: true,
            isProjectedCovered: true,
            hasProjectedTransfer: false,
            percentChange: 5,
            annualizedPercentChange: 0,
            unaccounted: 0,
            balanced: true,
          },
          {
            month: 'Jul2026',
            isClosed: false,
            addSub: 5000,
            balance: 45000,
            isProjected: false,
            isCovered: true,
            isProjectedCovered: true,
            hasProjectedTransfer: false,
            percentChange: 0,
            annualizedPercentChange: 0,
            unaccounted: 0,
            balanced: true,
          },
        ],
      },
      {
        id: 'root/sub/checking',
        title: 'checking',
        indent: 2,
        isSpace: false,
        isTotal: false,
        isNormal: true,
        isInactive: false,
        account: baseAccount('checking', 'https://example.com/checking'),
        cells: [
          {
            month: 'Aug2026',
            isClosed: false,
            addSub: 1500,
            balance: -67000,
            isProjected: true,
            isCovered: false,
            isProjectedCovered: true,
            hasProjectedTransfer: true,
            percentChange: undefined as unknown as number,
            annualizedPercentChange: 0,
            unaccounted: 0,
            balanced: true,
          },
          {
            month: 'Jul2026',
            isClosed: true,
            addSub: 0,
            balance: 0,
            isProjected: false,
            isCovered: true,
            isProjectedCovered: true,
            hasProjectedTransfer: false,
            percentChange: 0,
            annualizedPercentChange: 0,
            unaccounted: 0,
            balanced: true,
          },
        ],
      },
      {
        id: 'root/sub/covered',
        title: 'covered',
        indent: 2,
        isSpace: false,
        isTotal: false,
        isNormal: true,
        isInactive: true,
        account: baseAccount('covered'),
        cells: [
          {
            month: 'Aug2026',
            isClosed: false,
            addSub: 0,
            balance: 10000,
            isProjected: false,
            isCovered: true,
            isProjectedCovered: true,
            hasProjectedTransfer: false,
            percentChange: 1.5,
            annualizedPercentChange: 0,
            unaccounted: 0,
            balanced: true,
          },
          {
            month: 'Jul2026',
            isClosed: false,
            addSub: 0,
            balance: 10000,
            isProjected: false,
            isCovered: true,
            isProjectedCovered: true,
            hasProjectedTransfer: false,
            percentChange: 0,
            annualizedPercentChange: 0,
            unaccounted: 0,
            balanced: true,
          },
        ],
      },
      {
        id: 'space-row',
        title: 'Section',
        indent: 0,
        isSpace: true,
        isTotal: false,
        isNormal: false,
        isInactive: false,
        account: null,
        cells: [],
      },
      {
        id: 'empty-row',
        title: 'Ignored',
        indent: 0,
        isSpace: false,
        isTotal: false,
        isNormal: false,
        isInactive: false,
        account: null,
        cells: [],
      },
    ];

    const element = await fixture<SummaryTable>(
      html`<summary-table .months=${months} .rows=${rows}></summary-table>`
    );

    const row0 = element.shadowRoot!.getElementById('row-0')!;
    const row1 = element.shadowRoot!.getElementById('row-1')!;
    const row2 = element.shadowRoot!.getElementById('row-2')!;
    const row3 = element.shadowRoot!.getElementById('row-3')!;
    const row4 = element.shadowRoot!.getElementById('row-4')!;

    // Space row, closed cells, link, projected/covered styles, infinity change, hidden inactive row
    expect(row0.querySelector('td.closed')).to.not.equal(null);
    expect(row2.querySelector('a')!.getAttribute('href')).to.equal('https://example.com/checking');
    expect(row2.querySelector('td.closed')).to.not.equal(null);
    const row2Balance = row2.querySelector('td.balance') as HTMLElement;
    expect(row2Balance.classList.contains('projected')).to.equal(true);
    expect(row2Balance.textContent).to.include('☂');
    expect(row2Balance.style.color).to.equal('rgb(255, 170, 0)');
    expect(row2.querySelector('td.change')!.textContent!.trim()).to.equal('∞');
    expect(row3.hasAttribute('hidden')).to.equal(true);
    expect((row3.querySelector('td.balance') as HTMLElement).style.color).to.equal('');
    expect(row4.querySelector('td.account_type')!.textContent).to.equal('Section');
    expect(element.shadowRoot!.getElementById('row-5')).to.equal(null);

    // Click add_sub on total row and normal row
    const totalAddSub = row0.querySelector('td.add_sub') as HTMLElement;
    setTimeout(() => totalAddSub.click());
    const totalAddSubEvent = await oneEvent(element, 'addSubCellClick');
    expect(totalAddSubEvent.detail.rowId).to.equal('root');
    expect(totalAddSubEvent.detail.month.toString()).to.equal('Jul2026');
    expect(totalAddSubEvent.detail.isSummary).to.equal(true);

    const normalAddSub = row2.querySelector('td.add_sub') as HTMLElement;
    setTimeout(() => normalAddSub.click());
    const normalAddSubEvent = await oneEvent(element, 'addSubCellClick');
    expect(normalAddSubEvent.detail.rowId).to.equal('root/sub/checking');
    expect(normalAddSubEvent.detail.isSummary).to.equal(false);

    // Click balance cell on normal row
    setTimeout(() => row2Balance.click());
    const normalBalEvent = await oneEvent(element, 'cellclick');
    expect(normalBalEvent.detail.rowId).to.equal('root/sub/checking');
    expect(normalBalEvent.detail.month.toString()).to.equal('Aug2026');
    expect(normalBalEvent.detail.isSummary).to.equal(false);

    // Click title cell on normal row
    const titleCell = element.shadowRoot!.getElementById('checking')!;
    setTimeout(() => titleCell.click());
    const titleEvent = await oneEvent(element, 'cellclick');
    expect(titleEvent.detail.rowId).to.equal('root/sub/checking');
    expect(titleEvent.detail.account?.name).to.equal('checking');
    expect(titleEvent.detail.month).to.equal(undefined);

    // Click unaccounted/accounted cells to copy to clipboard
    const copied: string[] = [];
    const origClipboardDesc = Object.getOwnPropertyDescriptor(navigator, 'clipboard');
    Object.defineProperty(navigator, 'clipboard', {
      value: {
        writeText: (text: string) => {
          copied.push(text);
          return Promise.resolve();
        },
      },
      configurable: true,
    });
    try {
      (row0.querySelector('td.unaccounted') as HTMLElement).click();
      (row2.querySelector('td.accounted') as HTMLElement).click();
      expect(copied).to.deep.equal(['2.50', '0.00']);

      element.copyToClipBoard(null);
      element.copyToClipBoard(undefined);
      expect(copied).to.deep.equal(['2.50', '0.00']);
    } finally {
      if (origClipboardDesc) {
        Object.defineProperty(navigator, 'clipboard', origClipboardDesc);
      }
    }

    // Nested row folding via expand-button toggle events
    const row0Btn = row0.querySelector('expand-button')!;
    const row1Btn = row1.querySelector('expand-button')!;

    row0Btn.dispatchEvent(new CustomEvent('toggle', {detail: {expanded: false}}));
    await element.updateComplete;
    expect(row1.hidden).to.equal(true);
    expect(row2.hidden).to.equal(true);

    row0Btn.dispatchEvent(new CustomEvent('toggle', {detail: {expanded: true}}));
    await element.updateComplete;
    expect(row1.hidden).to.equal(false);
    expect(row2.hidden).to.equal(false);

    // Collapse child (row1), then collapse & re-expand parent (row0) -> grandchild stays hidden
    row1Btn.dispatchEvent(new CustomEvent('toggle', {detail: {expanded: false}}));
    await element.updateComplete;
    expect(row2.hidden).to.equal(true);
    row0Btn.dispatchEvent(new CustomEvent('toggle', {detail: {expanded: false}}));
    await element.updateComplete;
    expect(row1.hidden).to.equal(true);
    expect(row2.hidden).to.equal(true);
    row0Btn.dispatchEvent(new CustomEvent('toggle', {detail: {expanded: true}}));
    await element.updateComplete;
    expect(row1.hidden).to.equal(false);
    expect(row2.hidden).to.equal(true);

    // Live reload with fresh rows preserves collapsed state of row1
    element.rows = rows.map((r) => ({...r}));
    await element.updateComplete;
    expect(element.shadowRoot!.getElementById('row-1')!.hidden).to.equal(false);
    expect(element.shadowRoot!.getElementById('row-2')!.hidden).to.equal(true);

    // Expanding row1 reveals row2 (and inactive child row3 when explicitly expanded)
    row1Btn.dispatchEvent(new CustomEvent('toggle', {detail: {expanded: true}}));
    await element.updateComplete;
    expect(element.shadowRoot!.getElementById('row-2')!.hidden).to.equal(false);
    expect(element.shadowRoot!.getElementById('row-3')!.hidden).to.equal(false);
  });

  it('starts inactive summary groups collapsed and expands them on toggle', async () => {
    const months = ['Aug2026'];
    const rows: GqlTableRow[] = [
      {
        id: 'external/inactive',
        title: 'inactive',
        indent: 0,
        isSpace: false,
        isTotal: true,
        isNormal: false,
        isInactive: true,
        account: null,
        cells: [],
      },
      {
        id: 'external/inactive/old-acct',
        title: 'old-acct',
        indent: 1,
        isSpace: false,
        isTotal: false,
        isNormal: true,
        isInactive: true,
        account: {
          address: '',
          closedOn: undefined,
          description: '',
          external: true,
          name: 'old-acct',
          number: undefined,
          openedOn: undefined,
          owners: [],
          password: '',
          path: ['external', 'inactive'],
          phone: '',
          summary: false,
          url: '',
          userName: '',
        },
        cells: [],
      },
    ];

    const element = await fixture<SummaryTable>(
      html`<summary-table .months=${months} .rows=${rows}></summary-table>`
    );

    const groupRow = element.shadowRoot!.getElementById('row-0')!;
    const childRow = element.shadowRoot!.getElementById('row-1')!;
    const btn = groupRow.querySelector('expand-button')!;
    expect(btn.expanded).to.equal(false);
    expect(childRow.hidden).to.equal(true);

    btn.dispatchEvent(new CustomEvent('toggle', {detail: {expanded: true}}));
    await element.updateComplete;
    expect(btn.expanded).to.equal(true);
    expect(childRow.hidden).to.equal(false);

    // Sticky header and first column styles
    const thead = element.shadowRoot!.querySelector('thead')!;
    const firstHeader = element.shadowRoot!.querySelector('thead th:first-child')!;
    const totalFirstCell = groupRow.querySelector('td:first-child')!;
    const normalFirstCell = childRow.querySelector('td:first-child')!;
    expect(getComputedStyle(thead).position).to.equal('sticky');
    expect(getComputedStyle(thead).top).to.equal('0px');
    expect(getComputedStyle(firstHeader).position).to.equal('sticky');
    expect(getComputedStyle(firstHeader).left).to.equal('0px');
    expect(getComputedStyle(totalFirstCell).position).to.equal('sticky');
    expect(getComputedStyle(totalFirstCell).left).to.equal('0px');
    expect(getComputedStyle(totalFirstCell).backgroundColor).to.equal('rgb(255, 255, 221)');
    expect(getComputedStyle(normalFirstCell).position).to.equal('sticky');
    expect(getComputedStyle(normalFirstCell).backgroundColor).to.equal('rgb(255, 255, 255)');
  });

  it('handles optional indent/cells and total-row rowId fallbacks on click', async () => {
    const months = ['Aug2026'];
    const makeCell = () => ({
      month: 'Aug2026',
      isClosed: false,
      addSub: 100,
      balance: 200,
      isProjected: false,
      isCovered: true,
      isProjectedCovered: true,
      hasProjectedTransfer: false,
      percentChange: 0,
      annualizedPercentChange: 0,
      unaccounted: 0,
      balanced: true,
    });
    const baseAccount = (name: string) => ({
      address: '',
      closedOn: undefined,
      description: '',
      external: false,
      name,
      number: undefined,
      openedOn: undefined,
      owners: [],
      password: '',
      path: [],
      phone: '',
      summary: false,
      url: '',
      userName: '',
    });

    const rows = [
      {
        id: undefined,
        title: 'fallback-title',
        indent: undefined,
        isSpace: false,
        isTotal: true,
        isNormal: false,
        isInactive: false,
        account: baseAccount('acct-fallback'),
        cells: [makeCell()],
      },
      {
        id: undefined,
        title: 'title-only',
        indent: 0,
        isSpace: false,
        isTotal: true,
        isNormal: false,
        isInactive: false,
        account: null,
        cells: [makeCell()],
      },
      {
        id: 'total-no-cells',
        title: 'total-no-cells',
        indent: undefined,
        isSpace: false,
        isTotal: true,
        isNormal: false,
        isInactive: false,
        account: null,
        cells: undefined,
      },
      {
        id: 'normal-no-cells',
        title: 'normal-no-cells',
        indent: undefined,
        isSpace: false,
        isTotal: false,
        isNormal: true,
        isInactive: false,
        account: baseAccount('normal-no-cells'),
        cells: undefined,
      },
    ] as unknown as GqlTableRow[];

    const element = await fixture<SummaryTable>(
      html`<summary-table .months=${months} .rows=${rows}></summary-table>`
    );

    const row0 = element.shadowRoot!.getElementById('row-0')!;
    const row1 = element.shadowRoot!.getElementById('row-1')!;

    // Row 0 falls back to account.name
    setTimeout(() => (row0.querySelector('td.add_sub') as HTMLElement).click());
    const ev0AddSub = await oneEvent(element, 'addSubCellClick');
    expect(ev0AddSub.detail.rowId).to.equal('acct-fallback');

    setTimeout(() => (row0.querySelector('td.balance') as HTMLElement).click());
    const ev0Bal = await oneEvent(element, 'cellclick');
    expect(ev0Bal.detail.rowId).to.equal('acct-fallback');

    // Row 1 falls back to row.title
    setTimeout(() => (row1.querySelector('td.add_sub') as HTMLElement).click());
    const ev1AddSub = await oneEvent(element, 'addSubCellClick');
    expect(ev1AddSub.detail.rowId).to.equal('title-only');

    setTimeout(() => (row1.querySelector('td.balance') as HTMLElement).click());
    const ev1Bal = await oneEvent(element, 'cellclick');
    expect(ev1Bal.detail.rowId).to.equal('title-only');
  });
});
