package com.cherba29.tally.schema

import com.cherba29.tally.core.Month
import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import kotlinx.datetime.LocalDate

data class GqlAccount(
  @param:GraphQLDescription("Account id/name.")
  val name: String,
  @param:GraphQLDescription("Long description for the account.")
  val description: String,
  @param:GraphQLDescription("Replacement for type, so that accounts are grouped.")
  val path: List<String>,
  //  TODO: This should change to enum, but maybe remove now that we have path.
  @param:GraphQLDescription("Account type. Determines how account is grouped.")
  val type: String,
  val external: Boolean,
  val summary: Boolean,
  @param:GraphQLDescription("Account number. Can be null or unknown for external or proxy accounts.")
  val number: String?,
  @param:GraphQLDescription("Month when account was open. Can be unknown.")
  val openedOn: Month?,
  @param:GraphQLDescription("Month when account was closed. If not set means account is still open.")
  val closedOn: Month?,
  @param:GraphQLDescription("List of owner ids for this account.")
  val owners: List<String>,
  val url: String,
  val address: String,
  val userName: String,
  val password: String,
  val phone: String,
)

data class GqlBalance(
  val amount: Int,
  // TODO: make LocalDate work.
  val date: LocalDate,
  val type: String
)

data class GqlStatement(
  val name: String,
  val month: Month,
  val isClosed: Boolean,
  val isCovered: Boolean,
  val isProjectedCovered: Boolean,
  val hasProjectedTransfer: Boolean,
  val startBalance: GqlBalance?,
  val endBalance: GqlBalance?,
  val inFlows: Int,
  val outFlows: Int,
  val income: Int,
  val totalPayments: Int,
  val totalTransfers: Int,
  val change: Int,
  val addSub: Int,
  val percentChange: Float,
  val annualizedPercentChange: Float,
  // TODO: this should be Int.
  val unaccounted: Float,
  val transactions: List<GqlTransaction>,
)

data class GqlSummaryStatement(
  val name: String,
  val month: Month,
  val accounts: List<String>,
  val addSub: Int,
  val income: Int,
  val change: Int,
  val inFlows: Int,
  val outFlows: Int,
  val percentChange: Float,
  val annualizedPercentChange: Float,
  val totalPayments: Int,
  val totalTransfers: Int,
  val unaccounted: Int,
  val endBalance: GqlBalance?,
  val startBalance: GqlBalance?,
)

data class GqlSummaryData(val statements: List<GqlStatement>, val total: GqlSummaryStatement)

data class GqlTableCell(
  val month: Month,
  val isClosed: Boolean,
  val addSub: Int,
  val balance: Int?,
  val isProjected: Boolean,
  val isCovered: Boolean,
  val isProjectedCovered: Boolean,
  val hasProjectedTransfer: Boolean,
  val percentChange: Float,
  val annualizedPercentChange: Float,
  val unaccounted: Int?,
  val balanced: Boolean,
)

data class GqlTableRow(
  val title: String,
  val account: GqlAccount,
  val indent: Int,
  val isSpace: Boolean,
  val isTotal: Boolean,
  val isNormal: Boolean,
  val cells: List<GqlTableCell>
)

data class GqlTable(
  val currentOwner: String,
  val owners: List<String>,
  val months: List<Month>,
  val rows: List<GqlTableRow>
)

data class GqlTransaction(
  val toAccountName: String,
  val isIncome: Boolean,
  val isExpense: Boolean,
  val balance: GqlBalance,
  val balanceFromStart: Int,
  val description: String,
)
