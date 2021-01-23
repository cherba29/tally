import { Month } from './month';

export enum Type {
  BILL = 'bill',
  CHECKING = 'checking',
  CREDIT = 'credit',
  CREDIT_CARD = 'credit-card',
  DEFERRED_INCOME = 'deferred income',
  EXTERNAL = 'external',
  INCOME = 'income',
  INVESTMENT = 'investment',
  RETIREMENT = 'retirement',
  SUMMARY = '_summary_',
  TAX = 'tax_',
}

export interface InitData {
  name: string;
  description?: string;
  type: Type;
  number?: string;
  openedOn?: Month;
  closedOn?: Month;
  owners: string[];
}

export class Account {
  readonly name: string;
  readonly description?: string;
  readonly type: Type;
  readonly number?: string;
  readonly openedOn?: Month;
  readonly closedOn?: Month;
  readonly owners: string[];

  constructor(data: InitData) {
    this.name = data.name;
    this.description = data.description;
    this.type = data.type;
    this.number = data.number;
    this.openedOn = data.openedOn;
    this.closedOn = data.closedOn;
    this.owners = data.owners;
  }

  isClosed(month: Month) : boolean {
    return this.closedOn?.isLess(month)  // After closed.
        || !this.openedOn?.isLess(month);  // Before open.
  }
}