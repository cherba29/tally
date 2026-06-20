package com.cherba29.tally.schema

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.NodeId
import com.cherba29.tally.core.reduceTo
import com.cherba29.tally.data.Budget
import com.cherba29.tally.statement.SummaryStatement
import com.cherba29.tally.statement.TransactionStatement
import io.github.oshai.kotlinlogging.KotlinLogging


fun buildGqlTable(payload: Budget, owner: String?, startMonth: Month, endMonth: Month): GqlTable {
  val requestedMonths = payload.months.reduceTo(startMonth..endMonth)?.sortedDescending()
  if (requestedMonths.isNullOrEmpty()) {
    throw IllegalArgumentException(
      "Bad month range, budget has ${payload.months} yet ${startMonth..endMonth} was requested"
    )
  }
  val owners = payload.tree.children.map { it.name }.sorted()
  val forOwner = if (owner.isNullOrEmpty()) {
    owners.firstOrNull { !it.isEmpty() }
      ?: throw IllegalArgumentException("No owner is specified and one cannot be derived from accounts")
  } else {
    owner
  }
  val ownerTree = payload.tree[forOwner]
    ?: throw java.lang.IllegalArgumentException(
      "Owner $forOwner is not in account tree, known owners ${payload.tree.children.joinToString { it.name }}"
    )

  val rows = mutableListOf<GqlTableRow>()
  for (treeNode in ownerTree.traverseSortedDepthDown()) {
    val path = if (treeNode.path.size < 2) listOf() else treeNode.path.subList(1, treeNode.path.size)

    val account = if (treeNode.children.isEmpty()) {
      payload.leafToAccount[treeNode]
        ?: throw java.lang.IllegalArgumentException(
          "Could not find account for ${treeNode.path.joinToString("/")}"
        )
    } else {
      // Summaries don't have associated account, create a dummy.
      // TODO: instead of dummy return null.
      Account(
        NodeId(path.joinToString("/"), isSummary = true, owners = setOf(forOwner),
          path = if (path.size > 1) path.subList(0, path.size-1) else listOf("")),
        openedOn = Month(2010, 0)
      )
    }

    // TODO: Move this logic into budget.
    val monthMap = if (treeNode.children.isNotEmpty()) {  // Summary row.
      payload.getOwnerMonthlySummaries(forOwner, path)
    } else {
      payload.nodeToStatement[treeNode]
    } ?: throw java.lang.IllegalArgumentException(
      "Did not find monthly statements at '${treeNode.path.joinToString("/")}'"
    )

    val cells = requestedMonths.map { month ->
      val monthlyStatement = monthMap[month]
      // Extension functions are not polymorphic.
      // TODO: refactor so not to do manual polymorphism here.
      when (monthlyStatement) {
        is TransactionStatement -> monthlyStatement.toGqlTableCell()
        is SummaryStatement -> monthlyStatement.toGqlTableCell()
        else -> throw IllegalStateException("Could not find statement for '${account.nodeId}' for month $month")
      }
    }

    if (cells.any { c -> !c.isClosed }) {
      rows.add(
        GqlTableRow(
          title = treeNode.name,
          indent = treeNode.path.size - 1,
          account = account.toGql(),
          isTotal = treeNode.children.isNotEmpty(),
          cells = cells,
          isSpace = false,
          isNormal = treeNode.children.isEmpty(),
        )
      )
    }
  }
  return GqlTable(currentOwner = forOwner, owners, requestedMonths, rows)
}

private val logger = KotlinLogging.logger {}
