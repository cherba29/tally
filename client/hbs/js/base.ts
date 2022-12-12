import { Balance } from '@tally-lib';

export interface Transaction {
  isExpense: boolean;
  isIncome: boolean;
  toAccountName: string;
  balance: Balance;
  balanceFromStart: number;
  balanceFromEnd: number;
  description: string;
}

export interface Statement {
  inFlows?: number;
  outFlows?: number;
  income?: number;
  totalPayments?: number;
  totalTransfers?: number;
  isClosed?: boolean;
  addSub?: number;
  startBalance?: Balance;
  endBalance?: Balance;
  isCovered?: boolean;
  isProjectedCovered?: boolean;
  hasProjectedTransfer?: boolean;
  change?: number;
  percentChange?: number;
  unaccounted?: number;
  transactions?: Transaction[];
}

export interface SummaryStatement {
  inFlows: number;
  outFlows: number;
  income: number;
  totalPayments: number;
  totalTransfers: number;
  isClosed?: boolean;
  accounts: string[];
  addSub: number;
  startBalance: Balance;
  endBalance: Balance;
  isCovered?: boolean;
  isProjectedCovered?: boolean;
  hasProjectedTransfer?: boolean;
  change?: number;
  percentChange?: number;
  unaccounted?: number;
  transactions?: Transaction[];
}
