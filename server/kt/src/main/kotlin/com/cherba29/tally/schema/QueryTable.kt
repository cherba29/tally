package com.cherba29.tally.schema

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.NodeId
import com.cherba29.tally.core.reduceTo
import com.cherba29.tally.data.Budget
import io.github.oshai.kotlinlogging.KotlinLogging

class NotFoundException(message: String) : RuntimeException(message)

data class RowEntry(
  val title: String,
  val pathId: List<String>,
  val isTotal: Boolean,
  val nodeId: NodeId?,
  val depth: Int,
)

fun buildGqlTable(payload: Budget, owner: String?, startMonth: Month, endMonth: Month): GqlTable {
  val requestedMonths = payload.months.reduceTo(startMonth..endMonth)?.sortedDescending()
  if (requestedMonths.isNullOrEmpty()) {
    throw IllegalArgumentException(
      "Bad month range, budget has ${payload.months} yet ${startMonth..endMonth} was requested"
    )
  }
  val activeNodeIds = payload.accounts.keys
  val owners = activeNodeIds.flatMap { nodeId -> nodeId.owners }.distinct().sorted()
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
  val rowOrdering = ownerTree.traverseSortedDepthDown().map { treeNode ->
    val path = if (treeNode.path.size < 2) listOf() else treeNode.path.subList(1, treeNode.path.size)
    RowEntry(
      title = treeNode.name,
      pathId = path,
      nodeId = if (treeNode.children.isNotEmpty()) null else
        NodeId(
          name = treeNode.name, isSummary = treeNode.children.isNotEmpty(),
          owners = setOf(forOwner),
          path = if (path.isEmpty()) path else path.subList(0, path.lastIndex)
        ),
      isTotal = treeNode.children.isNotEmpty(),
      depth = treeNode.path.size - 1
    )
  }

  val rows = mutableListOf<GqlTableRow>()
  for (rowEntry in rowOrdering) {
    if (rowEntry.isTotal) {  // Summary row.
      val summaryMonthMap = payload.summaries[listOf(forOwner) + rowEntry.pathId.ifEmpty { listOf("") }]
        ?: throw java.lang.IllegalArgumentException(
          "Did not find summary statement at '${
            rowEntry.pathId.joinToString("/")
          }' for owner '$forOwner' in payload summaries"
        )
      val cells: List<GqlTableCell> = requestedMonths.mapNotNull { month ->
        summaryMonthMap[month]?.toGqlTableCell()
      }
      if (cells.any { c -> !c.isClosed }) {
        val nodeId = summaryMonthMap.values.first().nodeId
        val account = payload.accounts.getOrElse(nodeId) {
          Account(nodeId, openedOn = Month(2010, 0))
        }
        rows.add(
          GqlTableRow(
            title = rowEntry.title,
            indent = rowEntry.depth,
            account = account.toGql(),
            isTotal = true,
            cells = cells,
            isSpace = false,
            isNormal = false,
          )
        )
      }
    } else {  // Account row.
      val cells = mutableListOf<GqlTableCell>()
      val nodeId = rowEntry.nodeId!!
      val account = payload.accounts.getOrElse(nodeId) {
        Account(nodeId, openedOn = Month(2010, 0))
      }
      var isClosed = true
      for (month in requestedMonths) {
        val stmt = payload.statements[nodeId]?.get(month)
          ?: throw IllegalStateException("Could not find statement for '$nodeId' for month $month")
        isClosed = isClosed && stmt.isClosed
        cells.add(stmt.toGqlTableCell())
      }
      if (!isClosed) {
        // Don't add accounts which are closed over selected timeframe.
        rows.add(
          GqlTableRow(
            title = rowEntry.title,
            indent = rowEntry.depth,
            account = account.toGql(),
            isNormal = true,
            cells = cells,
            isSpace = false,
            isTotal = false,
          )
        )
      }
    }
  }
  return GqlTable(currentOwner = forOwner, owners, requestedMonths, rows)
}

private val logger = KotlinLogging.logger {}
