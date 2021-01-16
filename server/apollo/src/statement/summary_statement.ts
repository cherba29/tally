import {Balance} from '../core/balance';
import {Transaction} from '../core/transaction';


export interface SummaryStatement {
  isClosed: boolean;
  accounts: string[];
  addSub: number;
  endBalance: Balance;
  isCovered: boolean;
  isProjectedCovered: boolean;
  hasProjectedTransfer: boolean;
  percentChange: number;
  unaccounted: number;
  transactions: Transaction[];
}
