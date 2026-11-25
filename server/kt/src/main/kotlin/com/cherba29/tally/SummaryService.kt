package com.cherba29.tally

import com.cherba29.tally.core.Month
import com.cherba29.tally.data.Loader
import com.cherba29.tally.data.builder.transfersSummary
import com.cherba29.tally.data.summary
import com.cherba29.tally.schema.GqlSummaryData
import com.cherba29.tally.schema.GqlTransfersSummary
import com.cherba29.tally.schema.toGqlSummaryData
import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import kotlin.time.measureTimedValue

class SummaryService(val loader: Loader) : Query {
  /**
   * Computes summary data over range of months from provided monthly summaries.
   * @param accountPath path to summary, concatenated with / separator.
   * @param startMonth is optional when max back range is selected.
   * @param endMonth end month until which summary is computed.
   * @return gql formatted summary data over specified period.
   */
  @GraphQLDescription("Generates delta summary table between two months.")
  fun summary(
    accountPath: String,
    startMonth: Month? = null,
    endMonth: Month
  ): GqlSummaryData {
    logger.info { "summary accountPath=$accountPath, startMonth=$startMonth, endMonth=$endMonth" }
    val (result, timeTaken) = measureTimedValue {
      try {
        val budget = runBlocking { loader.budget() }
        val (summaryNode, summaryStatement) = budget.summary(accountPath, startMonth, endMonth)
        summaryStatement.toGqlSummaryData(summaryNode.name, budget::isClosed)
      } catch (e: Exception) {
        logger.error(e) {
          "Error while processing summary query accountType=$accountPath " +
            "startMonth=$startMonth, endMonth=$endMonth"
        }
        throw e
      }
    }
    logger.info { "Summary in ${timeTaken.inWholeMilliseconds}ms" }
    return result
  }

  @GraphQLDescription("Generates sum of internal/external transfers for each month.")
  fun transfersSummary(
    accountPath: String,
    startMonth: Month? = null,
    endMonth: Month
  ): GqlTransfersSummary {
    logger.info { "transfersSummary accountPath=$accountPath, startMonth=$startMonth, endMonth=$endMonth" }
    val (result, timeTaken) = measureTimedValue {
      try {
        val budget = runBlocking { loader.budget() }
        budget.transfersSummary(accountPath, startMonth, endMonth)
      } catch (e: Exception) {
        logger.error(e) {
          "Error while processing transfersSummary query accountType=$accountPath " +
            "startMonth=$startMonth, endMonth=$endMonth"
        }
        throw e
      }
    }
    logger.info { "Computed transfer summary in ${timeTaken.inWholeMilliseconds}ms" }
    return result
  }

  companion object {
    private val logger = KotlinLogging.logger {}
  }
}
