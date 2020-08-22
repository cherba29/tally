import {TallyAccount} from './base';

// Data for rendering given row.
export class Row {
  title: string|TallyAccount;
  isSpace: boolean;
  isTotal: boolean;
  isNormal: boolean;
  cells: any[];

  constructor(title: string|TallyAccount, type: string, cells: any[]) {
    this.title = title;
    this.isSpace = ('SPACE' === type);
    this.isTotal = ('TOTAL' === type);
    this.isNormal = ('NORMAL' === type);
    this.cells = cells
  }
}
