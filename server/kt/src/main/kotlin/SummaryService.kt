package com.cherba29.tally

import com.cherba29.tally.core.Month
import com.cherba29.tally.data.Loader
import com.cherba29.tally.schema.GqlSummaryData
import com.cherba29.tally.schema.buildSummaryData
import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import io.github.oshai.kotlinlogging.KotlinLogging

class SummaryService(val loader: Loader) : Query {
  @GraphQLDescription("Generates delta summary table between two months.")
  @Suppress("unused")
  fun summary(owner: String, accountType: String, startMonth: Month, endMonth: Month): GqlSummaryData {
    return buildSummaryData(loader.loadBudget(), owner, accountType, startMonth, endMonth)
  }

  companion object {
    private val logger = KotlinLogging.logger {}
  }
}

