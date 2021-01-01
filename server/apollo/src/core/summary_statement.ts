import {Balance} from './balance';
import {Transaction} from './transaction';


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
