import { Month } from '../core/month';
import { Balance } from '../core/balance';

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
  inFlows = 0;

  // Total transaction outflows.
  outFlows = 0;

  // Amount transfered to other accounts by same owner.
  totalTransfers = 0;

  // Amount transfered to external entities.
  totalPayments = 0;

  // Amount transfered from external entities.
  income = 0;

  constructor(name: string, month: Month) {
    this.name = name;
    this.month = month;
  }

  get addSub(): number {
    return this.inFlows + this.outFlows;
  }

  get change(): number | undefined {
    const startAmount = this.startBalance?.amount;
    if (startAmount === undefined) return undefined;
    const endAmount = this.endBalance?.amount;
    if (endAmount === undefined) return undefined;
    return endAmount - startAmount;
  }

  get percentChange(): number | undefined {
    const startAmount = this.startBalance?.amount;
    const change = this.change;
    return change && startAmount && (100 * change) / startAmount;
  }

  get unaccounted(): number | undefined {
    const change = this.change;
    return change !== undefined ? change - this.addSub : undefined;
  }

  abstract get isClosed(): boolean;

  addInFlow(inFlow: number): void {
    if (inFlow > 0) {
      this.inFlows += inFlow;
    } else {
      this.outFlows += inFlow;
    }
  }

  addOutFlow(outFlow: number): void {
    if (outFlow > 0) {
      this.inFlows += outFlow;
    } else {
      this.outFlows += outFlow;
    }
  }
}
