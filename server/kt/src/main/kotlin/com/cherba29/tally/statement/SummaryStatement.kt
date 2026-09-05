package com.cherba29.tally.statement

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.MonthRange
import com.cherba29.tally.core.TreeNode
import com.cherba29.tally.data.Profile

/**
 * Extension of Statement which represents collection of statements.
 */
class SummaryStatement(
  monthRange: MonthRange,
  startBalance: Balance? = null,
  endBalance: Balance? = null,
  inFlows: Long = 0,
  outFlows: Long = 0,
  totalTransfers: Long = 0,
  totalPayments: Long = 0,
  income: Long = 0,
  /** Constituent statements making up this summary. */
  val statements: Map<TreeNode<Profile>, Statement> = mapOf()
) : Statement(
  monthRange,
  startBalance,
  endBalance,
  inFlows,
  outFlows,
  totalTransfers,
  totalPayments,
  income
) {
  override fun toString(): String = "${super.toString()}, statements=$statements"
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!super.equals(other)) return false

    other as SummaryStatement

    return statements == other.statements
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + statements.hashCode()
    return result
  }
}
