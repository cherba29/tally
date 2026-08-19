package com.cherba29.tally.data.builder

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.Transfer
import com.cherba29.tally.core.TreeNode
import com.cherba29.tally.statement.Transaction
import com.cherba29.tally.statement.TransactionStatement

class TransactionStatementBuilder {
  var month: Month? = null
  var isClosed: Boolean = false
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

  fun addTransfer(transfer: Transfer) {
    hasProjectedTransfer = hasProjectedTransfer || transfer.balance.type == Balance.Type.PROJECTED

    val amount = -transfer.balance.amount
    val transactionType = getTransactionType(transfer.fromAccount, transfer.toAccount, amount)
    if (amount > 0) {
      inFlows += amount
    } else {
      outFlows += amount
    }
    when (transactionType) {
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
    transactions.add(
      Transaction(
        transfer.toAccount,
        -transfer.balance,
        transfer.description,
        transactionType,
        balanceFromStart = null
      )
    )
  }

  fun build(): TransactionStatement {
    transactions.sort()

    val firstTransaction = transactions.firstOrNull()
    if (firstTransaction != null
      && startBalance != null
      && firstTransaction.balance.date < startBalance!!.date) {
      throw IllegalStateException(
        "$month $startBalance starts after its first " +
            "transfer to ${firstTransaction.targetTreeNode.path.joinToString("/")} " +
            "for amount of ${firstTransaction.balance} desc '${firstTransaction.description}'"
      )
    }

    val updatedTransactions = mutableListOf<Transaction>()
    var prevBalance = startBalance?.amount
    for (t in transactions) {
      prevBalance = prevBalance?.plus(t.balance.amount)
      updatedTransactions.add(t.copy(balanceFromStart = prevBalance))
    }
    // Transactions are displayed last at the top.
    updatedTransactions.reverse()

    return TransactionStatement(
      month!!..month!!,
      isClosed,
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
      transactions = updatedTransactions
    )
  }

  private fun getTransactionType(fromAccount: TreeNode, toAccount: TreeNode, amount: Long): Transaction.Type {
    return if ((toAccount.path.first() == fromAccount.path.first()) && !toAccount.isExternal && !fromAccount.isExternal) {
      Transaction.Type.TRANSFER
    } else {
      if (amount > 0) Transaction.Type.INCOME else Transaction.Type.EXPENSE
    }
  }
}

fun transactionStatement(init: TransactionStatementBuilder.() -> Unit): TransactionStatement {
  val statementBuilder = TransactionStatementBuilder()
  statementBuilder.init()
  return statementBuilder.build()
}
