export interface TallyAccount {
  name: string;
  description: string;

  // Month when account was opened and possibly closed.
  openedOn: string;
  closedOn: string | null;

  // Is this an external/bookeeping account. External accounts are not
  // considered to be part of any owner.
  external: boolean;

  // Is this synthetic summary account.
  summary: boolean;

  // List of account owner ids.
  owners: string[];

  // Account type, for example 'CREDIT_CARD'.
  type: string;

  // Url to the account.
  url: string;

  // Physical address for the account.
  address: string;

  // Phone number for customer support.
  phone: string;

  // Username/password to use to login to the account.
  userName: string;
  password: string;

  // Real account number associated with this account.
  number: string;
}

export interface Balance {
  amount: number;
  type: string;
}

export interface Transaction {
  isExpense: boolean;
  isIncome: boolean;
  toAccountName: string;
  balance: Balance;
}

export interface Statement {
  isClosed: boolean;
  addSub: number;
  endBalance: Balance;
  isCovered: boolean;
  isProjectedCovered: boolean;
  hasProjectedTransfer: boolean;
  percentChange: number;
  unaccounted: number;
  transactions: Transaction[];
}

export interface SummaryStatement {
  isClosed: boolean;
  accounts: string[];
  addSub: number;
  endBalance: Balance;
  isCovered: boolean;
  isProjectedCovered: boolean;
  hasProjectedTransfer: boolean;
  percentChange: number;
  unaccounted: number;
  transactions: Transaction[];
}
