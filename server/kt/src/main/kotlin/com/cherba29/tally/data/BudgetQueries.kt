package com.cherba29.tally.data

import com.cherba29.tally.NotFoundException
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.rangeTo
import com.cherba29.tally.core.reduceTo
import com.cherba29.tally.data.builder.MonthRangeSummaryStatementBuilder
import com.cherba29.tally.recurring.RecurringGroup
import com.cherba29.tally.schema.GqlTable
import com.cherba29.tally.schema.GqlTableRow
import com.cherba29.tally.schema.toGql
import com.cherba29.tally.schema.toGqlTableCell
import com.cherba29.tally.statement.SummaryStatement
import com.cherba29.tally.statement.TransactionStatement
import com.cherba29.tally.utils.TreeNode

data class SummaryQueryResult(
  val node: TreeNode<Profile>,
  val statement: SummaryStatement
)

data class StatementQueryResult(
  val node: TreeNode.Leaf<Profile>,
  val statement: TransactionStatement,
  val isClosed: Boolean
)

/**
 * Computes an aggregated [SummaryStatement] for [accountPath] over `[startMonth, endMonth]`,
 * excluding closed sub-statements.
 */
fun Budget.summary(
  accountPath: String,
  startMonth: Month? = null,
  endMonth: Month
): SummaryQueryResult {
  val summaryPath = accountPath.split("/").filter { it.isNotEmpty() }
  val summaryNode = tree[summaryPath]
    ?: throw NotFoundException("Summary '$accountPath' not found.")
  val monthRange = startMonth..endMonth
  val summaryStatements = nodeToStatement[summaryNode]
    ?.filter { it.key in monthRange }
    ?.mapValues { it.value as SummaryStatement }
    .orEmpty()
  if (summaryStatements.isEmpty()) {
    throw NotFoundException(
      "Summary '$accountPath' for months [$startMonth, $endMonth] not found."
    )
  }
  val builder = MonthRangeSummaryStatementBuilder()
  for (summaryStatement in summaryStatements.values) {
    for ((treeNode, subStatement) in summaryStatement.statements) {
      // Do not include closed statements in the summary.
      if (!isClosed(treeNode, subStatement.monthRange.first)) {
        builder.addStatement(treeNode, subStatement.monthRange.first, subStatement)
      }
    }
  }
  return SummaryQueryResult(summaryNode, builder.build())
}

/**
 * Looks up a monthly [TransactionStatement] for a leaf account at [accountPath] and [month].
 */
fun Budget.statement(
  accountPath: String,
  month: Month
): StatementQueryResult {
  val accountNode = tree[accountPath.split("/").filter { it.isNotEmpty() }]
    ?: throw NotFoundException("Did not find account '$accountPath'")
  if (accountNode !is TreeNode.Leaf) {
    throw NotFoundException("'$accountPath' is not an account path")
  }
  val account = accountNode.data.account
    ?: throw IllegalStateException("No corresponding account for $accountNode")
  val statement = nodeToStatement[accountNode]?.get(month) as? TransactionStatement
    ?: throw NotFoundException("Did not find statement for month '$month' for account '$accountPath'")
  return StatementQueryResult(
    node = accountNode,
    statement = statement,
    isClosed = account.isClosed(statement.monthRange.first)
  )
}

/**
 * Looks up a detected [RecurringGroup] by [groupId] synchronously.
 */
fun Budget.recurringGroup(groupId: String): RecurringGroup =
  recurringIndex.findGroupById(groupId)
    ?: throw NotFoundException("Recurring transaction group '$groupId' not found.")

/**
 * Suspends until the recurring index is ready and looks up [groupId].
 */
suspend fun Budget.awaitRecurringGroup(groupId: String): RecurringGroup =
  awaitRecurringIndex().findGroupById(groupId)
    ?: throw NotFoundException("Recurring transaction group '$groupId' not found.")

/**
 * Builds the tabular ledger view for [owner] (or the first available owner when null/blank)
 * across `[startMonth, endMonth]`.
 */
fun Budget.table(
  owner: String?,
  startMonth: Month,
  endMonth: Month
): GqlTable {
  val requestedMonths = months.reduceTo(startMonth..endMonth)?.sortedDescending()
  if (requestedMonths.isNullOrEmpty()) {
    throw IllegalArgumentException(
      "Bad month range, budget has $months yet ${startMonth..endMonth} was requested"
    )
  }
  val owners = tree.children
    .map { it.name }
    .sorted()
  val forOwner = if (owner.isNullOrEmpty()) {
    owners.firstOrNull { it.isNotEmpty() }
      ?: throw IllegalArgumentException("No owner is specified and one cannot be derived from accounts")
  } else {
    owner
  }
  val ownerTree = tree[forOwner]
    ?: throw IllegalArgumentException(
      "Owner $forOwner is not in account tree, known owners ${tree.children.joinToString { it.name }}"
    )

  val rows = mutableListOf<GqlTableRow>()
  for (treeNode in ownerTree.traverseDepthDown()) {
    val monthMap = nodeToStatement[treeNode]
      ?: throw IllegalArgumentException(
        "Did not find monthly statements at '${treeNode.path.joinToString("/")}'"
      )

    val cells = requestedMonths.map { month ->
      monthMap[month]?.toGqlTableCell(isClosed(treeNode, month))
        ?: throw IllegalStateException(
          "Missing statement for ${treeNode.path.joinToString("/")} for month $month, " +
            "only available ${monthMap.keys}"
        )
    }

    if (cells.any { c -> !c.isClosed }) {
      val account = treeNode.data.account
      rows.add(
        GqlTableRow(
          id = treeNode.path.joinToString("/"),
          title = treeNode.name,
          indent = treeNode.path.size - 1,
          account = account?.toGql(treeNode.data.isExternal, treeNode.children.isNotEmpty()),
          isTotal = treeNode.children.isNotEmpty(),
          cells = cells,
          isSpace = false,
          isInactive = treeNode.data.isInactive,
          isNormal = treeNode.children.isEmpty()
        )
      )
    }
  }
  return GqlTable(currentOwner = forOwner, owners, requestedMonths, rows)
}
