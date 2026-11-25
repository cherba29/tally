import { assert } from '@esm-bundle/chai';
import { html, fixture, expect, oneEvent } from '@open-wc/testing';
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

  it('renders collapsed icon when expanded is false', async () => {
    const element = await fixture<ExpandButton>(
      html`<expand-button .expanded=${false}></expand-button>`
    );
    assert.shadowDom.equal(
      element,
      `<button class="collapsible-button">⯈</button>`
    );
  });

  it('toggles state, updates icon, and dispatches toggle event on click', async () => {
    const element = await fixture<ExpandButton>(
      html`<expand-button></expand-button>`
    );
    const button = element.shadowRoot!.querySelector('button')!;

    setTimeout(() => button.click());
    const collapseEvent = await oneEvent(element, 'toggle');
    expect(collapseEvent.detail.expanded).to.equal(false);
    expect(collapseEvent.bubbles).to.equal(true);
    expect(collapseEvent.composed).to.equal(true);
    await element.updateComplete;
    assert.shadowDom.equal(
      element,
      `<button class="collapsible-button">⯈</button>`
    );

    setTimeout(() => button.click());
    const expandEvent = await oneEvent(element, 'toggle');
    expect(expandEvent.detail.expanded).to.equal(true);
    await element.updateComplete;
    assert.shadowDom.equal(
      element,
      `<button class="collapsible-button">⯆</button>`
    );
  });
});
