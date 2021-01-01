import {Account} from './account';
import {Balance} from './balance';
import {Month} from './month';
import {Transfer} from './transfer';

export class Budget {
  // Period over which the budget is defined.
  months: Month[] = [];
  // Account name to account map.
  readonly accounts = new Map<string, Account>();
  // Account name -> month -> balance map.
  readonly balances = new Map<string, Map<string, Balance>>();
  // Account name -> month -> transfers map.
  readonly transfers = new Map<string, Map<string, Set<Transfer>>>();

  setPeriod(start: Month, end: Month) {
    this.months = Array.from(Month.generate(start, end));
  }

  setAccount(account: Account) {
    this.accounts.set(account.name, account);
  }

  setBalance(accountName: string, month: string, balance: Balance) {
    let balances = this.balances.get(accountName);
    if (!balances) {
      balances = new Map<string, Balance>();
      this.balances.set(accountName, balances);
    }
    balances.set(month, balance);
  }
}