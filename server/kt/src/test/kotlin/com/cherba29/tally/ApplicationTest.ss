╔═ SDL route test ═╗
"Schema for Tally data"
schema @contact(description : "Report issues on github.", name : "Tally", url : "https://github.com/cherba29/tally"){
  query: Query
}

"Provides contact information of the owner responsible for this subgraph schema."
directive @contact(description: String!, name: String!, url: String!) on SCHEMA

"Marks the field, argument, input field or enum value as deprecated"
directive @deprecated(
    "The reason for the deprecation"
    reason: String = "No longer supported"
  ) on FIELD_DEFINITION | ARGUMENT_DEFINITION | ENUM_VALUE | INPUT_FIELD_DEFINITION

"Directs the executor to include this field or fragment only when the `if` argument is true"
directive @include(
    "Included when true."
    if: Boolean!
  ) on FIELD | FRAGMENT_SPREAD | INLINE_FRAGMENT

"Indicates an Input Object is a OneOf Input Object."
directive @oneOf on INPUT_OBJECT

"Directs the executor to skip this field or fragment when the `if` argument is true."
directive @skip(
    "Skipped when true."
    if: Boolean!
  ) on FIELD | FRAGMENT_SPREAD | INLINE_FRAGMENT

"Exposes a URL that specifies the behaviour of this scalar."
directive @specifiedBy(
    "The URL that specifies the behaviour of this scalar."
    url: String!
  ) on SCALAR

type GqlAccount {
  address: String!
  "Month when account was closed. If not set means account is still open."
  closedOn: GqlMonth
  "Long description for the account."
  description: String!
  external: Boolean!
  "Account id/name."
  name: String!
  "Account number. Can be null or unknown for external or proxy accounts."
  number: String
  "Month when account was open. Can be unknown."
  openedOn: GqlMonth
  "List of owner ids for this account."
  owners: [String!]!
  password: String!
  "Replacement for type, so that accounts are grouped."
  path: [String!]!
  phone: String!
  summary: Boolean!
  url: String!
  userName: String!
}

type GqlBalance {
  amount: Long!
  date: Date!
  desc: String!
  type: String!
}

type GqlStatement {
  addSub: Long!
  annualizedPercentChange: Float!
  change: Long!
  endBalance: GqlBalance
  hasProjectedTransfer: Boolean!
  inFlows: Long!
  income: Long!
  isClosed: Boolean!
  isCovered: Boolean!
  isProjectedCovered: Boolean!
  month: GqlMonth!
  name: String!
  outFlows: Long!
  percentChange: Float!
  startBalance: GqlBalance
  totalPayments: Long!
  totalTransfers: Long!
  transactions: [GqlTransaction!]!
  unaccounted: Long!
}

type GqlSummaryData {
  statements: [GqlStatement!]!
  total: GqlSummaryStatement!
}

type GqlSummaryStatement {
  accounts: [String!]!
  addSub: Long!
  annualizedPercentChange: Float!
  change: Long!
  endBalance: GqlBalance
  inFlows: Long!
  income: Long!
  month: GqlMonth!
  name: String!
  outFlows: Long!
  percentChange: Float!
  startBalance: GqlBalance
  totalPayments: Long!
  totalTransfers: Long!
  unaccounted: Long!
}

type GqlTable {
  currentOwner: String!
  months: [GqlMonth!]!
  owners: [String!]!
  rows: [GqlTableRow!]!
}

type GqlTableCell {
  addSub: Long!
  annualizedPercentChange: Float!
  balance: Long
  balanced: Boolean!
  hasProjectedTransfer: Boolean!
  isClosed: Boolean!
  isCovered: Boolean!
  isProjected: Boolean!
  isProjectedCovered: Boolean!
  month: GqlMonth!
  percentChange: Float!
  unaccounted: Long
}

type GqlTableRow {
  account: GqlAccount!
  cells: [GqlTableCell!]!
  indent: Int!
  isNormal: Boolean!
  isSpace: Boolean!
  isTotal: Boolean!
  title: String!
}

type GqlTransaction {
  balance: GqlBalance!
  balanceFromStart: Long!
  description: String!
  isExpense: Boolean!
  isIncome: Boolean!
  toAccountName: String!
}

type Query {
  hello: String!
  "Returns a monthly statement for given account."
  statement(account: String!, month: GqlMonth!, owner: String!): GqlStatement!
  "Generates delta summary table between two months."
  summary(accountType: String!, endMonth: GqlMonth!, owner: String!, startMonth: GqlMonth): GqlSummaryData!
  "Generates full tally table in given month range."
  table(endMonth: GqlMonth!, owner: String, startMonth: GqlMonth!): GqlTable!
}

"Date representation in YYYY-MM-DD format."
scalar Date

"Month representation in XxxYYYY format."
scalar GqlMonth

"Long 64-bit integer."
scalar Long
╔═ [end of file] ═╗
