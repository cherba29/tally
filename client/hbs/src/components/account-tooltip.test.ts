import { html, fixture, expect } from '@open-wc/testing';
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
});
