import {Account} from '../core/account';
import {Balance} from '../core/balance';
import {Statement} from './statement';


export interface Transaction {
  isExpense: boolean;
  isIncome: boolean;
  toAccountName: string;
  balance: Balance;
}


// Extension of Statement for transactions over an account.
export interface TransactionStatement extends Statement {
  // Account to which this transaction statements belongs.
  account: Account;

  // List of transcation in this statement.
  transactions: Transaction[];

  // True if any transactions in this statement "cover" previous statement.
  coversPrevious: boolean;

  // True if any projected transactions in this statement "cover"
  // previous statement.
  coversProjectedPrevious: boolean;

  // True if any of the transcations are projects.
  hasProjectedTransfer: boolean;

  // True if this statement is covered by next.
  isCovered: boolean;

  // True if this statement is covered by any projected transactions in next
  // statement.
  isProjectedCovered: boolean;
}
