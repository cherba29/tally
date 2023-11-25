import { Account } from '../core/account';
import { Balance } from '../core/balance';
import { Month } from '../core/month';

// Abstraction for a financial statement for a period of time.
export abstract class Statement {
  // Account to which this statements belongs.
  account: Account;

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

  constructor(account: Account, month: Month) {
    this.account = account;
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
    if (change === 0) return 0;
    if (change === undefined || !startAmount) return undefined;
    return (100 * change) / startAmount;
  }

  get annualizedPercentChange(): number | undefined {
    const prctChange = this.percentChange;
    if (prctChange === undefined) {
      return undefined;
    }
    const result = Math.pow(1 + Math.abs(prctChange) / 100, 12) - 1;
    // Dont consider 1000% and more as meaningful annualized numbers.
    return result < 10 ? 100 * Math.sign(prctChange) * result : undefined;
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
