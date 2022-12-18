import {Account} from '@tally-lib';

export enum Type  {
  SPACE = 'SPACE',
  TOTAL = 'TOTAL',
  NORMAL = 'NORMAL',
}

/** Data for rendering given row. */
export class Row {
  title: string|Account;
  isSpace: boolean;
  isTotal: boolean;
  isNormal: boolean;
  cells: any[];

  /**
   * Build row object.
   * @param {string|Account} title row title.
   * @param {string} type row type
   * @param {any[]} cells list of cells
   */
  constructor(title: string|Account, type: Type, cells: any[]) {
    this.title = title;
    this.isSpace = Type.SPACE === type;
    this.isTotal = Type.TOTAL === type;
    this.isNormal = Type.NORMAL === type;
    this.cells = cells;
  }
}
