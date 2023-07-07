import {LitElement, css, html, nothing} from 'lit';
import {customElement, property} from 'lit/decorators.js';
import {Account} from '@tally/lib/core/account';

@customElement('account-tooltip')
export class AccountTooltip extends LitElement {
  static override styles = css`
    td {
      border-left: 1px solid #c3c3c3;
      border-top: 1px solid #c3c3c3;
      padding: 1px;
      vertical-align: middle;
    }
  `;

  @property({attribute: false})
  account: Account | undefined = undefined;

  onCloseButton() {
    this.dispatchEvent(new CustomEvent('close'));
  }
  override render() {
    if (!this.account) {
      return nothing;
    }
    return html`
      <span @click="${this.onCloseButton}">XXX</span>
      <table>
        <tr>
          <td>Account</td>
          <td>
            ${this.account.url
              ? html`<a href="${this.account.url}" target="_blank">${this.account.name}</a>`
              : this.account.name}
          </td>
        </tr>
        <tr>
          <td>Description</td>
          <td>${this.account.description}</td>
        </tr>
        <tr>
          <td>Number</td>
          <td>${this.account.number}</td>
        </tr>
        <tr>
          <td>User Name</td>
          <td>${this.account.userName}</td>
        </tr>
        <tr>
          <td>Password</td>
          <td>${this.account.password}</td>
        </tr>
        <tr>
          <td>Opened On</td>
          <td>${this.account.openedOn}</td>
        </tr>
        <tr>
          <td>Phone</td>
          <td>${this.account.phone}</td>
        </tr>
        <tr>
          <td>Address</td>
          <td>${this.account.address}</td>
        </tr>
      </table>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'account-tooltip': AccountTooltip;
  }
}
