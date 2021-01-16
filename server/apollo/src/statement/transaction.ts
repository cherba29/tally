import {Balance} from './balance';

export interface Transaction {
  isExpense: boolean;
  isIncome: boolean;
  toAccountName: string;
  balance: Balance;
}
