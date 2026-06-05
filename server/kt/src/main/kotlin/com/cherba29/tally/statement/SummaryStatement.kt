package com.cherba29.tally.statement

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.MonthRange
import com.cherba29.tally.core.NodeId
import com.cherba29.tally.core.enlargeTo
import com.cherba29.tally.core.plus

class SummaryStatement(
  nodeId: NodeId,
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
  nodeId, monthRange, isClosed, startBalance, endBalance, inFlows, outFlows, totalTransfers, totalPayments, income
) {

  override fun toString(): String = "${super.toString()}, statements=$statements"

  companion object {
    class Builder {
      var nodeId: NodeId? = null
      var monthRange: MonthRange? = null
      private var startBalance: Balance? = null
      private var endBalance: Balance? = null
      private var inFlows: Int = 0
      private var outFlows: Int = 0
      private var totalTransfers: Int = 0
      private var totalPayments: Int = 0
      private var income: Int = 0

      private val statements: MutableList<Statement> = mutableListOf()

      fun addStatement(statement: Statement) {
        if (statement.isClosed) return  // Does not contribute to the summary.

        monthRange = monthRange.enlargeTo(statement.monthRange)

        startBalance += statement.startBalance
        endBalance += statement.endBalance
        if (statement.inFlows > 0) {
          inFlows += statement.inFlows
        } else {
          outFlows += statement.inFlows
        }
        if (statement.outFlows > 0) {
          inFlows += statement.outFlows
        } else {
          outFlows += statement.outFlows
        }
        totalTransfers += statement.totalTransfers
        totalPayments += statement.totalPayments
        income += statement.income
        statements.add(statement)
      }
      fun build(): SummaryStatement {
        require(nodeId != null)
        require(monthRange != null)
        return SummaryStatement(
          nodeId!!,
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
    }

    fun builder(block: Builder.()->Unit): SummaryStatement {
      val builder = Builder()
      block(builder)
      return builder.build()
    }
  }
}
