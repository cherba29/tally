package com.cherba29.tally.statement

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.MonthRange
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.sign

/**
 * For given month represents a snapshot of starting and ending balances with summarized activity.
 * Extensions of this specialize it over set of transactions or set of other statements.
 */
sealed class Statement(
  /** Period of time for the statement. */
  val monthRange: MonthRange,

  /** Recorded start balance for the statement. */
  val startBalance: Balance? = null,

  /** Recorded end balance for the statement. */
  val endBalance: Balance? = null,

  /** Total transaction inflows. */
  val inFlows: Long = 0,

  /** Total transaction outflows. */
  val outFlows: Long = 0,

  /** Amount transferred to other accounts by same owner. */
  val totalTransfers: Long = 0,

  /** Amount transferred to external entities. */
  val totalPayments: Long = 0,

  /** Amount transferred from external entities. */
  val income: Long = 0,
) {
  /** Total known change based on transactions. */
  val addSub: Long = inFlows + outFlows

  /** Total change based on starting and ending balance. */
  val change: Long? = startBalance?.let { s ->
    endBalance?.let { e -> e.amount - s.amount }
  }

  /**
   * Change should generally be same as addSub,
   * any difference is considered to be unaccounted amount.
   */
  val unaccounted: Long? = change?.let { it - addSub }

  /** Total change as a percentage of starting balance. */
  val percentChange: Double? = startBalance?.let {
    when (val changeAmount = change) {
      null -> null
      0L -> 0.0
      else -> if (it.amount != 0L) (100.0 * changeAmount) / it.amount else null
    }
  }

  /** Same as percentChange but at annualized rate. */
  val annualizedPercentChange: Double? = run {
    val prctChange = percentChange ?: return@run null
    val numberOfMonths = monthRange.size
    val annualFrequency = 12.0 / numberOfMonths
    val result = (1 + prctChange.absoluteValue / 100).pow(annualFrequency) - 1
    // Annualized percentage change is not that meaningful if large.
    if (result < 10) 100 * prctChange.sign * result else null
  }

  /** Marker if there has been any activity of this month and account. */
  val isEmpty: Boolean =
    startBalance == null && endBalance == null && totalTransfers == 0L && income == 0L &&
        inFlows == 0L && outFlows == 0L && totalPayments == 0L

  override fun toString(): String {
    return "months=$monthRange startBalance=$startBalance endBalance=$endBalance inFlows=$inFlows outFlows=$outFlows"
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Statement) return false

    return monthRange == other.monthRange
        && startBalance == other.startBalance
        && endBalance == other.endBalance
        && inFlows == other.inFlows
        && outFlows == other.outFlows
        && totalPayments == other.totalPayments
        && totalTransfers == other.totalTransfers
        && income == other.income
  }

  override fun hashCode(): Int {
    var result = monthRange.hashCode()
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
