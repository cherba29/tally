package com.cherba29.tally

import com.cherba29.tally.core.Month
import com.cherba29.tally.core.rangeTo
import com.cherba29.tally.core.reduceTo
import com.cherba29.tally.data.Loader
import com.cherba29.tally.data.builder.MonthRangeSummaryStatementBuilder
import com.cherba29.tally.schema.GqlMonthTransferSummary
import com.cherba29.tally.schema.GqlSummaryData
import com.cherba29.tally.schema.GqlTransfersSummary
import com.cherba29.tally.schema.toGqlSummaryData
import com.cherba29.tally.statement.SummaryStatement
import com.cherba29.tally.utils.IrregularCashFlow
import com.cherba29.tally.utils.asRounded
import com.cherba29.tally.utils.asRoundedPercent
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
          for ((treeNode, subStatement) in summaryStatement.statements) {
            // Do not include closed statements in the summary.
            if (!subStatement.isClosed) {
              builder.addStatement(treeNode,subStatement.monthRange.first, subStatement)
            }
          }
        }
        builder.build().toGqlSummaryData(summaryNode.name)
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
        val ascMonthList = monthlyStatements.filterValues { !it.isClosed }.keys.sorted()
        val summaries = mutableMapOf<Month, GqlMonthTransferSummary>()

        val cashFlow = IrregularCashFlow()
        for (month in ascMonthList) {
          val statement = monthlyStatements[month] ?: continue
          val internalTransfers = statement.totalTransfers
          val externalTransfers = statement.income + statement.totalPayments
          cashFlow.add(internalTransfers, externalTransfers)

          summaries[month] = GqlMonthTransferSummary(
            internalTransfers,
            externalTransfers,
            totalMonthTransfers = internalTransfers + externalTransfers,
            totalInternalTransfers = cashFlow.totalContributions,
            totalInternalTransfersPrct = cashFlow.contributionFraction.asRoundedPercent(1),
            totalInternalTransfersAnnualPrct = cashFlow.effectiveRateOfReturnOnContributions().asRoundedPercent(2),
            weightedInternalAge = cashFlow.weightedContributionsAge().asRounded(2),
            totalExternalTransfers = cashFlow.totalGains,
            totalExternalTransfersPrct = cashFlow.gainsFraction.asRoundedPercent(1),
            totalExternalTransfersAnnualPrct = cashFlow.effectiveRateOfReturnOnGains().asRoundedPercent(2),
            weightedExternalAge = cashFlow.weightedGainsAge().asRounded(2),
            totalTransfers = cashFlow.total,
            totalAnnualPrct = cashFlow.effectiveRateOfReturn().asRoundedPercent(2),
            weightedAge = cashFlow.weightedAverageAmountAge().asRounded(2),
            unaccounted = (statement.startBalance?.amount
              ?: 0) - cashFlow.total + internalTransfers + externalTransfers,
          )
        }

        // TODO: apply limiting months upfront using start balance.
        val limitMonths = (ascMonthList.first()..ascMonthList.last()).reduceTo(startMonth..endMonth)
          ?: throw NotFoundException("Not statements for $accountPath for month range $startMonth..$endMonth")
        GqlTransfersSummary(
          months = limitMonths.reversed(),
          data = summaries.filterKeys { it in limitMonths }.toSortedMap().values.reversed()
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
