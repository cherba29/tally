import {BalanceType} from '@tally-lib';
import {Statement} from './base';

/** Data for rendering given cell. */
export class Cell {
  readonly id: string;
  readonly isClosed: boolean;
  readonly addSub: number | null;
  readonly balance: number | null;
  readonly isProjected: boolean;
  readonly isCovered: boolean;
  readonly isProjectedCovered: boolean;
  readonly hasProjectedTransfer: boolean;
  readonly percentChange: number | null;
  readonly unaccounted: number | null;
  readonly balanced: boolean;

  /**
   * Build cell representing the statement.
   * @param {string} id cell id
   * @param {Statement} stmt underlying statement.
   */
  constructor(id: string, stmt: Statement) {
    this.isClosed = stmt.isClosed ?? false;
    this.id = id;
    this.addSub = stmt.addSub ?? null;
    if (stmt.endBalance) {
      this.balance = stmt.endBalance.amount;
      this.isProjected = stmt.endBalance.type !== BalanceType.CONFIRMED;
    } else {
      this.balance = null;
      this.isProjected = false;
    }
    this.isCovered = stmt.isCovered ?? false;
    this.isProjectedCovered = stmt.isProjectedCovered ?? false;
    this.hasProjectedTransfer = stmt.hasProjectedTransfer ?? false;
    this.isProjected = this.isProjected || this.hasProjectedTransfer;
    this.percentChange = stmt.percentChange ?? null;
    if (stmt.unaccounted) {
      this.unaccounted = stmt.unaccounted;
      this.balanced = !this.unaccounted;
    } else {
      this.unaccounted = null;
      this.balanced = true;
    }
  }
}
