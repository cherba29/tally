import { gql } from 'apollo-server';

export default gql`
  scalar GqlMonth
  scalar Date

  type Query {
    """
    Test Message. 
    """
    testMessage: String!
    foo: String!
    accounts: [GqlAccount]
    months: [GqlMonth]
    statements: [GqlStatement] 
    summaries: [GqlSummaryStatement]
    files: [String!]
  
  }
  type GqlAccount {
    name: String!
    description: String!
    type: String!
    number: String!
    openedOn: String!
    closedOn: String!
    owners: [String!]
  }
  type GqlBalance {
    """
    Amount in cents.
    """
    amount: Int!
    date: Date!
  }
  type GqlStatement {
    name: String!
    month: GqlMonth!
    isClosed: Boolean!
    startBalance: GqlBalance!
  }
  type GqlSummaryStatement {
    isCovered: Boolean!
  }
`;
