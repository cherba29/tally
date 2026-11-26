package com.cherba29.tally.data

import com.cherba29.tally.core.Month
import com.cherba29.tally.core.MonthRange
import com.cherba29.tally.recurring.RecurringTransactionIndex
import com.cherba29.tally.statement.Statement
import com.cherba29.tally.utils.TreeNode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking

/**
 * Order-independent fingerprint of canonical transfers used to skip recurring recomputation
 * on balance-only edits.
 */
data class TransfersFingerprint(
  val count: Int,
  val sumHash: Long,
  val xorHash: Long
)

/**
 * Data representing all accounts, their transactions and totals per month.
 */
data class Budget(
  /** Period over which the budget is defined. */
  val months: MonthRange,
  /** Hierarchical structure of accounts and summaries. */
  val tree: TreeNode<Profile>,
  // Tree node to corresponding statement.
  // Parent nodes map to SummaryStatement and leaf nodes to TransactionStatement.
  val nodeToStatement: Map<TreeNode<Profile>, Map<Month, Statement>>,
  /** Deferred recurring transaction index (may complete asynchronously on live reloads). */
  internal val recurringIndexDeferred: Deferred<RecurringTransactionIndex> = EMPTY_RECURRING_DEFERRED,
  /** Fingerprint of canonical transfers used to build [recurringIndexDeferred]. */
  internal val transfersFingerprint: TransfersFingerprint? = null
) {
  constructor(
    months: MonthRange,
    tree: TreeNode<Profile>,
    nodeToStatement: Map<TreeNode<Profile>, Map<Month, Statement>>,
    recurringIndex: RecurringTransactionIndex
  ) : this(
    months = months,
    tree = tree,
    nodeToStatement = nodeToStatement,
    recurringIndexDeferred = CompletableDeferred(recurringIndex),
    transfersFingerprint = null
  )

  private val isClosedCache = mutableMapOf<TreeNode<Profile>, MutableMap<Month, Boolean>>()

  /**
   * Suspends until the recurring transaction index is ready without blocking a thread.
   */
  suspend fun awaitRecurringIndex(): RecurringTransactionIndex = recurringIndexDeferred.await()

  /**
   * Synchronous access to the recurring transaction index (instant when already completed).
   */
  @OptIn(ExperimentalCoroutinesApi::class)
  val recurringIndex: RecurringTransactionIndex
    get() = if (recurringIndexDeferred.isCompleted && !recurringIndexDeferred.isCancelled) {
      recurringIndexDeferred.getCompleted()
    } else {
      runBlocking { recurringIndexDeferred.await() }
    }

  fun getAccountNode(accountName: String) = tree.traverseLeaves().find { it.data.account?.name == accountName }

  /**
   * Checks if node is closed.
   * It is closed if its corresponding account is closed, or all its children are closed.
   */
  fun isClosed(
    treeNode: TreeNode<Profile>,
    month: Month
  ): Boolean =
    isClosedCache.getOrPut(treeNode) { mutableMapOf() }.getOrPut(month) {
      when (treeNode) {
        is TreeNode.Leaf -> treeNode.data.account?.isClosed(month)
          ?: throw IllegalStateException("No matching account for $treeNode")
        is TreeNode.Root,
        is TreeNode.Branch -> {
          treeNode.children.all { isClosed(it, month) }
        }
      }
    }

  companion object {
    internal val EMPTY_RECURRING_DEFERRED: Deferred<RecurringTransactionIndex> =
      CompletableDeferred(RecurringTransactionIndex.EMPTY)
  }
}
