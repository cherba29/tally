package com.cherba29.tally.statement

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.MonthRange
import com.cherba29.tally.core.NodeId

class SummaryStatement(nodeId: NodeId, monthRange: MonthRange) : Statement(nodeId, monthRange) {
  val statements: MutableList<Statement> = mutableListOf()

  override val isClosed: Boolean
    get() = statements.any { statement -> statement.isClosed }

  fun addStatement(statement: Statement) {
    if (statement.monthRange != monthRange) {
      throw IllegalArgumentException(
        "${statement.nodeId} statement for months ${statement.monthRange} is being added to summary for month $monthRange"
      )
    }
    if (statement.isClosed) {
      // Does not contribute to the summary.
      return
    }
    startBalance = Balance.add(startBalance, statement.startBalance)
    endBalance = Balance.add(endBalance, statement.endBalance)
    addInFlow(statement.inFlows)
    addOutFlow(statement.outFlows)
    totalTransfers += statement.totalTransfers
    totalPayments += statement.totalPayments
    income += statement.income
    statements.add(statement)
  }

  override fun toString(): String = "${super.toString()}, statements=$statements"
}
