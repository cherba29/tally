package com.cherba29.tally

import com.cherba29.tally.core.Month
import com.cherba29.tally.core.rangeTo
import com.cherba29.tally.data.Loader
import com.cherba29.tally.data.builder.combineSummaryStatements
import com.cherba29.tally.schema.GqlSummaryData
import com.cherba29.tally.schema.toGqlSummaryData
import com.cherba29.tally.statement.SummaryStatement
import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.time.measureTimedValue
import kotlinx.coroutines.runBlocking

class SummaryService(val loader: Loader) : Query {
  /**
   * Computes summary data over range of months from provided monthly summaries.
   * @param owner name of the owner over which accounts summaries are computed.
   * @param accountType path to summary, concatenated with / separator.
   * @param startMonth is optional when max back range is selected.
   * @param endMonth end month until which summary is computed.
   * @return gql formatted summary data over specified period.
   */
  @GraphQLDescription("Generates delta summary table between two months.")
  fun summary(owner: String, accountType: String, startMonth: Month? = null, endMonth: Month): GqlSummaryData {
    logger.info { "summary owner=$owner, accountType=$accountType, startMonth=$startMonth, endMonth=$endMonth" }
    val (result, timeTaken) = measureTimedValue {
      try {
        val budget = runBlocking { loader.budget() }
        val summaryPath = listOf(owner) + accountType.split("/").filter { it.isNotEmpty() }
        val summaryNode = budget.tree[summaryPath]
          ?: throw NotFoundException("Summary '$accountType' for owner '$owner' not found.")
        val monthRange = startMonth..endMonth
        val summaryStatements = budget.nodeToStatement[summaryNode]!!.filter { it.key in monthRange }.values.map { it as SummaryStatement }
        if (summaryStatements.isEmpty()) {
          throw NotFoundException(
            "Summary '$accountType' for owner '$owner' for months [$startMonth, $endMonth] not found."
          )
        }
        // Multi-month queries will produce multiple summary statements which need to be combined,
        // but for single month we can simply return found single summary.
        val summary =
          if (summaryStatements.size == 1)
            summaryStatements.first()
          else
            combineSummaryStatements(budget.tree, summaryPath, summaryStatements)

        summary.toGqlSummaryData()
      } catch (e: Exception) {
        logger.error(e) {
          "Error while processing summary query owner=$owner, accountType=$accountType " +
              "startMont=$startMonth, endMonth=$endMonth"
        }
        throw e
      }
    }
    logger.info { "Summary in ${timeTaken.inWholeMilliseconds}ms" }
    return result
  }

  companion object {
    private val logger = KotlinLogging.logger {}
  }
}