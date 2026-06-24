package com.cherba29.tally.data

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Group
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.MonthRange
import com.cherba29.tally.statement.Statement
import com.cherba29.tally.statement.SummaryStatement
import com.cherba29.tally.statement.TransactionStatement

/**
 * Data representing all accounts, their transactions and totals per month.
 */
data class Budget(
  /** Period over which the budget is defined. */
  val months: MonthRange,
  /** Hierarchical structure of accounts and summaries. */
  val tree: Group,
  /** Maps leaf tree node to corresponding account. */
  val leafToAccount: Map<Group.Leaf, Account>,
  // Tree node to corresponding statement.
  // Parent nodes map to SummaryStatement and leaf nodes to TransactionStatement.
  val nodeToStatement: Map<Group, Map<Month,Statement>>,
) {
  fun getAccountNode(accountName: String) = leafToAccount.entries.find { it.value.name == accountName }?.key

  fun getMonthlyStatements(accountName: String): Map<Month, TransactionStatement>? {
    val accountNode = tree.traverseBottomUp().find { it.name == accountName }
    return nodeToStatement[accountNode]?.mapValues { it.value as TransactionStatement }
  }
  fun getOwnerSummaries(
    path: List<String>, startMonth: Month?, endMonth: Month
  ): List<SummaryStatement> {
    val node = tree[path] ?: return listOf()
    val monthSummaries = nodeToStatement[node] ?: return listOf()
    return monthSummaries.values.filter { stmt ->
      if (startMonth != null)
        stmt.monthRange.first in startMonth..endMonth
      else
        stmt.monthRange.last <= endMonth
    }.map { it as SummaryStatement }
  }
}