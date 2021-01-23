import {Month} from '../core/month';
import {Balance} from '../core/balance';


// Abstraction for a financial statement for a period of time.
export abstract class Statement {
  name: string;
  
  // Period of time for the statement 
  month: Month;  
  
  // Recorded start balance for the statement.
  startBalance?: Balance; 
  
  // Recorded end balance for the statement. 
  endBalance?: Balance;  

  // Total transaction inflows.
  inFlows: number = 0;

  // Total transaction outflows.
  outFlows: number = 0;  

  // Amount transfered to other accounts by same owner.
  totalTransfers: number = 0;  

  // Amount transfered to external entities.
  totalPayments: number = 0;

  // Amount transfered from external entities.
  income: number = 0;

  constructor(name: string, month: Month) {
    this.name = name;
    this.month = month;
  }

  get addSub(): number {
    return this.inFlows + this.outFlows;
  }

  get change(): number | undefined {
    const startAmount = this.startBalance?.amount
    const endAmount = this.endBalance?.amount
    return startAmount && endAmount && (endAmount - startAmount);
  }

  get percentChange(): number | undefined {
    const startAmount = this.startBalance?.amount
    const change = this.change;
    return change && startAmount && (100 * change / startAmount);
  }

  get unaccounted(): number | undefined {
    const change = this.change;
    return change && change - this.addSub;
  }

  abstract get isClosed(): boolean;

  addInFlow(inFlow: number) {
    if (inFlow > 0) {
      this.inFlows += inFlow;
    } else {
      this.outFlows += inFlow;
    }
  }

  addOutFlow(outFlow: number) {
    if (outFlow > 0) {
      this.inFlows += outFlow;
    } else {
      this.outFlows += outFlow;
    }
  }
}
