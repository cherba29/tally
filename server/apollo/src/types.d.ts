export type Maybe<T> = T | null;
export type Exact<T extends { [key: string]: unknown }> = { [K in keyof T]: T[K] };
/** All built-in and custom scalars, mapped to their actual values */
export type Scalars = {
  ID: string;
  String: string;
  Boolean: boolean;
  Int: number;
  Float: number;
  /** The `Upload` scalar type represents a file upload. */
  Upload: any;
};

export type Query = {
  __typename?: 'Query';
  /** Test Message.  */
  testMessage: Scalars['String'];
  foo: Scalars['String'];
  accounts?: Maybe<Array<Maybe<Account>>>;
  months?: Maybe<Array<Maybe<RawMonth>>>;
};

export type Account = {
  __typename?: 'Account';
  name: Scalars['String'];
  description: Scalars['String'];
  type: Scalars['String'];
  number: Scalars['String'];
  openedOn: Scalars['String'];
  closedOn: Scalars['String'];
  owners?: Maybe<Array<Scalars['String']>>;
};

export type RawMonth = {
  __typename?: 'RawMonth';
  year: Scalars['Int'];
  month: Scalars['Int'];
};

export enum CacheControlScope {
  Public = 'PUBLIC',
  Private = 'PRIVATE'
}

