package com.cherba29.tally

import com.cherba29.tally.core.Month
import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import kotlinx.datetime.LocalDate

data class GqlBalance(
  val amount: Int,
  // TODO: make LocalDate work.
  val date: LocalDate,
  val type: String
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
  val endBalance: GqlBalance,
  val startBalance: GqlBalance,
)

data class GqlSummaryData(val statements: List<GqlStatement>, val total: GqlSummaryStatement)

class SummaryService : Query {
  @GraphQLDescription("Generates delta summary table between two months.")
  @Suppress("unused")
  fun summary(owner: String, startMonth: Month, endMonth: Month): GqlSummaryData {
    return GqlSummaryData(
      listOf(),
      GqlSummaryStatement(
        "stmt1",
        Month.fromString("Mar2022"),
        listOf(),
        0,
        0,
      0,
      0,
      0,
        0.0f,
        0.0f,
        0,
        0,
        0,
        GqlBalance(0, LocalDate(2022, 3, 1), "confirmed"),
        GqlBalance(0, LocalDate(2022, 3, 1),"confirmed")))
  }
}

