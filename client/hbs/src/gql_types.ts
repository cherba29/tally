export type Maybe<T> = T | null;
export type InputMaybe<T> = Maybe<T>;
export type Exact<T extends { [key: string]: unknown }> = { [K in keyof T]: T[K] };
export type MakeOptional<T, K extends keyof T> = Omit<T, K> & { [SubKey in K]?: Maybe<T[SubKey]> };
export type MakeMaybe<T, K extends keyof T> = Omit<T, K> & { [SubKey in K]: Maybe<T[SubKey]> };
/** All built-in and custom scalars, mapped to their actual values */
export type Scalars = {
  ID: string;
  String: string;
  Boolean: boolean;
  Int: number;
  Float: number;
  Date: any;
  GqlMonth: any;
};

export type GqlAccount = {
  __typename?: 'GqlAccount';
  address?: Maybe<Scalars['String']>;
  /** Month when account was clased. If not set means account is still open. */
  closedOn?: Maybe<Scalars['GqlMonth']>;
  /** Long description for the account. */
  description?: Maybe<Scalars['String']>;
  external?: Maybe<Scalars['Boolean']>;
  /** Account id/name. */
  name?: Maybe<Scalars['String']>;
  /** Account number. Can be null or unknown for external or proxy accounts. */
  number?: Maybe<Scalars['String']>;
  /** Month when account was open. Can be unknown. */
  openedOn?: Maybe<Scalars['GqlMonth']>;
  /** List of owner ids for this account. */
  owners?: Maybe<Array<Maybe<Scalars['String']>>>;
  password?: Maybe<Scalars['String']>;
  phone?: Maybe<Scalars['String']>;
  summary?: Maybe<Scalars['Boolean']>;
  path?: Maybe<Array<Maybe<Scalars['String']>>>;
  /**
   * Account type. Determines how account is grouped.
   * TODO: This should change to enum.
   */
  type?: Maybe<Scalars['String']>;
  url?: Maybe<Scalars['String']>;
  userName?: Maybe<Scalars['String']>;
};

export type GqlBalance = {
  __typename?: 'GqlBalance';
  /** Amount in cents. */
  amount?: Maybe<Scalars['Int']>;
  date?: Maybe<Scalars['Date']>;
  type?: Maybe<Scalars['String']>;
};

export type GqlStatement = {
  __typename?: 'GqlStatement';
  addSub?: Maybe<Scalars['Int']>;
  change?: Maybe<Scalars['Int']>;
  endBalance?: Maybe<GqlBalance>;
  hasProjectedTransfer?: Maybe<Scalars['Boolean']>;
  inFlows?: Maybe<Scalars['Int']>;
  income?: Maybe<Scalars['Int']>;
  isClosed?: Maybe<Scalars['Boolean']>;
  isCovered?: Maybe<Scalars['Boolean']>;
  isProjectedCovered?: Maybe<Scalars['Boolean']>;
  month?: Maybe<Scalars['GqlMonth']>;
  name?: Maybe<Scalars['String']>;
  outFlows?: Maybe<Scalars['Int']>;
  percentChange?: Maybe<Scalars['Float']>;
  annualizedPercentChange?: Maybe<Scalars['Float']>;
  startBalance?: Maybe<GqlBalance>;
  totalPayments?: Maybe<Scalars['Int']>;
  totalTransfers?: Maybe<Scalars['Int']>;
  transactions?: Maybe<Array<Maybe<GqlTransaction>>>;
  unaccounted?: Maybe<Scalars['Float']>;
};

export type GqlSummaryData = {
  __typename?: 'GqlSummaryData';
  statements?: Maybe<Array<Maybe<GqlStatement>>>;
  total?: Maybe<GqlSummaryStatement>;
};

export type GqlSummaryStatement = {
  __typename?: 'GqlSummaryStatement';
  accounts?: Maybe<Array<Maybe<Scalars['String']>>>;
  addSub?: Maybe<Scalars['Int']>;
  change?: Maybe<Scalars['Int']>;
  endBalance?: Maybe<GqlBalance>;
  inFlows?: Maybe<Scalars['Int']>;
  income?: Maybe<Scalars['Int']>;
  month?: Maybe<Scalars['GqlMonth']>;
  name?: Maybe<Scalars['String']>;
  outFlows?: Maybe<Scalars['Int']>;
  percentChange?: Maybe<Scalars['Float']>;
  annualizedPercentChange?: Maybe<Scalars['Float']>;
  startBalance?: Maybe<GqlBalance>;
  totalPayments?: Maybe<Scalars['Int']>;
  totalTransfers?: Maybe<Scalars['Int']>;
  unaccounted?: Maybe<Scalars['Int']>;
};

export type GqlTable = {
  __typename?: 'GqlTable';
  currentOwner?: Maybe<Scalars['String']>;
  months?: Maybe<Array<Maybe<Scalars['GqlMonth']>>>;
  owners?: Maybe<Array<Maybe<Scalars['String']>>>;
  rows?: Maybe<Array<Maybe<GqlTableRow>>>;
};

export type GqlTableCell = {
  __typename?: 'GqlTableCell';
  addSub?: Maybe<Scalars['Int']>;
  balance?: Maybe<Scalars['Int']>;
  balanced?: Maybe<Scalars['Boolean']>;
  hasProjectedTransfer?: Maybe<Scalars['Boolean']>;
  isClosed?: Maybe<Scalars['Boolean']>;
  isCovered?: Maybe<Scalars['Boolean']>;
  isProjected?: Maybe<Scalars['Boolean']>;
  isProjectedCovered?: Maybe<Scalars['Boolean']>;
  month?: Maybe<Scalars['GqlMonth']>;
  percentChange?: Maybe<Scalars['Float']>;
  annualizedPercentChange?: Maybe<Scalars['Float']>;
  unaccounted?: Maybe<Scalars['Int']>;
};

export type GqlTableRow = {
  __typename?: 'GqlTableRow';
  account?: Maybe<GqlAccount>;
  cells?: Maybe<Array<Maybe<GqlTableCell>>>;
  indent?: Maybe<Scalars['Int']>;
  isNormal?: Maybe<Scalars['Boolean']>;
  isSpace?: Maybe<Scalars['Boolean']>;
  isTotal?: Maybe<Scalars['Boolean']>;
  title?: Maybe<Scalars['String']>;
};

export type GqlTransaction = {
  __typename?: 'GqlTransaction';
  balance?: Maybe<GqlBalance>;
  balanceFromStart?: Maybe<Scalars['Int']>;
  description?: Maybe<Scalars['String']>;
  isExpense?: Maybe<Scalars['Boolean']>;
  isIncome?: Maybe<Scalars['Boolean']>;
  toAccountName?: Maybe<Scalars['String']>;
};

export type Query = {
  __typename?: 'Query';
  files?: Maybe<Array<Maybe<Scalars['String']>>>;
  statement?: Maybe<GqlStatement>;
  summary?: Maybe<GqlSummaryData>;
  table?: Maybe<GqlTable>;
};

export type QueryStatementArgs = {
  account: Scalars['String'];
  month: Scalars['GqlMonth'];
  owner: Scalars['String'];
};

export type QuerySummaryArgs = {
  accountType: Scalars['String'];
  startMonth: Scalars['GqlMonth'];
  endMonth: Scalars['GqlMonth'];
  owner: Scalars['String'];
};

export type QueryTableArgs = {
  endMonth: Scalars['GqlMonth'];
  owner?: InputMaybe<Scalars['String']>;
  startMonth: Scalars['GqlMonth'];
};
