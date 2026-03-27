package com.cherba29.tally.statement

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Month
import com.cherba29.tally.utils.Map3

// TODO: add tests for this class.
class SummaryStatementAggregator {
  // Map of owner -> 'summary name' -> month -> 'summary statement'.
  val summaryStatements = Map3<SummaryStatement>()
  private val summaryAccounts: MutableMap<String, Account> = mutableMapOf()

  fun addStatement(summaryName: String, owner: String, statement: Statement) {
    val summaryAccount = getAccount(summaryName, owner, statement.account.path, statement.month)

    val accountMonthSummaryStatement = summaryStatements.getDefault(
      owner, summaryAccount.name, statement.month.toString()
    ) {
      SummaryStatement(summaryAccount, statement.month, startMonth = statement.month)
    }
    accountMonthSummaryStatement.addStatement(statement)
  }

  private fun getAccount(name: String, owner: String, path: List<String>, month: Month): Account {
    val key = "$owner - $name"
    var account = summaryAccounts[key]
    if (account == null) {
      account = Account(
        name,
        owners = listOf(owner),
        path = path.slice(0..path.size - 2),
        openedOn = month
      )
      summaryAccounts[key] = account
    }
    return account
  }

  // Make sure totals are computed for parent summary accounts up the path to the root.
  fun propagateUpThePath2() {
    // Build a multi-root tree based on account paths for each owner.
    val tree: MutableMap<String, MutableSet<String>> = mutableMapOf()  // node -> set of children.
    val owners: MutableSet<String> = mutableSetOf()
    for (account in summaryAccounts.values) {
      for (owner in account.owners) {
        owners.add(owner)
        if (!account.name.startsWith('/')) continue
        val path = account.path
        var entry = "/" + owner + account.name
        for (sub in path.size downTo 0) {
          val subPath = path.slice(0..sub - 1)
          val subPathId = "/" + owner + "/" + subPath.joinToString("/")
          var subTreeEntry = tree[subPathId]
          if (subTreeEntry == null) {
            subTreeEntry = mutableSetOf()
            tree[subPathId] = subTreeEntry
          }
          if (subPathId != entry) {  // Make sure root does not reference itself.
            subTreeEntry.add(entry)
          }
          entry = subPathId
        }
      }
    }
    // For each owner bottom up, build up summaries.
    for (owner in owners) {
      val ownerRoot = "/$owner/"
      for (node in traverseBottomUp(ownerRoot, tree)) {
        val fullPath = node.split('/')
        val summaryId = "/" + fullPath.subList(2, fullPath.size).joinToString("/")
        // skip this is root node it does not need to be added to anything.
        if (summaryId == "/") continue
        val monthlyStatements = summaryStatements.get2(owner, summaryId)
          ?: throw IllegalStateException(
            "$node has no monthly statements, [$owner, $summaryId] key not found."
          )  // Should never happen.

        val parentSummaryId = '/' + fullPath.subList(2, fullPath.lastIndex).joinToString("/")
        for (monthlyStatement in monthlyStatements.values) {
          addStatement(parentSummaryId, owner, monthlyStatement)
        }
      }
    }
  }

  companion object {
    private fun traverseBottomUp(root: String, tree: Map<String, Set<String>>): Sequence<String> = sequence {
      for (child in tree[root] ?: setOf()) {
        yieldAll(traverseBottomUp(child, tree))
      }
      yield(root)
    }
  }
}
