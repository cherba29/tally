package com.cherba29.tally.data.builder

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.TreeNode
import com.cherba29.tally.core.MonthRange
import com.cherba29.tally.core.plus
import com.cherba29.tally.statement.Statement
import com.cherba29.tally.statement.SummaryStatement

class MonthSummaryStatementBuilder {
  var treeNode: TreeNode? = null
  private val statements: MutableList<Statement> = mutableListOf()

  fun addStatement(statement: Statement) {
    statements.add(statement)
  }

  fun build(): SummaryStatement {
    require(treeNode != null) { "summary build failed: treeNode is not set"}
    require(statements.isNotEmpty()) { "summary build failed: no statements have been added"}
    val monthRanges = statements.map { it.monthRange }.toSet()
    require(monthRanges.size == 1) { "summary build failed: statements for different months provided"}
    val monthRange: MonthRange = monthRanges.first()
    var startBalance: Balance? = null
    var endBalance: Balance? = null
    var inFlows: Long = 0
    var outFlows: Long = 0
    var totalTransfers: Long = 0
    var totalPayments: Long = 0
    var income: Long = 0

    for (statement in statements) {
      startBalance += statement.startBalance
      endBalance += statement.endBalance
      inFlows += statement.inFlows
      outFlows += statement.outFlows
      totalTransfers += statement.totalTransfers
      totalPayments += statement.totalPayments
      income += statement.income
    }

    return SummaryStatement(
      treeNode!!,
      monthRange,
      statements.all { statement -> statement.isClosed },
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
