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
};

export type GqlAccount = {
  __typename?: 'GqlAccount';
  address?: Maybe<Scalars['String']['output']>;
  /** Month when account was clased. If not set means account is still open. */
  closedOn?: Maybe<Scalars['GqlMonth']['output']>;
  /** Long description for the account. */
  description?: Maybe<Scalars['String']['output']>;
  external?: Maybe<Scalars['Boolean']['output']>;
  /** Account id/name. */
  name?: Maybe<Scalars['String']['output']>;
  /** Account number. Can be null or unknown for external or proxy accounts. */
  number?: Maybe<Scalars['String']['output']>;
  /** Month when account was open. Can be unknown. */
  openedOn?: Maybe<Scalars['GqlMonth']['output']>;
  /** List of owner ids for this account. */
  owners?: Maybe<Array<Maybe<Scalars['String']['output']>>>;
  password?: Maybe<Scalars['String']['output']>;
  /** Replacement for type, so that accounts are grouped. */
  path?: Maybe<Array<Maybe<Scalars['String']['output']>>>;
  phone?: Maybe<Scalars['String']['output']>;
  summary?: Maybe<Scalars['Boolean']['output']>;
  /**
   * Account type. Determines how account is grouped.
   * TODO: This should change to enum.
   */
  type?: Maybe<Scalars['String']['output']>;
  url?: Maybe<Scalars['String']['output']>;
  userName?: Maybe<Scalars['String']['output']>;
};

export type GqlBalance = {
  __typename?: 'GqlBalance';
  /** Amount in cents. */
  amount?: Maybe<Scalars['Int']['output']>;
  date?: Maybe<Scalars['Date']['output']>;
  type?: Maybe<Scalars['String']['output']>;
};

export type GqlStatement = {
  __typename?: 'GqlStatement';
  addSub?: Maybe<Scalars['Int']['output']>;
  annualizedPercentChange?: Maybe<Scalars['Float']['output']>;
  change?: Maybe<Scalars['Int']['output']>;
  endBalance?: Maybe<GqlBalance>;
  hasProjectedTransfer?: Maybe<Scalars['Boolean']['output']>;
  inFlows?: Maybe<Scalars['Int']['output']>;
  income?: Maybe<Scalars['Int']['output']>;
  isClosed?: Maybe<Scalars['Boolean']['output']>;
  isCovered?: Maybe<Scalars['Boolean']['output']>;
  isProjectedCovered?: Maybe<Scalars['Boolean']['output']>;
  month?: Maybe<Scalars['GqlMonth']['output']>;
  name?: Maybe<Scalars['String']['output']>;
  outFlows?: Maybe<Scalars['Int']['output']>;
  percentChange?: Maybe<Scalars['Float']['output']>;
  startBalance?: Maybe<GqlBalance>;
  totalPayments?: Maybe<Scalars['Int']['output']>;
  totalTransfers?: Maybe<Scalars['Int']['output']>;
  transactions?: Maybe<Array<Maybe<GqlTransaction>>>;
  unaccounted?: Maybe<Scalars['Float']['output']>;
};

export type GqlSummaryData = {
  __typename?: 'GqlSummaryData';
  statements?: Maybe<Array<Maybe<GqlStatement>>>;
  total?: Maybe<GqlSummaryStatement>;
};

export type GqlSummaryStatement = {
  __typename?: 'GqlSummaryStatement';
  accounts?: Maybe<Array<Maybe<Scalars['String']['output']>>>;
  addSub?: Maybe<Scalars['Int']['output']>;
  annualizedPercentChange?: Maybe<Scalars['Float']['output']>;
  change?: Maybe<Scalars['Int']['output']>;
  endBalance?: Maybe<GqlBalance>;
  inFlows?: Maybe<Scalars['Int']['output']>;
  income?: Maybe<Scalars['Int']['output']>;
  month?: Maybe<Scalars['GqlMonth']['output']>;
  name?: Maybe<Scalars['String']['output']>;
  outFlows?: Maybe<Scalars['Int']['output']>;
  percentChange?: Maybe<Scalars['Float']['output']>;
  startBalance?: Maybe<GqlBalance>;
  totalPayments?: Maybe<Scalars['Int']['output']>;
  totalTransfers?: Maybe<Scalars['Int']['output']>;
  unaccounted?: Maybe<Scalars['Int']['output']>;
};

export type GqlTable = {
  __typename?: 'GqlTable';
  currentOwner?: Maybe<Scalars['String']['output']>;
  months?: Maybe<Array<Maybe<Scalars['GqlMonth']['output']>>>;
  owners?: Maybe<Array<Maybe<Scalars['String']['output']>>>;
  rows?: Maybe<Array<Maybe<GqlTableRow>>>;
};

export type GqlTableCell = {
  __typename?: 'GqlTableCell';
  addSub?: Maybe<Scalars['Int']['output']>;
  annualizedPercentChange?: Maybe<Scalars['Float']['output']>;
  balance?: Maybe<Scalars['Int']['output']>;
  balanced?: Maybe<Scalars['Boolean']['output']>;
  hasProjectedTransfer?: Maybe<Scalars['Boolean']['output']>;
  isClosed?: Maybe<Scalars['Boolean']['output']>;
  isCovered?: Maybe<Scalars['Boolean']['output']>;
  isProjected?: Maybe<Scalars['Boolean']['output']>;
  isProjectedCovered?: Maybe<Scalars['Boolean']['output']>;
  month?: Maybe<Scalars['GqlMonth']['output']>;
  percentChange?: Maybe<Scalars['Float']['output']>;
  unaccounted?: Maybe<Scalars['Int']['output']>;
};

export type GqlTableRow = {
  __typename?: 'GqlTableRow';
  account?: Maybe<GqlAccount>;
  cells?: Maybe<Array<Maybe<GqlTableCell>>>;
  indent?: Maybe<Scalars['Int']['output']>;
  isNormal?: Maybe<Scalars['Boolean']['output']>;
  isSpace?: Maybe<Scalars['Boolean']['output']>;
  isTotal?: Maybe<Scalars['Boolean']['output']>;
  title?: Maybe<Scalars['String']['output']>;
};

export type GqlTransaction = {
  __typename?: 'GqlTransaction';
  balance?: Maybe<GqlBalance>;
  balanceFromStart?: Maybe<Scalars['Int']['output']>;
  description?: Maybe<Scalars['String']['output']>;
  isExpense?: Maybe<Scalars['Boolean']['output']>;
  isIncome?: Maybe<Scalars['Boolean']['output']>;
  toAccountName?: Maybe<Scalars['String']['output']>;
};

export type Query = {
  __typename?: 'Query';
  files?: Maybe<Array<Maybe<Scalars['String']['output']>>>;
  statement?: Maybe<GqlStatement>;
  summary?: Maybe<GqlSummaryData>;
  table?: Maybe<GqlTable>;
};


export type QueryStatementArgs = {
  account: Scalars['String']['input'];
  month: Scalars['GqlMonth']['input'];
  owner: Scalars['String']['input'];
};


export type QuerySummaryArgs = {
  accountType: Scalars['String']['input'];
  endMonth: Scalars['GqlMonth']['input'];
  owner: Scalars['String']['input'];
  startMonth?: InputMaybe<Scalars['GqlMonth']['input']>;
};


export type QueryTableArgs = {
  endMonth: Scalars['GqlMonth']['input'];
  owner?: InputMaybe<Scalars['String']['input']>;
  startMonth: Scalars['GqlMonth']['input'];
};
