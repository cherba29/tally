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
    this.isClosed = stmt.isClosed;
    this.id = id;
    this.addSub = ('addSub' in stmt) ? stmt.addSub : null;
    if ('endBalance' in stmt && stmt.endBalance !== null) {
      this.balance = stmt.endBalance.amount;
      this.isProjected = stmt.endBalance.type !== 'CONFIRMED';
    } else {
      this.balance = null;
      this.isProjected = false;
    }
    this.isCovered = stmt.isCovered;
    this.isProjectedCovered = stmt.isProjectedCovered;
    this.hasProjectedTransfer = stmt.hasProjectedTransfer;
    this.isProjected = this.isProjected || this.hasProjectedTransfer;
    this.percentChange = ('percentChange' in stmt) ? stmt.percentChange : null;
    if ('unaccounted' in stmt) {
      this.unaccounted = stmt.unaccounted;
      this.balanced = !this.unaccounted;
    } else {
      this.unaccounted = null;
      this.balanced = true;
    }
  }
}
