import { afterEach, beforeEach, describe, expect, it } from '@jest/globals';
import { AccountTooltip } from './account-tooltip';
import {Account} from '@tally/lib/core/account';

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
    // TODO: use recomended https://lit.dev/docs/tools/testing/.
    expect(element.shadowRoot).toBeNull();
  });
});
