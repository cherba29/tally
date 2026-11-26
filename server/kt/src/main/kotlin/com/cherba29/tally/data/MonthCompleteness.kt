package com.cherba29.tally.data

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.Month
import com.cherba29.tally.statement.SummaryStatement
import com.cherba29.tally.statement.TransactionStatement
import com.cherba29.tally.utils.TreeNode
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Computes how complete a given month is for an owner (0.0 to 100.0).
 *
 * A month is 100% complete if and only if every open active account has confirmed starting and
 * ending balances, all of its transactions are confirmed, and unaccounted is 0 for every open
 * account as well as every non-closed summary up to the owner root.
 *
 * Projected values score strictly higher than unset values (0.5 vs 0.0) and strictly lower than
 * confirmed values (1.0). Non-zero unaccounted amounts reduce completeness proportionally to the
 * relative size of the unaccounted amount.
 */
object MonthCompleteness {
  private const val BALANCE_WEIGHT = 0.40
  private const val TRANSACTIONS_WEIGHT = 0.30
  private const val RECONCILIATION_WEIGHT = 0.30

  private const val CONFIRMED_SCORE = 1.0
  private const val PROJECTED_SCORE = 0.5
  private const val UNSET_SCORE = 0.0

  fun scoreBalanceType(type: Balance.Type): Double =
    when (type) {
      Balance.Type.CONFIRMED -> CONFIRMED_SCORE
      Balance.Type.PROJECTED -> PROJECTED_SCORE
    }

  fun scoreBalance(balance: Balance?): Double = if (balance == null) UNSET_SCORE else scoreBalanceType(balance.type)

  /**
   * Computes completeness in `[0.0, 1.0]` for a single open leaf account's [TransactionStatement].
   */
  fun accountCompleteness(statement: TransactionStatement): Double {
    val balanceScore = 0.5 * scoreBalance(statement.startBalance) + 0.5 * scoreBalance(statement.endBalance)

    val transactionsScore = if (statement.transactions.isNotEmpty()) {
      var weightedSum = 0.0
      var totalAbs = 0L
      for (tx in statement.transactions) {
        val absAmt = abs(tx.balance.amount)
        totalAbs += absAmt
        weightedSum += absAmt * scoreBalanceType(tx.balance.type)
      }
      if (totalAbs > 0L) {
        weightedSum / totalAbs.toDouble()
      } else {
        statement.transactions.sumOf { scoreBalanceType(it.balance.type) } / statement.transactions.size.toDouble()
      }
    } else {
      if (statement.startBalance != null && statement.endBalance != null && statement.unaccounted == 0L) {
        balanceScore
      } else {
        0.0
      }
    }

    val reconciliationScore = scoreReconciliation(
      unaccounted = statement.unaccounted,
      startAmount = statement.startBalance?.amount,
      endAmount = statement.endBalance?.amount,
      change = statement.change,
      inFlows = statement.inFlows,
      outFlows = statement.outFlows
    )

    return (
      BALANCE_WEIGHT * balanceScore +
        TRANSACTIONS_WEIGHT * transactionsScore +
        RECONCILIATION_WEIGHT * reconciliationScore
    ).coerceIn(0.0, 1.0)
  }

  /**
   * Computes completeness percentage (`0.0f..100.0f`, rounded to 1 decimal place) for [ownerTree]
   * in [month]. Strictly returns `100.0f` only when every open active account and every non-closed
   * summary up to [ownerTree] is fully confirmed and reconciled (`unaccounted == 0`).
   */
  fun monthCompletenessPercent(
    budget: Budget,
    ownerTree: TreeNode<Profile>,
    month: Month
  ): Float {
    var openAccountCount = 0
    var totalAccountScore = 0.0
    var allPerfect = true

    for (leaf in ownerTree.traverseLeaves()) {
      if (leaf.data.isInactive || budget.isClosed(leaf, month)) continue
      val stmt = budget.nodeToStatement[leaf]?.get(month) as? TransactionStatement ?: continue
      openAccountCount++
      val score = accountCompleteness(stmt)
      totalAccountScore += score
      if (!isFullyCompleteStatement(stmt)) {
        allPerfect = false
      }
    }

    if (openAccountCount == 0) {
      return 0.0f
    }

    var rawScore = totalAccountScore / openAccountCount.toDouble()

    // Ensure non-zero unaccounted in any non-closed summary up to the owner root also penalizes
    // completeness and prevents reaching 100%. (A null summary.unaccounted occurs when balances
    // are unset across the subtree, which is already accounted for in the leaf scores above.)
    var worstSummaryFactor = 1.0
    for (node in ownerTree.traverseDepthDown()) {
      if (node.children.isEmpty() || budget.isClosed(node, month)) continue
      val summary = budget.nodeToStatement[node]?.get(month) as? SummaryStatement ?: continue
      val unaccounted = summary.unaccounted
      if (unaccounted == null) {
        allPerfect = false
      } else if (unaccounted != 0L) {
        allPerfect = false
        val summaryRecon = scoreReconciliation(
          unaccounted = unaccounted,
          startAmount = summary.startBalance?.amount,
          endAmount = summary.endBalance?.amount,
          change = summary.change,
          inFlows = summary.inFlows,
          outFlows = summary.outFlows
        )
        if (summaryRecon < worstSummaryFactor) {
          worstSummaryFactor = summaryRecon
        }
      }
    }

    if (worstSummaryFactor < 1.0) {
      rawScore *= (1.0 - RECONCILIATION_WEIGHT) + RECONCILIATION_WEIGHT * worstSummaryFactor
    }

    if (allPerfect) {
      return 100.0f
    }
    if (rawScore <= 0.0) {
      return 0.0f
    }

    val roundedTenths = (rawScore * 1000.0).roundToInt() / 10.0
    return roundedTenths.coerceIn(0.1, 99.9).toFloat()
  }

  private fun isFullyCompleteStatement(stmt: TransactionStatement): Boolean =
    stmt.startBalance?.type == Balance.Type.CONFIRMED &&
      stmt.endBalance?.type == Balance.Type.CONFIRMED &&
      stmt.unaccounted == 0L &&
      stmt.transactions.all { it.balance.type == Balance.Type.CONFIRMED }

  private fun scoreReconciliation(
    unaccounted: Long?,
    startAmount: Long?,
    endAmount: Long?,
    change: Long?,
    inFlows: Long,
    outFlows: Long
  ): Double {
    if (unaccounted == null) return 0.0
    if (unaccounted == 0L) return 1.0

    val absUnaccounted = abs(unaccounted).toDouble()
    val grossFlow = abs(inFlows).toDouble() + abs(outFlows).toDouble()
    val scale = maxOf(
      abs(startAmount ?: 0L).toDouble(),
      abs(endAmount ?: 0L).toDouble(),
      abs(change ?: 0L).toDouble(),
      grossFlow
    )
    if (scale <= 0.0) return 0.0
    return (1.0 - (absUnaccounted / scale)).coerceIn(0.0, 1.0)
  }
}
