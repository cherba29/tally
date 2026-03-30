package com.cherba29.tally.schema

import com.cherba29.tally.core.Month
import com.cherba29.tally.data.Loader.DataPayload
import com.cherba29.tally.statement.CombinedStatement
import com.cherba29.tally.statement.SummaryStatement
import com.cherba29.tally.statement.TransactionStatement
import com.cherba29.tally.statement.combineSummaryStatements
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.time.Clock

// startMonth is optional when max range is selected.
fun buildSummaryData(payload: DataPayload, owner: String, accountType:String, startMonth: Month?, endMonth: Month): GqlSummaryData {
  // TODO: replace with measureTime.
  val startTimeMs: Long = Clock.System.now().toEpochMilliseconds()
  val summaryName = if (accountType.startsWith("/")) accountType else owner + " " + (if (accountType === owner) "SUMMARY" else accountType)
  // TODO: replace with indexing operator.
  val monthSummaries = payload.summaries.get2(owner, summaryName)
  if (monthSummaries == null) {
    throw NotFoundException("Summary $accountType for $owner not found.")
  }
  val summaryStatements = monthSummaries.values.filter {
    stmt -> if (startMonth != null) stmt.month in startMonth..endMonth else stmt.month <= endMonth
  }
  if (summaryStatements.isEmpty()) {
    throw NotFoundException(
        "Summary $accountType for $owner for months [$startMonth, $endMonth] not found."
        )
  }
  val summary = if (summaryStatements.size == 1) summaryStatements.first() else combineSummaryStatements(summaryStatements)
  val result = GqlSummaryData(
    statements = summary.statements.sortedWith { a, b ->
      if (a.nodeId.name < b.nodeId.name) -1 else 1
    }.map {
      stmt ->
      when (stmt) {
        is CombinedStatement -> stmt.toGqlStatement()
        is TransactionStatement -> stmt.toGql()
        is SummaryStatement -> stmt.toGqlStatement()
        else -> throw IllegalStateException("Unexpected statement type ${stmt.javaClass.name}")
      }
    },
    total = summary.toGql()
  )
  logger.info {
    "gql '${summaryName}' summary data in ${Clock.System.now().toEpochMilliseconds() - startTimeMs}ms for [$startMonth, $endMonth]"
  }
  return result
}

private val logger = KotlinLogging.logger {}