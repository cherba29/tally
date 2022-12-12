import {Account as TallyAccount} from '@tally-lib';

/** Data for rendering given row. */
export class Row {
  title: string|TallyAccount;
  isSpace: boolean;
  isTotal: boolean;
  isNormal: boolean;
  cells: any[];

  /**
   * Build row object.
   * @param {string|TallyAccount} title row title.
   * @param {string} type row type
   * @param {any[]} cells list of cells
   */
  constructor(title: string|TallyAccount, type: string, cells: any[]) {
    this.title = title;
    this.isSpace = ('SPACE' === type);
    this.isTotal = ('TOTAL' === type);
    this.isNormal = ('NORMAL' === type);
    this.cells = cells;
  }
}
