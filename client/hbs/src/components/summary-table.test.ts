import { assert } from '@esm-bundle/chai';
import { html, fixture, expect, oneEvent } from '@open-wc/testing';
import { SummaryTable } from './summary-table';
import { type GqlTableRow } from '../gql_types'

describe('SummaryTable component', () => {
  it('is defined', () => {
    const el = document.createElement('summary-table');
    assert.instanceOf(el, SummaryTable);
  });

  it('renders with default property values', async () => {
    const onCellClick = (e: CustomEvent) => { console.log(e); };
    const months: String[] = [];
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
    const months: String[] = ['Aug2026', 'Jul2026'];
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
    const onCellClick = (e: CustomEvent) => { console.log(e); };
    const months: String[] = ['Aug2026', 'Jul2026'];
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
        isNormal: true,
        isSpace: false,
        isTotal: false,
        title: 'test-account1'
      }
    ];

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
    expect(detail.accountName).to.equal('internal/test-account1');
    expect(detail.month).to.equal('Aug2026');
    expect(detail.isSummary).to.be.true;
  });
});
