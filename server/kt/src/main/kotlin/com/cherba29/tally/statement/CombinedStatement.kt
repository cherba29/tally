package com.cherba29.tally.statement

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.BalanceType
import com.cherba29.tally.core.Month
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign

class CombinedStatement(account: Account, month: Month, val startMonth: Month) : Statement(account, month) {
  override val annualizedPercentChange: Double?
    get() {
      val prctChange = percentChange ?: return null
      val numberOfMonths = month - startMonth + 1
      val annualFrequency = 12.0 / numberOfMonths
      val result = (1 + abs(prctChange) / 100).pow(annualFrequency) - 1
      // Annualized percentage change is not that meaningful if large.
      return if (result < 10) 100 * prctChange.sign * result else null
    }

  override val isClosed: Boolean get() = false

  companion object {
    fun fromStatements(
      account: Account,
      startMonth: Month,
      endMonth: Month,
      statements: Map<Month, Statement>
    ): CombinedStatement {
      val combined = CombinedStatement(account, endMonth, startMonth)
      for (currentMonth in startMonth..endMonth) {
        val stmt: Statement = makeProxyStatement(
          account,
          currentMonth,
          statements[currentMonth],
          statements[currentMonth.previous()],
          statements[currentMonth.next()]
        )
        if (
          combined.startBalance == null ||
          (stmt.startBalance != null && combined.startBalance!!.date > stmt.startBalance!!.date)
        ) {
          combined.startBalance = stmt.startBalance
        }
        if (
          combined.endBalance == null ||
          (stmt.endBalance != null && combined.endBalance!!.date < stmt.endBalance!!.date)
        ) {
          combined.endBalance = stmt.endBalance
        }
        combined.addInFlow(stmt.inFlows)
        combined.addOutFlow(stmt.outFlows)
        combined.totalTransfers += stmt.totalTransfers
        combined.totalPayments += stmt.totalPayments
        combined.income += stmt.income
      }
      return combined
    }

    private fun makeProxyStatement(
      account: Account,
      month: Month,
      currStmt: Statement?,
      prevStmt: Statement?,
      nextStmt: Statement?
    ): Statement {
      val stmt = currStmt ?: EmptyStatement(account, month)
      if (stmt.startBalance == null) {
        stmt.startBalance = prevStmt?.endBalance ?: Balance(0, month.toDate(), BalanceType.PROJECTED)
      }
      if (stmt.endBalance == null) {
        stmt.endBalance = nextStmt?.startBalance ?: Balance(0, month.toDate(), BalanceType.PROJECTED)
      }
      return stmt
    }

  }
}
