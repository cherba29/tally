package com.cherba29.tally.statement

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.TreeNode
import com.cherba29.tally.core.MonthRange

class SummaryStatement(
  treeNode: TreeNode,
  monthRange: MonthRange,
  isClosed: Boolean = false,
  startBalance: Balance? = null,
  endBalance: Balance? = null,
  inFlows: Int = 0,
  outFlows: Int = 0,
  totalTransfers: Int = 0,
  totalPayments: Int = 0,
  income: Int = 0,
  val statements: List<Statement> = listOf()
) : Statement(
  treeNode, monthRange, isClosed, startBalance, endBalance, inFlows, outFlows, totalTransfers, totalPayments, income
) {
  override fun toString(): String = "${super.toString()}, statements=$statements"
}
