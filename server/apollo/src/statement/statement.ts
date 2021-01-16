import {Month} from './month';
import {Balance} from './balance';
import {Transaction} from './transaction';


// Abstraction for a financial statement for a period of time.
export interface Statement {
  name: string;
  
  // Period of time for the statement 
  month: Month;  
  
  // Recorded start balance for the statement.
  startBalance?: Balance; 
  
  // Recorded end balance for the statement. 
  endBalance?: Balance;  

  // Total transaction inflows.
  inFlows: number;

  // Total transaction outflows.
  outFlows: number;  

  // Amount transfered to other accounts by same owner.
  totalTransfers: number;  

  // Amount transfered to external entities.
  totalPayments: number;

  // Amount transfered from external entities.
  income: number;
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
