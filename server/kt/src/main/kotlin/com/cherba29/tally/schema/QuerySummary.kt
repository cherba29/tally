package com.cherba29.tally.schema

import com.cherba29.tally.core.Month
import com.cherba29.tally.data.DataPayload
import com.cherba29.tally.statement.CombinedStatement
import com.cherba29.tally.statement.SummaryStatement
import com.cherba29.tally.statement.TransactionStatement
import com.cherba29.tally.statement.combineSummaryStatements
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.time.Clock
import kotlin.time.measureTimedValue

// startMonth is optional when max range is selected.
fun buildSummaryData(payload: DataPayload, owner: String, accountType:String, startMonth: Month?, endMonth: Month): GqlSummaryData {
  val summaryName =
    if (accountType.startsWith("/")) accountType else owner + " " + (if (accountType === owner) "SUMMARY" else accountType)
  val (result, timeTaken) = measureTimedValue {
    // TODO: replace with indexing operator.
    val monthSummaries = payload.summaries.get2(owner, summaryName)
    if (monthSummaries == null) {
      throw NotFoundException("Summary $accountType for $owner not found.")
    }
    val summaryStatements = monthSummaries.values.filter { stmt ->
      if (startMonth != null) stmt.monthRange.first in startMonth..endMonth else stmt.monthRange.last <= endMonth
    }
    if (summaryStatements.isEmpty()) {
      throw NotFoundException(
        "Summary $accountType for $owner for months [$startMonth, $endMonth] not found."
      )
    }
    val summary =
      if (summaryStatements.size == 1) summaryStatements.first() else combineSummaryStatements(summaryStatements)
    GqlSummaryData(
      statements = summary.statements.sortedWith { a, b ->
        if (a.nodeId.name < b.nodeId.name) -1 else 1
      }.map { stmt ->
        when (stmt) {
          is CombinedStatement -> stmt.toGqlStatement()
          is TransactionStatement -> stmt.toGql()
          is SummaryStatement -> stmt.toGqlStatement()
          else -> throw IllegalStateException("Unexpected statement type ${stmt.javaClass.name}")
        }
      },
      total = summary.toGql()
    )
  }
  logger.info {
    "gql '${summaryName}' summary data in ${timeTaken.inWholeMilliseconds}ms for [$startMonth, $endMonth]"
  }
  return result
}

private val logger = KotlinLogging.logger {}
