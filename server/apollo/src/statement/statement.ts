import {Month} from '../core/month';
import {Balance} from '../core/balance';


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

