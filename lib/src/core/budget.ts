import { Account } from './account';
import { Balance } from './balance';
import { Month } from './month';
import { Transfer } from './transfer';

export class Budget {
  constructor(
    // Period over which the budget is defined.
    readonly months: Month[],
    // Account name to account map.
    readonly accounts: Map<string, Account>,
    // Account name -> month -> balance map.
    readonly balances: Map<string, Map<string, Balance>>,
    // Account name -> month -> transfers map.
    readonly transfers: Map<string, Map<string, Set<Transfer>>>
  ) {}

  findActiveAccounts(): Account[] {
    const accounts: Account[] = [];
    for (const account of this.accounts.values()) {
      if (this.months.some((m) => !account.isClosed(m))) {
        accounts.push(account);
      }
    }
    return accounts;
  }
}

export interface TransferData {
  toAccount: string;
  toMonth: Month;
  fromAccount: string;
  fromMonth: Month;
  balance: Balance;
  description?: string;
}

function getMonthTransfers<T>(
  transfers: Map<string, Map<string, Set<T>>>,
  accountName: string,
  month: string
) {
  let accountTransfers = transfers.get(accountName);
  if (!accountTransfers) {
    accountTransfers = new Map<string, Set<T>>();
    transfers.set(accountName, accountTransfers);
  }
  let monthTransfers = accountTransfers.get(month);
  if (!monthTransfers) {
    monthTransfers = new Set<T>();
    accountTransfers.set(month, monthTransfers);
  }
  return monthTransfers;
}

export class BudgetBuilder {
  // Period over which the budget is defined.
  months: Month[] = [];
  // Account name to account map.
  readonly accounts = new Map<string, Account>();
  // Account name -> month -> balance map.
  readonly balances = new Map<string, Map<string, Balance>>();
  readonly transfers: TransferData[] = [];

  setPeriod(start: Month, end: Month): void {
    this.months = Array.from(Month.generate(start, end));
  }

  setAccount(account: Account): void {
    this.accounts.set(account.name, account);
  }

  setBalance(accountName: string, month: string, balance: Balance): void {
    let balances = this.balances.get(accountName);
    if (!balances) {
      balances = new Map<string, Balance>();
      this.balances.set(accountName, balances);
    }
    const existingBalance = balances.get(month);
    if (existingBalance) {
      throw new Error(
        `Balance for '${accountName}' '${month}' is already set to ${balance.toString()}`
      );
    }
    balances.set(month, balance);
  }

  addTransfer(transferData: TransferData): void {
    this.transfers.push(transferData);
  }

  build(): Budget {
    const transfers = new Map<string, Map<string, Set<Transfer>>>();
    for (const transferData of this.transfers) {
      const toAccount = this.accounts.get(transferData.toAccount);
      if (!toAccount) {
        throw new Error(`Unknown account ${transferData.toAccount}`);
      }
      const fromAccount = this.accounts.get(transferData.fromAccount);
      if (!fromAccount) {
        throw new Error(`Unknown account ${transferData.fromAccount}`);
      }
      const transfer: Transfer = {
        toAccount,
        fromAccount,
        toMonth: transferData.toMonth,
        fromMonth: transferData.fromMonth,
        balance: transferData.balance,
        description: transferData.description
      };
      const toMonthTransfers = getMonthTransfers(
        transfers,
        toAccount.name,
        transferData.toMonth.toString()
      );
      toMonthTransfers.add(transfer);
      const fromMonthTransfers = getMonthTransfers(
        transfers,
        fromAccount.name,
        transferData.fromMonth.toString()
      );
      fromMonthTransfers.add(transfer);
    }
    return new Budget(this.months, this.accounts, this.balances, transfers);
  }
}
