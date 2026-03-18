package com.cherba29.tally.statement

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.Month
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.sign

class SummaryStatement(account: Account, month: Month, val startMonth: Month) : Statement(account, month) {
  val statements: MutableList<Statement> = mutableListOf()

  override val isClosed: Boolean
    get() = statements.any { statement -> statement.isClosed }

  override val annualizedPercentChange: Double?
    get() {
      val prctChange = percentChange ?: return null
      val numberOfMonths = month - startMonth + 1
      val annualFrequency = 12.0 / numberOfMonths
      val result = (1 + prctChange.absoluteValue / 100).pow(annualFrequency) - 1
      // Annualized percentage change is not that meaningful if large.
      return if (result < 10) 100 * prctChange.sign * result else null
    }

  fun addStatement(statement: Statement) {
    if (statement.month.compareTo(month) != 0) {
      throw IllegalArgumentException(
        "${statement.account.name} statement for month ${statement.month} is being added to summary for month $month"
      )
    }
    if (statement.isClosed) {
      // Does not contribute to the summary.
      return
    }
    if (startBalance == null) {
      startBalance = statement.startBalance
    } else if (statement.startBalance != null) {
      startBalance = Balance.add(startBalance!!, statement.startBalance!!)
    }
    if (endBalance == null) {
      endBalance = statement.endBalance
    } else if (statement.endBalance != null) {
      endBalance = Balance.add(endBalance!!, statement.endBalance!!)
    }
    addInFlow(statement.inFlows)
    addOutFlow(statement.outFlows)
    totalTransfers += statement.totalTransfers
    totalPayments += statement.totalPayments
    income += statement.income
    statements.add(statement)
  }
}
