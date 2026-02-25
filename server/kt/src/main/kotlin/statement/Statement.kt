package com.cherba29.tally.statement

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.Month
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.sign

abstract class Statement(
  // Account to which this statement belongs to.
  val account: Account,

  // Period of time for the statement
  val month: Month,

  // Recorded start balance for the statement.
  var startBalance: Balance? = null,

  // Recorded end balance for the statement.
  var endBalance: Balance? = null,

  // Total transaction inflows.
  var inFlows: Int = 0,

  // Total transaction outflows.
  var outFlows: Int = 0,

  // Amount transferred to other accounts by same owner.
  var totalTransfers: Int = 0,

  // Amount transferred to external entities.
  var totalPayments: Int = 0,

  // Amount transferred from external entities.
  var income: Int = 0,
) {
  val addSub: Int
    get() = inFlows + outFlows
  val change: Int? = startBalance?.let { s ->
    endBalance?.let { e -> e.amount - s.amount }
  }
  val percentChange: Double? = startBalance?.let {
    when (change) {
      null -> null
      0 -> 0.0
      else -> (100.0 * change) / it.amount
    }
  }
  open val annualizedPercentChange: Double? = percentChange?.let {
    val result = (1 + (it.absoluteValue) / 100).pow(12) - 1
    // Don't consider 1000% and more as meaningful annualized numbers.
    if (result < 10) 100 * it.sign * result else null
  }
  val unaccounted: Int?
    get() = change?.let { it - addSub }

  abstract val isClosed: Boolean

  // TODO: make inFlows/outFlows immutable.
  fun addInFlow(inFlowAmount: Int) {
    if (inFlowAmount > 0) {
      inFlows += inFlowAmount
    } else {
      outFlows += inFlowAmount
    }
  }

  fun addOutFlow(outFlowAmount: Int) {
    if (outFlowAmount > 0) {
      inFlows += outFlowAmount
    } else {
      outFlows += outFlowAmount
    }
  }

  fun isEmpty(): Boolean =
    startBalance == null && endBalance == null && totalTransfers == 0 && income == 0 &&
        inFlows == 0 && outFlows == 0 && totalPayments == 0
}