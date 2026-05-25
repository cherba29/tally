package com.cherba29.tally.schema

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.NodeId
import com.cherba29.tally.data.Budget
import io.github.oshai.kotlinlogging.KotlinLogging

class NotFoundException(message: String) : RuntimeException(message)

data class RowEntry(
  val title: String,
  val pathId: String,
  val isTotal: Boolean,
  val nodeId: NodeId?,
  val depth: Int,
)

// Build a tree based on paths.
private fun buildAccountPathTree(accounts: List<NodeId>): Map<List<String>, Set<List<String>>> {
  val tree = mutableMapOf<List<String>, MutableSet<List<String>>>()
  for (account in accounts) {
    if (account.path.isEmpty()) {
      logger.warn { "${account.name} has no path skipping" }
      continue
    }
    val path = account.path
    var entry = path + listOf(account.name)
    for (sub in path.size downTo 0) {
      val subPath = path.slice(0..sub-1)
      val subPathId = subPath
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
  var nodesToProcess = mutableListOf(listOf<String>())
  while (nodesToProcess.isNotEmpty()) {
    val subTreeId = nodesToProcess.removeFirst()
    val children = tree[subTreeId]
    val account = if (children != null) null
                  else if (subTreeId.isEmpty()) null
                  else nameToAccount[subTreeId[subTreeId.lastIndex]]
    entries.add(
      RowEntry(
        title = if (subTreeId.isNotEmpty()) subTreeId[subTreeId.lastIndex] else owner,
        pathId = subTreeId.joinToString("/"),
        nodeId = account,
        isTotal = children?.isNotEmpty() ?: (account == null),
        depth = subTreeId.size
      )
    )
    if (children != null) {
      // Add in front as we want to process children next, in dept-first fashion.
      val childrenList = children.sortedWith(lexicographicalComparator).toMutableList()
      childrenList.addAll(nodesToProcess)
      nodesToProcess = childrenList
    }
  }
  return entries
}

private val lexicographicalComparator = Comparator<List<String>> { list1, list2 ->
  val size1 = list1.size
  val size2 = list2.size
  val minSize = minOf(size1, size2)

  for (i in 0 until minSize) {
    val cmp = list1[i].compareTo(list2[i])
    if (cmp != 0) return@Comparator cmp
  }
  // If all compared elements are equal, the shorter list comes first
  size1.compareTo(size2)
}

fun buildGqlTable(payload: Budget, owner: String?, startMonth: Month, endMonth: Month): GqlTable {
  val months = payload.months.filter { m -> m <= endMonth && startMonth <= m }.sortedDescending()
  val activeAccounts = payload.accounts.keys
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
      val summaryMonthMap = payload.summaries[forOwner, entry.pathId]
        ?: throw java.lang.IllegalArgumentException(
          "Did not find summary statement at '${entry.pathId}' for owner '$forOwner' in payload summaries"
        )
      val cells: List<GqlTableCell> = months.mapNotNull { month ->
        summaryMonthMap[month]?.toGqlTableCell()
      }
      if (cells.any { c -> !c.isClosed }) {
        val nodeId = summaryMonthMap.values.first().nodeId
        val account = payload.accounts.getOrElse(nodeId) {
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
      val account = payload.accounts.getOrElse(nodeId) {
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
