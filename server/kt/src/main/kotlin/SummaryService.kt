package com.cherba29.tally

import com.cherba29.tally.core.Month
import com.cherba29.tally.schema.GqlBalance
import com.cherba29.tally.schema.GqlSummaryData
import com.cherba29.tally.schema.GqlSummaryStatement
import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import kotlinx.datetime.LocalDate

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
        GqlBalance(0, LocalDate(2022, 3, 1), "confirmed")
      )
    )
  }
}

