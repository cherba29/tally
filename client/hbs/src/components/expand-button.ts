import { html, css, LitElement, CSSResultGroup } from 'lit';
import { customElement, property } from 'lit/decorators.js';

@customElement('expand-button')
export class ExpandButton extends LitElement {
  static override styles?: CSSResultGroup | undefined = css`
    .collapsible-button {
      cursor: pointer;
      border: none;
      text-align: left;
      outline: none;
      font-size: 10px;
    }
  `
  @property() expanded = true;

  toggle() {
    // console.log(`### expand button ${this.expanded}`)
    this.expanded = !this.expanded;
    const event = new CustomEvent('toggle', {
      detail: {expanded: this.expanded},
      bubbles: true, composed: true});
    this.dispatchEvent(event);
    this.requestUpdate('expanded', !this.expanded);
  }

  override render() {
    return html`<button
      class="collapsible-button"
      @click="${this.toggle}"
    >${this.expanded ? html`&#11206;` : html`&#11208;`}</button>`;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'expand-button': ExpandButton;
  }
}
