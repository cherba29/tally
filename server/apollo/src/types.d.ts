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
  /** Test Message.  */
  testMessage: Scalars['String'];
  foo: Scalars['String'];
  accounts?: Maybe<Array<Maybe<GqlAccount>>>;
  months?: Maybe<Array<Maybe<Scalars['GqlMonth']>>>;
  statements?: Maybe<Array<Maybe<GqlStatement>>>;
  summaries?: Maybe<Array<Maybe<GqlSummaryStatement>>>;
  files?: Maybe<Array<Scalars['String']>>;
};

export type GqlAccount = {
  __typename?: 'GqlAccount';
  name: Scalars['String'];
  description: Scalars['String'];
  type: Scalars['String'];
  number: Scalars['String'];
  openedOn: Scalars['String'];
  closedOn: Scalars['String'];
  owners?: Maybe<Array<Scalars['String']>>;
};

export type GqlBalance = {
  __typename?: 'GqlBalance';
  /** Amount in cents. */
  amount: Scalars['Int'];
  date: Scalars['Date'];
};

export type GqlStatement = {
  __typename?: 'GqlStatement';
  name: Scalars['String'];
  month: Scalars['GqlMonth'];
  isClosed: Scalars['Boolean'];
  startBalance: GqlBalance;
};

export type GqlSummaryStatement = {
  __typename?: 'GqlSummaryStatement';
  isCovered: Scalars['Boolean'];
};

export enum CacheControlScope {
  Public = 'PUBLIC',
  Private = 'PRIVATE'
}

