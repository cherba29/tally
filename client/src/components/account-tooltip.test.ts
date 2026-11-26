import { html, fixture, expect, oneEvent } from '@open-wc/testing';
import { LitElement } from 'lit';
import { AccountTooltip } from './account-tooltip';
import { type GqlAccount } from '../gql_types';

describe('AccountTooltip component', () => {
  it('renders nothing when account is undefined', async () => {
    const element = await fixture<AccountTooltip>(
      html`<account-tooltip></account-tooltip>`
    );
    expect(element.shadowRoot!.querySelector('table')).to.equal(null);
  });

  it('renders with default property values', async () => {
    const account = {
       name: "test-account1"
    } as GqlAccount;

    const element = await fixture<AccountTooltip>(
      html`<account-tooltip .account=${account}></account-tooltip>`
    );

    // Drill into the Shadow DOM to verify content.
    expect(element.shadowRoot).not.to.be.null;
    const accountNameCellValue = element.shadowRoot!.querySelector('table td:nth-child(2)')!.textContent
    expect(accountNameCellValue.trim()).to.equal("test-account1")
  });

  it('updates rendered content when account property is changed directly', async () => {
    const account1 = { name: "test-account1" } as GqlAccount;
    const account2 = { name: "test-account2" } as GqlAccount;

    const element = await fixture<AccountTooltip>(
      html`<account-tooltip .account=${account1}></account-tooltip>`
    );
    expect(element.shadowRoot!.querySelector('table td:nth-child(2)')!.textContent!.trim()).to.equal("test-account1");

    element.account = account2;
    await element.updateComplete;

    expect(element.shadowRoot!.querySelector('table td:nth-child(2)')!.textContent!.trim()).to.equal("test-account2");
  });

  it('updates when parent LitElement re-renders with new account', async () => {
    const account1 = { name: "test-account1" } as GqlAccount;
    const account2 = { name: "test-account2" } as GqlAccount;

    class TestParent extends LitElement {
      account: GqlAccount | undefined = undefined;
      override render() {
        return html`<div><account-tooltip .account=${this.account}></account-tooltip></div>`;
      }
    }
    customElements.define('test-parent', TestParent);

    const parent = await fixture<TestParent>(html`<test-parent .account=${account1}></test-parent>`);
    const tooltip = parent.shadowRoot!.querySelector('account-tooltip')!;
    expect(tooltip.shadowRoot!.querySelector('table td:nth-child(2)')!.textContent!.trim()).to.equal("test-account1");

    parent.account = account2;
    parent.requestUpdate();
    await parent.updateComplete;
    await tooltip.updateComplete;

    expect(tooltip.shadowRoot!.querySelector('table td:nth-child(2)')!.textContent!.trim()).to.equal("test-account2");
  });

  it('renders account link, metadata fields, and dispatches close on close button click', async () => {
    const account = {
      name: 'checking',
      url: 'https://example.com/checking',
      description: 'Primary Checking',
      number: '****1234',
      userName: 'alex',
      password: 'secret',
      openedOn: 'Jul2026',
      phone: '555-0100',
      address: '123 Main St',
      statementCloseDay: '1',
    } as GqlAccount;

    const element = await fixture<AccountTooltip>(
      html`<account-tooltip .account=${account}></account-tooltip>`
    );

    const link = element.shadowRoot!.querySelector('a')!;
    expect(link).to.not.equal(null);
    expect(link.getAttribute('href')).to.equal('https://example.com/checking');
    expect(link.textContent).to.equal('checking');

    const values = Array.from(element.shadowRoot!.querySelectorAll('table tr td:nth-child(2)')).map(
      (td) => td.textContent!.trim()
    );
    expect(values).to.deep.equal([
      'checking',
      'Primary Checking',
      '****1234',
      'alex',
      'secret',
      'Jul2026',
      '555-0100',
      '123 Main St',
      '1',
    ]);

    const closeBtn = element.shadowRoot!.querySelector<HTMLButtonElement>('button.close-btn')!;
    setTimeout(() => closeBtn.click());
    const closeEvent = await oneEvent(element, 'close');
    expect(closeEvent.type).to.equal('close');
  });
});
