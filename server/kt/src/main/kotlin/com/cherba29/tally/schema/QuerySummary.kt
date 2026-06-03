package com.cherba29.tally.schema

import com.cherba29.tally.core.Month
import com.cherba29.tally.statement.SummaryStatement
import com.cherba29.tally.statement.TransactionStatement
import com.cherba29.tally.data.builder.combineSummaryStatements
import com.cherba29.tally.statement.Statement
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.time.measureTimedValue

/**
 * Converts summary statement as a summary data with substatements and a total.
 **/
fun SummaryStatement.toGqlSummaryData(): GqlSummaryData =  GqlSummaryData(
  statements = statements.sortedWith { a, b ->
    if (a.nodeId.name < b.nodeId.name) -1 else 1
  }.map { stmt ->
    when (stmt) {
      is TransactionStatement -> stmt.toGql()
      is SummaryStatement -> (stmt as Statement).toGql()
      else -> throw IllegalStateException("Unexpected statement type ${stmt.javaClass.name}")
    }
  },
  total = toGql()
)

/**
 * Computes summary data over range of months from provided monthly summaries.
 * @param summaries precomputed monthly summaries.
 * @param owner name of the owner over which accounts summaries are computed.
 * @param summaryName path to summary, concatenated with / separator.
 * @param startMonth is optional when max back range is selected.
 * @param endMonth end month until which summary is computed.
 * @return gql formatted summary data over specified perdiod.
 */
fun buildSummaryData(
  summaries: Map<List<String>, Map<Month, SummaryStatement>>,
  owner: String,
  summaryName: String,
  startMonth: Month?,
  endMonth: Month
): GqlSummaryData {
  val (result, timeTaken) = measureTimedValue {
    val monthSummaries = summaries[listOf(owner) + summaryName.split("/")]
      ?: throw NotFoundException("Summary $summaryName for $owner not found.")
    val summaryStatements = monthSummaries.values.filter { stmt ->
      if (startMonth != null)
        stmt.monthRange.first in startMonth..endMonth
      else
        stmt.monthRange.last <= endMonth
    }
    if (summaryStatements.isEmpty()) {
      throw NotFoundException(
        "Summary $summaryName for $owner for months [$startMonth, $endMonth] not found."
      )
    }
    // Multi-month queries will produce multiple summary statements which need to be combined,
    // but for single month we can simply return found single summary.
    val summary =
      if (summaryStatements.size == 1)
        summaryStatements.first()
      else
        combineSummaryStatements(summaryStatements)

    summary.toGqlSummaryData()
  }
  logger.info {
    "gql '${summaryName}' summary data in ${timeTaken.inWholeMilliseconds}ms for [$startMonth, $endMonth]"
  }
  return result
}

private val logger = KotlinLogging.logger {}
