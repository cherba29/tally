import {Statement} from './base';

// Data for rendering given cell.
export class Cell {
  id: string;
  isClosed: boolean;
  addSub: number | null;
  balance: number | null;
  isProjected: boolean;
  isCovered: boolean;
  isProjectedCovered: boolean;
  hasProjectedTransfer: boolean;
  percentChange: number | null;
  unaccounted: number | null;
  balanced: boolean;

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
