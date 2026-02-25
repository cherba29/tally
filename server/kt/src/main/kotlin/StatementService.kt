package com.cherba29.tally

import com.cherba29.tally.core.Month
import com.cherba29.tally.schema.GqlBalance
import com.cherba29.tally.schema.GqlStatement
import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import graphql.schema.DataFetchingEnvironment
import kotlinx.datetime.LocalDate

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