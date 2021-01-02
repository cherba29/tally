export type Maybe<T> = T | null;
export type Exact<T extends { [key: string]: unknown }> = { [K in keyof T]: T[K] };
/** All built-in and custom scalars, mapped to their actual values */
export type Scalars = {
  ID: string;
  String: string;
  Boolean: boolean;
  Int: number;
  Float: number;
  /** Month representation in XxxYYYY format. */
  GqlMonth: any;
  /** Date representation in YYYY-MM-DD format. */
  Date: any;
  /** The `Upload` scalar type represents a file upload. */
  Upload: any;
};



export type Query = {
  __typename?: 'Query';
  accounts?: Maybe<Array<Maybe<GqlAccount>>>;
  months?: Maybe<Array<Maybe<Scalars['GqlMonth']>>>;
  statements?: Maybe<Array<Maybe<GqlStatement>>>;
  summaries?: Maybe<Array<Maybe<GqlSummaryStatement>>>;
  files?: Maybe<Array<Maybe<Scalars['String']>>>;
};

export type GqlAccount = {
  __typename?: 'GqlAccount';
  /** Account id/name. */
  name?: Maybe<Scalars['String']>;
  /** Long description for the account. */
  description?: Maybe<Scalars['String']>;
  /**
   * Account type. Determines how account is grouped.
   * TODO: This should change to enum.
   */
  type?: Maybe<Scalars['String']>;
  /** Account number. Can be null or unknown for external or proxy accounts. */
  number?: Maybe<Scalars['String']>;
  /** Month when account was open. Can be unknown. */
  openedOn?: Maybe<Scalars['GqlMonth']>;
  /** Month when account was clased. If not set means account is still open. */
  closedOn?: Maybe<Scalars['GqlMonth']>;
  /** List of owner ids for this account. */
  owners?: Maybe<Array<Maybe<Scalars['String']>>>;
};

export type GqlBalance = {
  __typename?: 'GqlBalance';
  /** Amount in cents. */
  amount?: Maybe<Scalars['Int']>;
  date?: Maybe<Scalars['Date']>;
};

export type GqlStatement = {
  __typename?: 'GqlStatement';
  name?: Maybe<Scalars['String']>;
  month?: Maybe<Scalars['GqlMonth']>;
  isClosed?: Maybe<Scalars['Boolean']>;
  startBalance?: Maybe<GqlBalance>;
};

export type GqlSummaryStatement = {
  __typename?: 'GqlSummaryStatement';
  isCovered?: Maybe<Scalars['Boolean']>;
};

export enum CacheControlScope {
  Public = 'PUBLIC',
  Private = 'PRIVATE'
}

