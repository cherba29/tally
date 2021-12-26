import { gql } from 'apollo-server';

export default gql`
  scalar GqlMonth
  scalar Date

  type Query {
    files: [String]
    budget: GqlBudget
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

    external: Boolean

    summary: Boolean

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

    url: String

    address: String

    userName: String

    password: String

    phone: String
  }

  type GqlBalance {
    """
    Amount in cents.
    """
    amount: Int
    date: Date
    type: String
  }

  type GqlTransaction {
    toAccountName: String
    isIncome: Boolean
    isExpense: Boolean
    balance: GqlBalance
    balanceFromStart: Int
    balanceFromEnd: Int
    description: String
  }

  type GqlStatement {
    name: String
    month: GqlMonth
    isClosed: Boolean
    isCovered: Boolean
    isProjectedCovered: Boolean
    hasProjectedTransfer: Boolean
    startBalance: GqlBalance
    endBalance: GqlBalance
    inFlows: Float
    outFlows: Float
    income: Float
    totalPayments: Float
    totalTransfers: Float
    percentChange: Float
    unaccounted: Float
    transactions: [GqlTransaction]
  }

  type GqlSummaryStatement {
    name: String
    month: GqlMonth
    accounts: [String]
    addSub: Float
    change: Float
    inFlows: Float
    outFlows: Float
    percentChange: Float
    totalPayments: Float
    totalTransfers: Float
    unaccounted: Float
    endBalance: GqlBalance
    startBalance: GqlBalance
  }

  type GqlBudget {
    accounts: [GqlAccount]
    months: [GqlMonth]
    statements: [GqlStatement]
    summaries: [GqlSummaryStatement]
  }
`;
