import gql from 'graphql-tag';

export default gql`
  scalar GqlMonth
  scalar Date

  type Query {
    files: [String]
    budget: GqlBudget
    table(owner: String): GqlTable
    summary(owner: String!, accountType: String!, month: GqlMonth!): GqlSummaryData
    statement(owner: String!, account: String!, month: GqlMonth!): GqlStatement
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
    inFlows: Int
    outFlows: Int
    income: Int
    totalPayments: Int
    totalTransfers: Int
    change: Int
    addSub: Int
    percentChange: Float
    unaccounted: Float
    transactions: [GqlTransaction]
  }

  type GqlSummaryStatement {
    name: String
    month: GqlMonth
    accounts: [String]
    addSub: Int
    income: Int
    change: Int
    inFlows: Int
    outFlows: Int
    percentChange: Float
    totalPayments: Int
    totalTransfers: Int
    unaccounted: Int
    endBalance: GqlBalance
    startBalance: GqlBalance
  }

  type GqlSummaryData {
    statements: [GqlStatement]
    total: GqlSummaryStatement
  }

  type GqlBudget {
    accounts: [GqlAccount]
    months: [GqlMonth]
    statements: [GqlStatement]
    summaries: [GqlSummaryStatement]
  }

  # Below are types matching views.
  type GqlTableCell {
    isClosed: Boolean
    addSub: Int
    balance: Int
    isProjected: Boolean
    isCovered: Boolean
    isProjectedCovered: Boolean
    hasProjectedTransfer: Boolean
    percentChange: Float
    unaccounted: Int
    balanced: Boolean
  }

  type GqlTableRow {
    title: String
    account: GqlAccount
    isSpace: Boolean
    isTotal: Boolean
    isNormal: Boolean
    cells: [GqlTableCell]
  }

  type GqlTable {
    currentOwner: String
    owners: [String]
    months: [GqlMonth]
    rows: [GqlTableRow]
  }
`;
