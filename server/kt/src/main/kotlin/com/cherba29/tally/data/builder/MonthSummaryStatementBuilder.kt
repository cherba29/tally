package com.cherba29.tally.data.builder

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.TreeNode
import com.cherba29.tally.core.MonthRange
import com.cherba29.tally.core.enlargeTo
import com.cherba29.tally.core.plus
import com.cherba29.tally.statement.Statement
import com.cherba29.tally.statement.SummaryStatement

class MonthSummaryStatementBuilder {
  var treeNode: TreeNode? = null
  var monthRange: MonthRange? = null
  private var startBalance: Balance? = null
  private var endBalance: Balance? = null
  private var inFlows: Long = 0
  private var outFlows: Long = 0
  private var totalTransfers: Long = 0
  private var totalPayments: Long = 0
  private var income: Long = 0

  private val statements: MutableList<Statement> = mutableListOf()

  fun addStatement(statement: Statement) {
    if (statement.isClosed) return  // Does not contribute to the summary.

    monthRange = monthRange.enlargeTo(statement.monthRange)

    startBalance += statement.startBalance
    endBalance += statement.endBalance
    inFlows += statement.inFlows
    outFlows += statement.outFlows
    totalTransfers += statement.totalTransfers
    totalPayments += statement.totalPayments
    income += statement.income
    statements.add(statement)
  }

  fun build(): SummaryStatement {
    require(treeNode != null) { "summary build failed: treeNode is not set"}
    require(monthRange != null) { "summary build failed: month range is not set"}
    return SummaryStatement(
      treeNode!!,
      monthRange!!,
      statements.any { statement -> statement.isClosed },
      startBalance,
      endBalance,
      inFlows,
      outFlows,
      totalTransfers,
      totalPayments,
      income,
      statements
    )
  }


  companion object {
    fun builder(block: MonthSummaryStatementBuilder.() -> Unit): SummaryStatement {
      val builder = MonthSummaryStatementBuilder()
      block(builder)
      return builder.build()
    }
  }
}
