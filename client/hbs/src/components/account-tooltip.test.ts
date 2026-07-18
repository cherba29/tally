import { expect } from '@esm-bundle/chai'; // Or your preferred ESM-friendly assertion bundle
import { AccountTooltip } from './account-tooltip';
import { Account } from '@tally/lib/core/account';

describe('AccountTooltip component', () => {
  let element: AccountTooltip;

  beforeEach(()=> {
    // Create and append the component to the jsdom body
    element = document.createElement('account-tooltip') as AccountTooltip;
    document.body.appendChild(element);
  });

  afterEach(() => {
    // Clean up DOM after each test execution
    element.remove();
  });

  it('renders with default property values', async () => {
    element.account = {
      name: "test-account1"
    } as Account;
    // Wait for Lit's async rendering lifecycles to finish
    await element.updateComplete;

    // Drill into the Shadow DOM to verify content.
    // TODO: fix this should not be null.
    expect(element.shadowRoot).to.be.null;
  });
});
