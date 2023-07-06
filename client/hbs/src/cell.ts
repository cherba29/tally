import {Type as BalanceType} from '@tally/lib/core/balance';
import {GqlStatement, GqlSummaryStatement} from './gql_types';

/** Data for rendering given cell. */
export class Cell {
  readonly id: string;
  readonly month: string;
  readonly isClosed?: boolean;
  readonly addSub: number | null;
  readonly balance: number | null;
  readonly isProjected: boolean;
  readonly isCovered?: boolean;
  readonly isProjectedCovered?: boolean;
  readonly hasProjectedTransfer?: boolean;
  readonly percentChange: number | null;
  readonly unaccounted: number | null;
  readonly balanced: boolean;

  /**
   * Build cell representing the statement.
   * @param owner account owner, used for id.
   * @param accountName account name, used for id.
   * @param month used for id.
   * @param stmt underlying statement.
   */
  constructor(
    owner: string,
    accountName: string,
    month: string,
    stmt: GqlStatement | GqlSummaryStatement
  ) {
    this.id = `${owner}_${accountName}_${month}`;
    this.month = month;
    this.addSub = stmt.addSub ?? null;
    if (stmt.endBalance) {
      this.balance = stmt.endBalance.amount ?? null;
      this.isProjected = stmt.endBalance.type !== BalanceType.CONFIRMED;
    } else {
      this.balance = null;
      this.isProjected = false;
    }
    if ('isClosed' in stmt) {
      this.isClosed = stmt.isClosed ?? false;
      this.isCovered = stmt.isCovered ?? false;
      this.isProjectedCovered = stmt.isProjectedCovered ?? false;
      this.hasProjectedTransfer = stmt.hasProjectedTransfer ?? false;
      this.isProjected = this.isProjected || this.hasProjectedTransfer;
    }
    this.percentChange = stmt.percentChange ?? null;
    this.unaccounted = stmt.unaccounted ?? null;
    this.balanced = !this.unaccounted;
  }
}
