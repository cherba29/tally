import { gql } from 'apollo-server';

export default gql`
  scalar GqlMonth
  scalar Date

  type Query {
    accounts: [GqlAccount]
    months: [GqlMonth]
    statements: [GqlStatement] 
    summaries: [GqlSummaryStatement]
    files: [String]
  }


  type GqlAccount {
    """
    Account id/name.
    """
    name: String

    """
    Long description for the account.
    """
    description: String

    """
    Account type. Determines how account is grouped.
    TODO: This should change to enum.
    """
    type: String

    """
    Account number. Can be null or unknown for external or proxy accounts.
    """
    number: String

    """
    Month when account was open. Can be unknown.
    """
    openedOn: GqlMonth

    """
    Month when account was clased. If not set means account is still open.
    """
    closedOn: GqlMonth

    """
    List of owner ids for this account.
    """
    owners: [String]
  }


  type GqlBalance {
    """
    Amount in cents.
    """
    amount: Int
    date: Date
  }


  type GqlStatement {
    name: String
    month: GqlMonth
    isClosed: Boolean
    startBalance: GqlBalance
  }
  type GqlSummaryStatement {
    isCovered: Boolean
  }
`;
