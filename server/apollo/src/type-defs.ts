import { gql } from 'apollo-server';

export default gql`
  type Query {
    """
    Test Message. 
    """
    testMessage: String!
    foo: String!
    accounts: [Account]
    months: [RawMonth] 
  }
  type Account {
    name: String!
    description: String!
    type: String!
    number: String!
    openedOn: String!
    closedOn: String!
    owners: [String!]
  }
  type RawMonth {
    year: Int!
    month: Int!
  }
`;
