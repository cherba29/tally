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
  TAX = 'tax_'
}

export interface InitData {
  name: string;
  description?: string;
  type: Type;
  number?: string;
  openedOn?: Month;
  closedOn?: Month;
  owners: string[];
  url?: string;
  address?: string;
  phone?: string;
  userName?: string;
  password?: string;
}

export class Account {
  readonly name: string;
  readonly description?: string;
  // Account type, for example 'CREDIT_CARD'.
  readonly type: Type;
  // Real account number associated with this account.
  readonly number?: string;
  // Month when account was opened and possibly closed.
  readonly openedOn?: Month;
  readonly closedOn?: Month;
  // List of account owner ids.
  readonly owners: string[];
  // Url to the account.
  readonly url?: string;
  // Physical address for the account.
  readonly address?: string;
  // Phone number for customer support.
  readonly phone?: string;
  // Username/password to use to login to the account.
  readonly userName?: string;
  readonly password?: string;

  constructor(data: InitData) {
    this.name = data.name;
    this.description = data.description;
    this.type = data.type;
    this.number = data.number;
    this.openedOn = data.openedOn;
    this.closedOn = data.closedOn;
    this.owners = data.owners;
    this.url = data.url;
    this.address = data.address;
    this.phone = data.phone;
    this.userName = data.userName;
    this.password = data.password;
  }

  isClosed(month: Month): boolean {
    return (
      this.closedOn?.isLess(month) || // After closed.
      !this.openedOn?.isLess(month) // Before open.
    );
  }

  // Is this an external/bookeeping account. External accounts are not
  // considered to be part of any owner.
  get isExternal(): boolean {
    return (
      this.type === Type.EXTERNAL || this.type === Type.TAX || this.type === Type.DEFERRED_INCOME
    );
  }

  // Is this synthetic summary account.
  get isSummary(): boolean {
    return this.type === Type.SUMMARY;
  }

  hasCommonOwner(other: Account): boolean {
    return this.owners.some((owner) => other.owners.includes(owner));
  }
}
