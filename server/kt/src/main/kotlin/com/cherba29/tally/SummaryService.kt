package com.cherba29.tally

import com.cherba29.tally.core.Month
import com.cherba29.tally.core.TreeNode
import com.cherba29.tally.core.rangeTo
import com.cherba29.tally.core.reduceTo
import com.cherba29.tally.data.Loader
import com.cherba29.tally.data.builder.MonthRangeSummaryStatementBuilder
import com.cherba29.tally.schema.GqlMonthTransferSummary
import com.cherba29.tally.schema.GqlSummaryData
import com.cherba29.tally.schema.GqlTransfersSummary
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
   * @param accountPath path to summary, concatenated with / separator.
   * @param startMonth is optional when max back range is selected.
   * @param endMonth end month until which summary is computed.
   * @return gql formatted summary data over specified period.
   */
  @GraphQLDescription("Generates delta summary table between two months.")
  fun summary(accountPath: String, startMonth: Month? = null, endMonth: Month): GqlSummaryData {
    logger.info { "summary accountPath=$accountPath, startMonth=$startMonth, endMonth=$endMonth" }
    val (result, timeTaken) = measureTimedValue {
      try {
        val budget = runBlocking { loader.budget() }
        val summaryPath = accountPath.split("/").filter { it.isNotEmpty() }
        val summaryNode = budget.tree[summaryPath]
          ?: throw NotFoundException("Summary '$accountPath' not found.")
        val monthRange = startMonth..endMonth
        val summaryStatements = budget.nodeToStatement[summaryNode]!!.filter { it.key in monthRange }.mapValues { it.value as SummaryStatement }
        if (summaryStatements.isEmpty()) {
          throw NotFoundException(
            "Summary '$accountPath' for months [$startMonth, $endMonth] not found."
          )
        }
        val builder = MonthRangeSummaryStatementBuilder()
        for (summaryStatement in summaryStatements.values) {
          for (subStatement in summaryStatement.statements) {
            // Do not include closed statements in the summary.
            if (!subStatement.isClosed) {
              builder.addStatement(subStatement)
            }
          }
        }
        builder.build(summaryNode).toGqlSummaryData()
      } catch (e: Exception) {
        logger.error(e) {
          "Error while processing summary query accountType=$accountPath " +
              "startMont=$startMonth, endMonth=$endMonth"
        }
        throw e
      }
    }
    logger.info { "Summary in ${timeTaken.inWholeMilliseconds}ms" }
    return result
  }

  @GraphQLDescription("Generates sum of internal/external transfers for each month.")
  fun transfersSummary(accountPath: String, startMonth: Month? = null, endMonth: Month): GqlTransfersSummary {
    logger.info { "summary accountPath=$accountPath, startMonth=$startMonth, endMonth=$endMonth" }
    val (result, timeTaken) = measureTimedValue {
      try {
        val budget = runBlocking { loader.budget() }

        val treePath = accountPath.split("/").filter { it.isNotEmpty() }
        val treeNode = budget.tree[treePath]
          ?: throw NotFoundException("'$accountPath' not found.")

        val monthlyStatements = budget.nodeToStatement[treeNode]
          ?: throw IllegalStateException("Could not find statements for $accountPath")
        val ascMonthList = monthlyStatements.keys.sorted()
        val limitMonths = (ascMonthList.first()..ascMonthList.last()).reduceTo(startMonth..endMonth)
          ?: throw NotFoundException("Not statements for $accountPath for month range $startMonth..$endMonth")
        val summaries = mutableListOf<GqlMonthTransferSummary>()
        var totalInternalTransfers = 0L
        var totalExternalTransfers = 0L
        for (month in ascMonthList) {
          val statement = monthlyStatements[month] ?: continue
          totalInternalTransfers += statement.totalTransfers
          totalExternalTransfers += statement.income
          summaries.add(GqlMonthTransferSummary(
            internalTransfers = statement.totalTransfers,
            externalTransfers = statement.income,
            totalInternalTransfers,
            totalExternalTransfers
          ))
        }
        GqlTransfersSummary(
          months = limitMonths.reversed(),
          data = summaries.asReversed().subList(0, limitMonths.size)
        )
      } catch (e: Exception) {
        logger.error(e) {
          "Error while processing summary query accountType=$accountPath " +
              "startMont=$startMonth, endMonth=$endMonth"
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
