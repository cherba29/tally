import {Statement} from './statement';
import {Transaction} from './transaction';


export interface SummaryStatement extends Statement {
  isClosed: boolean;
  accounts: string[];
  addSub: number;
  isCovered: boolean;
  isProjectedCovered: boolean;
  hasProjectedTransfer: boolean;
  percentChange: number;
  unaccounted: number;
  transactions: Transaction[];
}
