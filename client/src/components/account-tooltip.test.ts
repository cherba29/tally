import { html, fixture, expect } from '@open-wc/testing';
import { LitElement } from 'lit';
import { AccountTooltip } from './account-tooltip';
import { type GqlAccount } from '../gql_types';

describe('AccountTooltip component', () => {
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
});
