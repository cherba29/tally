export type Maybe<T> = T | null;
export type InputMaybe<T> = Maybe<T>;
export type Exact<T extends { [key: string]: unknown }> = { [K in keyof T]: T[K] };
export type MakeOptional<T, K extends keyof T> = Omit<T, K> & { [SubKey in K]?: Maybe<T[SubKey]> };
export type MakeMaybe<T, K extends keyof T> = Omit<T, K> & { [SubKey in K]: Maybe<T[SubKey]> };
export type MakeEmpty<T extends { [key: string]: unknown }, K extends keyof T> = { [_ in K]?: never };
export type Incremental<T> = T | { [P in keyof T]?: P extends ' $fragmentName' | '__typename' ? T[P] : never };
/** All built-in and custom scalars, mapped to their actual values */
export type Scalars = {
  ID: { input: string; output: string; }
  String: { input: string; output: string; }
  Boolean: { input: boolean; output: boolean; }
  Int: { input: number; output: number; }
  Float: { input: number; output: number; }
  Date: { input: any; output: any; }
  GqlMonth: { input: any; output: any; }
  Long: { input: any; output: any; }
};

export type GqlAccount = {
  __typename?: 'GqlAccount';
  address: Scalars['String']['output'];
  /** Month when account was closed. If not set means account is still open. */
  closedOn?: Maybe<Scalars['GqlMonth']['output']>;
  /** Long description for the account. */
  description: Scalars['String']['output'];
  external: Scalars['Boolean']['output'];
  /** Account id/name. */
  name: Scalars['String']['output'];
  /** Account number. Can be null or unknown for external or proxy accounts. */
  number?: Maybe<Scalars['String']['output']>;
  /** Month when account was open. Can be unknown. */
  openedOn?: Maybe<Scalars['GqlMonth']['output']>;
  /** List of owner ids for this account. */
  owners: Array<Scalars['String']['output']>;
  password: Scalars['String']['output'];
  /** Replacement for type, so that accounts are grouped. */
  path: Array<Scalars['String']['output']>;
  phone: Scalars['String']['output'];
  summary: Scalars['Boolean']['output'];
  url: Scalars['String']['output'];
  userName: Scalars['String']['output'];
};

export type GqlBalance = {
  __typename?: 'GqlBalance';
  amount: Scalars['Long']['output'];
  date: Scalars['Date']['output'];
  desc: Scalars['String']['output'];
  type: Scalars['String']['output'];
};

/** Sum of transfers for given month for given account or summary. */
export type GqlMonthTransferSummary = {
  __typename?: 'GqlMonthTransferSummary';
  /** Sum of transfers from external accounts for this month. */
  externalTransfers: Scalars['Long']['output'];
  /** Sum of transfers from internal accounts for this month. */
  internalTransfers: Scalars['Long']['output'];
  /** Total annualized rate of return. */
  totalAnnualPrct: Scalars['Float']['output'];
  /** Sum of all previous and current transfers from external accounts for this month. */
  totalExternalTransfers: Scalars['Long']['output'];
  /** Total external transfer as annualized rate of internal transfers. */
  totalExternalTransfersAnnualPrct: Scalars['Float']['output'];
  /** Total external transfer as percentage of all transfers. */
  totalExternalTransfersPrct: Scalars['Float']['output'];
  /** Sum of all previous and current transfers from internal accounts for this month. */
  totalInternalTransfers: Scalars['Long']['output'];
  /** Total internal transfer as annualized rate of external transfers. */
  totalInternalTransfersAnnualPrct: Scalars['Float']['output'];
  /** Total internal transfer as percentage of all transfers. */
  totalInternalTransfersPrct: Scalars['Float']['output'];
  /** Sum of internal and external transfers. */
  totalMonthTransfers: Scalars['Long']['output'];
  /** Running total of all transfers including previous months. */
  totalTransfers: Scalars['Long']['output'];
  /** Difference between recorded balance and running totalTransfers. */
  unaccounted: Scalars['Long']['output'];
  /** Weighted age of the deposited amounts. */
  weightedAge: Scalars['Float']['output'];
};

export type GqlStatement = {
  __typename?: 'GqlStatement';
  addSub: Scalars['Long']['output'];
  annualizedPercentChange: Scalars['Float']['output'];
  change: Scalars['Long']['output'];
  endBalance?: Maybe<GqlBalance>;
  hasProjectedTransfer: Scalars['Boolean']['output'];
  inFlows: Scalars['Long']['output'];
  income: Scalars['Long']['output'];
  isClosed: Scalars['Boolean']['output'];
  isCovered: Scalars['Boolean']['output'];
  isProjectedCovered: Scalars['Boolean']['output'];
  month: Scalars['GqlMonth']['output'];
  name: Scalars['String']['output'];
  outFlows: Scalars['Long']['output'];
  percentChange: Scalars['Float']['output'];
  startBalance?: Maybe<GqlBalance>;
  totalPayments: Scalars['Long']['output'];
  totalTransfers: Scalars['Long']['output'];
  transactions: Array<GqlTransaction>;
  unaccounted: Scalars['Long']['output'];
};

export type GqlSummaryData = {
  __typename?: 'GqlSummaryData';
  statements: Array<GqlStatement>;
  total: GqlSummaryStatement;
};

export type GqlSummaryStatement = {
  __typename?: 'GqlSummaryStatement';
  accounts: Array<Scalars['String']['output']>;
  addSub: Scalars['Long']['output'];
  annualizedPercentChange: Scalars['Float']['output'];
  change: Scalars['Long']['output'];
  endBalance?: Maybe<GqlBalance>;
  inFlows: Scalars['Long']['output'];
  income: Scalars['Long']['output'];
  month: Scalars['GqlMonth']['output'];
  name: Scalars['String']['output'];
  outFlows: Scalars['Long']['output'];
  percentChange: Scalars['Float']['output'];
  startBalance?: Maybe<GqlBalance>;
  totalPayments: Scalars['Long']['output'];
  totalTransfers: Scalars['Long']['output'];
  unaccounted: Scalars['Long']['output'];
};

export type GqlTable = {
  __typename?: 'GqlTable';
  currentOwner: Scalars['String']['output'];
  months: Array<Scalars['GqlMonth']['output']>;
  owners: Array<Scalars['String']['output']>;
  rows: Array<GqlTableRow>;
};

export type GqlTableCell = {
  __typename?: 'GqlTableCell';
  addSub: Scalars['Long']['output'];
  annualizedPercentChange: Scalars['Float']['output'];
  balance?: Maybe<Scalars['Long']['output']>;
  balanced: Scalars['Boolean']['output'];
  hasProjectedTransfer: Scalars['Boolean']['output'];
  isClosed: Scalars['Boolean']['output'];
  isCovered: Scalars['Boolean']['output'];
  isProjected: Scalars['Boolean']['output'];
  isProjectedCovered: Scalars['Boolean']['output'];
  month: Scalars['GqlMonth']['output'];
  percentChange: Scalars['Float']['output'];
  unaccounted?: Maybe<Scalars['Long']['output']>;
};

export type GqlTableRow = {
  __typename?: 'GqlTableRow';
  /** Summary rows don't have an account. */
  account?: Maybe<GqlAccount>;
  cells: Array<GqlTableCell>;
  id: Scalars['String']['output'];
  indent: Scalars['Int']['output'];
  isNormal: Scalars['Boolean']['output'];
  isSpace: Scalars['Boolean']['output'];
  isTotal: Scalars['Boolean']['output'];
  title: Scalars['String']['output'];
};

export type GqlTransaction = {
  __typename?: 'GqlTransaction';
  balance: GqlBalance;
  balanceFromStart: Scalars['Long']['output'];
  description: Scalars['String']['output'];
  isExpense: Scalars['Boolean']['output'];
  isIncome: Scalars['Boolean']['output'];
  toAccountName: Scalars['String']['output'];
};

/** Return payload for transfersSummary query. */
export type GqlTransfersSummary = {
  __typename?: 'GqlTransfersSummary';
  /** For each of the months internal/external summary of transfers. */
  data: Array<GqlMonthTransferSummary>;
  /** List of months this summary covers, sorted descending order. */
  months: Array<Scalars['GqlMonth']['output']>;
};

export type Query = {
  __typename?: 'Query';
  hello: Scalars['String']['output'];
  /** Returns a monthly statement for given account. */
  statement: GqlStatement;
  /** Generates delta summary table between two months. */
  summary: GqlSummaryData;
  /** Generates full tally table in given month range. */
  table: GqlTable;
  /** Generates sum of internal/external transfers for each month. */
  transfersSummary: GqlTransfersSummary;
};


export type QueryStatementArgs = {
  accountPath: Scalars['String']['input'];
  month: Scalars['GqlMonth']['input'];
};


export type QuerySummaryArgs = {
  accountPath: Scalars['String']['input'];
  endMonth: Scalars['GqlMonth']['input'];
  startMonth?: InputMaybe<Scalars['GqlMonth']['input']>;
};


export type QueryTableArgs = {
  endMonth: Scalars['GqlMonth']['input'];
  owner?: InputMaybe<Scalars['String']['input']>;
  startMonth: Scalars['GqlMonth']['input'];
};


export type QueryTransfersSummaryArgs = {
  accountPath: Scalars['String']['input'];
  endMonth: Scalars['GqlMonth']['input'];
  startMonth?: InputMaybe<Scalars['GqlMonth']['input']>;
};
