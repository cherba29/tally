package com.cherba29.tally.schema

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Month
import com.cherba29.tally.data.DataPayload
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.time.Clock

class NotFoundException(message: String) : RuntimeException(message)

data class RowEntry(
  val title: String,
  val id: String,
  val isTotal: Boolean,
  val account: Account?,
  val depth: Int,
)

/** Sequence for presentation summaries and accounts based on their names. */
private fun sequenceStatements(owner: String, accounts: List<Account>): List<RowEntry> {
  // Build a tree based on paths.
  val tree = mutableMapOf<String, MutableSet<String>>()
  for (account in accounts) {
    if (account.path.isEmpty()) {
      logger.warn { "${account.name} has no path skipping" }
      continue
    }
    val path = account.path
    var entry = "/${path.joinToString("/")}/${account.name}"
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
  // Iterate over tree in depth-first fashion to sequence rows representing the tree.
  val nameToAccount = mutableMapOf<String, Account>()
  for (account in accounts) {
    nameToAccount[account.name] = account
  }
  val entries = mutableListOf<RowEntry>()
  var nodesToProcess = mutableListOf("/")
  while (nodesToProcess.isNotEmpty()) {
    val subTreeId = nodesToProcess.removeFirst()
    val subPath = subTreeId.split("/").filter { it.isNotEmpty() }
    val children = tree[subTreeId]
    val account = if (children != null) null
                  else if (subPath.isEmpty()) null
                  else nameToAccount[subPath[subPath.lastIndex]]
    entries.add(
      RowEntry(
        title = if (subPath.isNotEmpty()) subPath[subPath.lastIndex] else owner,
        id = subTreeId,
        account = account,
        isTotal = children?.isNotEmpty() ?: (account == null),
        depth = subPath.size
      )
    )
    if (children != null) {
      // Add in front as we want to process children next, in dept-first fashion.
      val childrenList = children.toMutableList()
      childrenList.addAll(nodesToProcess)
      nodesToProcess = childrenList
    }
  }
  return entries
}

fun buildGqlTable(payload: DataPayload, owner: String?, startMonth: Month, endMonth: Month): GqlTable {
  val startTimeMs: Long = Clock.System.now().toEpochMilliseconds()
  val months = payload.budget.months.filter { m -> m <= endMonth && startMonth <= m }.sortedDescending()
  val activeAccounts = payload.budget.findActiveAccounts()
  val owners = activeAccounts.map { account -> account.owners }.flatten().distinct().sorted()
  val forOwner = if (owner == null || owner.isEmpty()) owners.first() else owner
  val accounts = activeAccounts.filter { a -> a.owners.contains(forOwner) }

  val rows = mutableListOf<GqlTableRow>()

  val ordering = sequenceStatements(forOwner, accounts)
  //logger.info { "Statement ordering is $ordering" }

  for (entry in ordering) {
    if (entry.isTotal) {
      val summaryMonthMap = payload.summaries.get2(forOwner, entry.id)
        ?: throw NotFoundException("Did not find summary statement for ['$forOwner', '${entry.id}']")
      val cells: List<GqlTableCell> = months.mapNotNull { month ->
        summaryMonthMap[month.toString()]?.toGqlTableCell()
      }
      if (cells.any { c -> !c.isClosed }) {
        val account = summaryMonthMap.values.first().account
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
      val account = entry.account!!
      var isClosed = true
      for (month in months) {
        val stmt = payload.statements[account.name]?.get(month.toString())
        isClosed = isClosed && (stmt?.isClosed ?: false)
        cells.add(stmt!!.toGqlTableCell())
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
  logger.info { "gql table $endMonth--$startMonth in ${Clock.System.now().toEpochMilliseconds() - startTimeMs}ms" }
  return GqlTable(
    currentOwner = forOwner, owners, months, rows
  )
}

private val logger = KotlinLogging.logger {}
