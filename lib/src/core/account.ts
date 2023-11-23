import { Month } from './month';

export enum Type {
  UNSPECIFIED = '_unspecified_',
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

// Typescript cannot reverse map string enums, so prebuild a lookup map.
// https://www.typescriptlang.org/docs/handbook/enums.html
const reverseAccountType = new Map<string, string>(Object.entries(Type).map(([k, v]) => [v, k]));

export interface InitData {
  name: string;
  description?: string;
  parents?: Account[];
  children?: Account[];
  autoBalance?: boolean;
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
  readonly parents: Account[];
  readonly children: Account[];
  // Account type, for example 'CREDIT_CARD'.
  readonly type: Type;
  // Account balances are automatically considered to be committed.
  readonly autoBalance: boolean;
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
    this.parents = data.parents ?? [];
    this.children = data.children ?? [];
    this.autoBalance = data.autoBalance ?? false;
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

  toString() {
    return `Account ${this.name} ${this.type}${this.closedOn ? ` Closed ${this.closedOn}` : ''}`;
  }

  isClosed(month: Month): boolean {
    return !!(
      (
        this.closedOn?.isLess(month) || // After closed.
        (this.openedOn && month.isLess(this.openedOn))
      ) // Before or on open.
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

  get typeIdName(): string {
    return reverseAccountType.get(this.type.toString()) || '';
  }
}
