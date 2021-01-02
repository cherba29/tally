import { Month } from './month';

export interface InitData {
  name: string;
  description?: string;
  type: string;
  number?: string;
  openedOn?: Month;
  closedOn?: Month;
}

export class Account {
  readonly name: string;
  readonly description?: string;
  readonly type: string;
  readonly number?: string;
  readonly openedOn?: Month;
  readonly closedOn?: Month;

  constructor(data: InitData) {
    this.name = data.name;
    this.description = data.description;
    this.type = data.type;
    this.number = data.number;
    this.openedOn = data.openedOn;
    this.closedOn = data.closedOn;
  }
}