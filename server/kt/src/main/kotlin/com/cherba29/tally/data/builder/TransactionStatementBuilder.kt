package com.cherba29.tally.data.builder

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.Transaction
import com.cherba29.tally.statement.TransactionStatement

class TransactionStatementBuilder {
  var month: Month? = null
  var startBalance: Balance? = null
  var endBalance: Balance? = null
  var isCovered: Boolean = false
  var isProjectedCovered: Boolean = false
  private var inFlows = 0L
  private var outFlows = 0L
  private var totalPayments = 0L
  private var totalTransfers = 0L
  private var income = 0L
  private var hasProjectedTransfer: Boolean = false
  private var coversPrevious: Boolean = false
  private var coversProjectedPrevious: Boolean = false
  private var transactions = mutableListOf<Transaction>()

  fun addTransfer(transfer: Transaction) {
    hasProjectedTransfer = hasProjectedTransfer || transfer.balance.type == Balance.Type.PROJECTED

    val amount = transfer.balance.amount
    if (amount > 0) {
      inFlows += amount
    } else {
      outFlows += amount
    }
    when (transfer.type) {
      Transaction.Type.EXPENSE -> totalPayments += amount
      Transaction.Type.INCOME -> income += amount
      Transaction.Type.UNKNOWN -> {}
      Transaction.Type.TRANSFER -> totalTransfers += amount
    }
    if (!coversPrevious && amount > 0) {
      coversProjectedPrevious = true
      if (transfer.balance.type != Balance.Type.PROJECTED) {
        coversPrevious = true
      }
    }
    transactions.add(transfer)
  }

  fun build(): TransactionStatement {
    transactions.sort()

    val firstTransaction = transactions.firstOrNull()
    if (firstTransaction != null
      && startBalance != null
      && firstTransaction.balance.date < startBalance!!.date) {
      throw IllegalStateException(
        "$month $startBalance starts after its first " +
            "transfer to ${firstTransaction.targetTreeNode.pathString} " +
            "for amount of ${firstTransaction.balance} desc '${firstTransaction.description}'"
      )
    }

    val balanceFromStart = transactions.runningFold(startBalance?.amount) { total, element->
      total?.plus(element.balance.amount)
    }.drop(1)

    return TransactionStatement(
      month!!..month!!,
      startBalance,
      endBalance,
      inFlows,
      outFlows,
      totalTransfers,
      totalPayments,
      income,
      coversPrevious,
      coversProjectedPrevious,
      hasProjectedTransfer,
      isCovered,
      isProjectedCovered,
      // Transactions are displayed last at the top.
      transactions.asReversed(),
      balanceFromStart.asReversed()
    )
  }
}

fun transactionStatement(init: TransactionStatementBuilder.() -> Unit): TransactionStatement {
  val statementBuilder = TransactionStatementBuilder()
  statementBuilder.init()
  return statementBuilder.build()
}
