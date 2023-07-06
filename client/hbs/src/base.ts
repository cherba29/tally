import {Balance} from '@tally/lib/core/balance';
import {GqlTransaction} from './gql_types';

export interface SummaryStatement {
  inFlows: number;
  outFlows: number;
  income: number;
  totalPayments: number;
  totalTransfers: number;
  isClosed?: boolean;
  accounts: string[];
  addSub: number;
  startBalance?: Balance;
  endBalance?: Balance;
  isCovered?: boolean;
  isProjectedCovered?: boolean;
  hasProjectedTransfer?: boolean;
  change?: number;
  percentChange?: number;
  unaccounted?: number;
  transactions?: GqlTransaction[];
}
