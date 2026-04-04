package com.cherba29.tally.statement

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.MonthRange
import com.cherba29.tally.core.NodeId

class CombinedStatement(nodeId: NodeId, monthRange: MonthRange) : Statement(nodeId, monthRange) {
  companion object {
    fun fromStatements(
      nodeId: NodeId,
      monthRange: MonthRange,
      statements: Map<Month, Statement>
    ): CombinedStatement {
      val combined = CombinedStatement(nodeId, monthRange)
      for (currentMonth in monthRange) {
        val stmt: Statement = makeProxyStatement(
          nodeId,
          currentMonth,
          statements[currentMonth],
          statements[currentMonth.previous()],
          statements[currentMonth.next()]
        )
        combined.startBalance = Balance.pickMinDate(combined.startBalance, stmt.startBalance)
        combined.endBalance = Balance.pickMaxDate(combined.endBalance, stmt.endBalance)
        combined.addInFlow(stmt.inFlows)
        combined.addOutFlow(stmt.outFlows)
        combined.totalTransfers += stmt.totalTransfers
        combined.totalPayments += stmt.totalPayments
        combined.income += stmt.income
      }
      return combined
    }

    private fun makeProxyStatement(
      nodeId: NodeId,
      month: Month,
      currStmt: Statement?,
      prevStmt: Statement?,
      nextStmt: Statement?
    ): Statement {
      val stmt = currStmt ?: Statement(nodeId, month..month)
      if (stmt.startBalance == null) {
        stmt.startBalance = prevStmt?.endBalance ?: Balance(0, month.toDate(), Balance.Type.PROJECTED)
      }
      if (stmt.endBalance == null) {
        stmt.endBalance = nextStmt?.startBalance ?: Balance(0, month.toDate(), Balance.Type.PROJECTED)
      }
      return stmt
    }

  }
}
