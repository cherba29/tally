import {LitElement, css, html, nothing} from 'lit';
import {customElement, property} from 'lit/decorators.js';
import {type GqlAccount} from '../gql_types';
import {tooltipSharedStyles} from './tooltip-styles';

@customElement('account-tooltip')
export class AccountTooltip extends LitElement {
  static override styles = [
    tooltipSharedStyles,
    css`
      td:first-child {
        font-weight: 700;
        background-color: #f0f3f6;
        color: #24292f;
        white-space: nowrap;
      }
      a {
        color: #0969da;
      }
    `,
  ];

  private __account: GqlAccount | undefined = undefined;

  @property({attribute: false})
  set account(value: GqlAccount | undefined) {
    const oldValue = this.__account;
    this.__account = value;
    this.requestUpdate('account', oldValue);
  }
  get account() {
    return this.__account;
  }

  onCloseButton() {
    this.dispatchEvent(new CustomEvent('close'));
  }
  override render() {
    if (!this.account) {
      return nothing;
    }
    return html`
      <div class="header">
        <button
          type="button"
          class="close-btn"
          title="Close account details"
          aria-label="Close"
          @click="${this.onCloseButton}"
        >
          &times;
        </button>
        <span class="title">${this.account.name}</span>
      </div>
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
        <tr>
          <td>Statement Closes</td>
          <td>${this.account.statementCloseDay}</td>
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
