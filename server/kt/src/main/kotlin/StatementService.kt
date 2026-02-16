package com.cherba29.tally

import com.cherba29.tally.core.Month
import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import graphql.schema.DataFetchingEnvironment
import kotlinx.datetime.LocalDate

data class GqlTransaction(
  val toAccountName: String,
  val isIncome: Boolean,
  val isExpense: Boolean,
  val balance: GqlBalance,
  val balanceFromStart: Int,
  val description: String,
)

data class GqlStatement(
  val name: String,
  val month: Month,
  val isClosed: Boolean,
  val isCovered: Boolean,
  val isProjectedCovered: Boolean,
  val hasProjectedTransfer: Boolean,
  val startBalance: GqlBalance,
  val endBalance: GqlBalance,
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

class StatementService : Query {
  @GraphQLDescription("Returns a monthly statement for given account.")
  @Suppress("unused")
  fun statement(owner:String, account: String, month: String, dfe: DataFetchingEnvironment): GqlStatement {
    return GqlStatement(
      name = "${account}-${month}-${owner}",
      month = Month(2022, 3),
      isClosed = false,
      isCovered = true,
      isProjectedCovered = true,
      hasProjectedTransfer = false,
      startBalance = GqlBalance(0, LocalDate(2022, 3, 1), "confirmed"),
      endBalance = GqlBalance(0, LocalDate(2022, 3, 1),"confirmed"),
      inFlows = 0,
      outFlows = 0,
      income = 0,
      totalPayments = 0,
      totalTransfers = 0,
      change = 0,
      addSub = 0,
      percentChange = 0.0f,
      annualizedPercentChange = 0.0f,
      unaccounted = 0.0f,
      transactions = listOf(),
    )
  }
}