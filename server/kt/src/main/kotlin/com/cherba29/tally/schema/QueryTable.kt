package com.cherba29.tally.schema

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.NodeId
import com.cherba29.tally.data.DataPayload
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.time.Clock

class NotFoundException(message: String) : RuntimeException(message)

data class RowEntry(
  val title: String,
  val pathId: String,
  val isTotal: Boolean,
  val nodeId: NodeId?,
  val depth: Int,
)

// Build a tree based on paths.
private fun buildAccountPathTree(accounts: List<NodeId>): Map<String, Set<String>> {
  val tree = mutableMapOf<String, MutableSet<String>>()
  for (account in accounts) {
    if (account.path.isEmpty()) {
      logger.warn { "${account.name} has no path skipping" }
      continue
    }
    val path = account.path
    var entry = account.toString()
    for (sub in path.size downTo 0) {
      val subPath = path.slice(0..sub-1)
      val subPathId = "/" + subPath.joinToString("/")
      var subTreeEntry = tree[subPathId]
      if (subTreeEntry == null) {
        subTreeEntry = mutableSetOf()
        tree[subPathId] = subTreeEntry
      }
      subTreeEntry.add(entry)
      entry = subPathId
    }
  }
  return tree
}

/** Sequence for presentation summaries and accounts based on their names. */
private fun sequenceStatements(owner: String, accounts: List<NodeId>): List<RowEntry> {
  val tree = buildAccountPathTree(accounts)
  val nameToAccount = accounts.associateBy { it.name }
  // Iterate over tree in sorted depth-first fashion to sequence rows representing the tree.
  val entries = mutableListOf<RowEntry>()
  var nodesToProcess = mutableListOf("/")
  while (nodesToProcess.isNotEmpty()) {
    val subTreeId = nodesToProcess.removeFirst()
    // TODO: use nodeId.path and list structure instead of splitting.
    val subPath = subTreeId.split("/").filter { it.isNotEmpty() }
    val children = tree[subTreeId]
    val account = if (children != null) null
                  else if (subPath.isEmpty()) null
                  else nameToAccount[subPath[subPath.lastIndex]]
    entries.add(
      RowEntry(
        title = if (subPath.isNotEmpty()) subPath[subPath.lastIndex] else owner,
        pathId = subTreeId,
        nodeId = account,
        isTotal = children?.isNotEmpty() ?: (account == null),
        depth = subPath.size
      )
    )
    if (children != null) {
      // Add in front as we want to process children next, in dept-first fashion.
      val childrenList = children.sorted().toMutableList()
      childrenList.addAll(nodesToProcess)
      nodesToProcess = childrenList
    }
  }
  return entries
}

fun buildGqlTable(payload: DataPayload, owner: String?, startMonth: Month, endMonth: Month): GqlTable {
  val months = payload.budget.months.filter { m -> m <= endMonth && startMonth <= m }.sortedDescending()
  val activeAccounts = payload.budget.accounts.keys
  val owners = activeAccounts.map { account -> account.owners }.flatten().distinct().sorted()
  val forOwner = if (owner == null || owner.isEmpty()) {
    owners.firstOrNull { !it.isEmpty() }
      ?: throw IllegalArgumentException("No owner is specified and one cannot be derived from accounts")
  } else {
    owner
  }
  val accounts = activeAccounts.filter { a -> a.owners.contains(forOwner) }

  val rows = mutableListOf<GqlTableRow>()

  val ordering = sequenceStatements(forOwner, accounts)

  for (entry in ordering) {
    if (entry.isTotal) {
      val summaryMonthMap = payload.summaries.get2(forOwner, entry.pathId)
        ?: throw java.lang.IllegalArgumentException(
          "Did not find summary statement at '${entry.pathId}' for owner '$forOwner' in payload summaries"
        )
      val cells: List<GqlTableCell> = months.mapNotNull { month ->
        summaryMonthMap[month.toString()]?.toGqlTableCell()
      }
      if (cells.any { c -> !c.isClosed }) {
        val nodeId = summaryMonthMap.values.first().nodeId
        val account = payload.budget.accounts.getOrElse(nodeId) {
          Account(nodeId, openedOn = Month(2010, 0))
        }
        rows.add(
          GqlTableRow(
            title = entry.title,
            indent = entry.depth,
            account = account.toGql(),
            isTotal = true,
            cells = cells,
            isSpace = false,
            isNormal = false,
          )
        )
      }
    } else {
      val cells = mutableListOf<GqlTableCell>()
      val nodeId = entry.nodeId!!
      val account = payload.budget.accounts.getOrElse(nodeId) {
        Account(nodeId, openedOn = Month(2010, 0))
      }
      var isClosed = true
      for (month in months) {
        val stmt = payload.statements[nodeId]?.get(month)
          ?: throw IllegalStateException("Could not find statement for '$nodeId' for month $month")
        isClosed = isClosed && stmt.isClosed
        cells.add(stmt.toGqlTableCell())
      }
      if (!isClosed) {
        // Don't add accounts which are closed over selected timeframe.
        rows.add(
          GqlTableRow(
            title = entry.title,
            indent = entry.depth,
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
  return GqlTable(currentOwner = forOwner, owners, months, rows)
}

private val logger = KotlinLogging.logger {}
