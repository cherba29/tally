import {GqlAccount} from './gql_types';
import {Cell} from './cell';

/** Row type, each is rendered differently. */
export enum Type {
  // Empty row with just heading, used for summary.
  // For each owner summary and then owner/type summary.
  SPACE,
  // Row containing totals for summary per month.
  TOTAL,
  // Row containing account statements per month.
  NORMAL,
}

/** Data for rendering given row. */
export class Row {
  title: string;
  account?: GqlAccount;
  isSpace: boolean;
  isTotal: boolean;
  isNormal: boolean;
  cells: Cell[];

  /**
   * Build row object.
   * @param title row title or account itself for NORMAL type.
   * @param type row type
   * @param cells list of cells
   */
  constructor(title: string, account: GqlAccount | undefined, type: Type, cells: Cell[]) {
    this.title = title;
    this.account = account;
    this.isSpace = Type.SPACE === type;
    this.isTotal = Type.TOTAL === type;
    this.isNormal = Type.NORMAL === type;
    this.cells = cells;
  }
}
