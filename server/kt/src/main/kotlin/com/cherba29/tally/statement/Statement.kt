package com.cherba29.tally.statement

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.TreeNode
import com.cherba29.tally.core.MonthRange
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.sign

open class Statement(
  val treeNode: TreeNode,

  // Period of time for the statement
  val monthRange: MonthRange,

  open val isClosed: Boolean = false,

  // Recorded start balance for the statement.
  var startBalance: Balance? = null,

  // Recorded end balance for the statement.
  var endBalance: Balance? = null,

  // Total transaction inflows.
  var inFlows: Long = 0,

  // Total transaction outflows.
  var outFlows: Long = 0,

  // Amount transferred to other accounts by same owner.
  var totalTransfers: Long = 0,

  // Amount transferred to external entities.
  var totalPayments: Long = 0,

  // Amount transferred from external entities.
  var income: Long = 0,
) {
  val addSub: Long
    get() = inFlows + outFlows
  val change: Long? get() = startBalance?.let { s ->
    endBalance?.let { e -> e.amount - s.amount }
  }

  val percentChange: Double? get() = startBalance?.let {
    val changeAmount = change
    when (changeAmount) {
      null -> null
      0L -> 0.0
      else -> if (it.amount != 0L) (100.0 * changeAmount) / it.amount else null
    }
  }

  val annualizedPercentChange: Double?
    get() {
      val prctChange = percentChange ?: return null
      val numberOfMonths = monthRange.size
      val annualFrequency = 12.0 / numberOfMonths
      val result = (1 + prctChange.absoluteValue / 100).pow(annualFrequency) - 1
      // Annualized percentage change is not that meaningful if large.
      return if (result < 10) 100 * prctChange.sign * result else null
    }

  val unaccounted: Long?
    get() = change?.let { it - addSub }

  // TODO: make inFlows/outFlows immutable.
  fun addInFlow(inFlowAmount: Long) {
    if (inFlowAmount > 0) {
      inFlows += inFlowAmount
    } else {
      outFlows += inFlowAmount
    }
  }

  fun addOutFlow(outFlowAmount: Long) {
    if (outFlowAmount > 0) {
      inFlows += outFlowAmount
    } else {
      outFlows += outFlowAmount
    }
  }

  fun isEmpty(): Boolean =
    startBalance == null && endBalance == null && totalTransfers == 0L && income == 0L &&
        inFlows == 0L && outFlows == 0L && totalPayments == 0L

  override fun toString(): String {
    return "${treeNode.path.joinToString("/")} months=$monthRange isClodes=$isClosed startBalance=$startBalance endBalance=$endBalance inFlows=$inFlows outFlows=$outFlows"
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Statement) return false

    return treeNode == other.treeNode
        && monthRange == other.monthRange
        && isClosed == other.isClosed
        && startBalance == other.startBalance
        && endBalance == other.endBalance
        && inFlows == other.inFlows
        && outFlows == other.outFlows
        && totalPayments == other.totalPayments
        && totalTransfers == other.totalTransfers
        && income == other.income
  }

  override fun hashCode(): Int {
    var result = treeNode.hashCode()
    result = 31 * result + monthRange.hashCode()
    result = 31 * result + isClosed.hashCode()
    result = 31 * result + startBalance.hashCode()
    result = 31 * result + endBalance.hashCode()
    result = 31 * result + inFlows.hashCode()
    result = 31 * result + outFlows.hashCode()
    result = 31 * result + totalPayments.hashCode()
    result = 31 * result + totalTransfers.hashCode()
    result = 31 * result + income.hashCode()
    return result
  }
}
