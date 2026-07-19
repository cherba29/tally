import { assert } from '@esm-bundle/chai';
import { html, fixture, expect } from '@open-wc/testing';
import { ExpandButton } from './expand-button';

describe('ExpandButton component', () => {
  it('is defined', () => {
    const el = document.createElement('expand-button');
    assert.instanceOf(el, ExpandButton);
  });

  it('renders with default property values', async () => {
    const toggle = (e: CustomEvent) => { console.log(e); };

    const element = await fixture<ExpandButton>(
      html`<expand-button @toggle="${toggle}"></expand-button>`
    );

    // Drill into the Shadow DOM to verify content.
    expect(element.shadowRoot).not.to.be.null;
    assert.shadowDom.equal(
      element, 
      `<button class="collapsible-button">⯆</button>`
    );
  });
});
