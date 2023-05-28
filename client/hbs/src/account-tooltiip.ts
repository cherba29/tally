import {LitElement, html} from 'lit';
import {customElement, property} from 'lit/decorators.js';
import { Account } from '@tally/lib/core/account';

@customElement('account-tooltip')
export class AccountTooltip extends LitElement {
  @property()
  account: Account;

  constructor(account: Account, readonly onCloseButton: ()=>void) {
    super();
    this.account = account;
  }

  render() {
    return html`
    <div class="toolTip">
      <button @click="${this.onCloseButton}">XXX</button>
      <table>
      <tr>
        <td>Account</td>
        <td>
          ${this.account.url ? 
            html`<a href="${this.account.url}" target="_blank">${this.account.name}</a>` : 
            this.account.name
          }
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
    </div>
    `;
  }
}