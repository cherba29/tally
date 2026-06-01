package com.cherba29.tally.schema

import com.cherba29.tally.core.Month
import com.cherba29.tally.statement.SummaryStatement
import com.cherba29.tally.statement.TransactionStatement
import com.cherba29.tally.data.builder.combineSummaryStatements
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.time.measureTimedValue

/** Converts summary statement as a summary data with substatements and a total. **/
fun SummaryStatement.toGqlSummaryData(): GqlSummaryData =  GqlSummaryData(
  statements = statements.sortedWith { a, b ->
    if (a.nodeId.name < b.nodeId.name) -1 else 1
  }.map { stmt ->
    when (stmt) {
      is TransactionStatement -> stmt.toGql()
      is SummaryStatement -> stmt.toGqlStatement()
      else -> throw IllegalStateException("Unexpected statement type ${stmt.javaClass.name}")
    }
  },
  total = toGql()
)

// startMonth is optional when max range is selected.
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
