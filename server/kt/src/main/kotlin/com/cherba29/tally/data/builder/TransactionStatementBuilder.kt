package com.cherba29.tally.data.builder

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.Transfer
import com.cherba29.tally.core.TreeNode
import com.cherba29.tally.statement.Transaction
import com.cherba29.tally.statement.TransactionStatement
import java.lang.IllegalArgumentException

class TransactionStatementBuilder {
  var treeNode: TreeNode.Leaf? = null
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
    val leafTreeNode = treeNode ?: throw IllegalArgumentException("TreeNode must be set before adding transfer")
    hasProjectedTransfer = hasProjectedTransfer || transfer.balance.type == Balance.Type.PROJECTED

    var otherAccount: TreeNode
    var balance: Balance
    var transactionType: Transaction.Type
    if (transfer.toAccount.name == leafTreeNode.name) {
      balance = transfer.balance
      otherAccount = transfer.fromAccount
      transactionType = getTransactionType(fromAccount = otherAccount, toAccount = leafTreeNode, balance.amount)
    } else if (transfer.fromAccount.name == leafTreeNode.name) {
      balance = -transfer.balance
      otherAccount = transfer.toAccount
      transactionType = getTransactionType(fromAccount = leafTreeNode, toAccount = otherAccount, balance.amount)
    } else {
      // This should never occur since budget should have been validated by now.
      throw IllegalStateException(
        "Setting transfer from (${transfer.fromAccount} to ${transfer.toAccount}) for '${leafTreeNode.name}' account statement!"
      )
    }

    if (balance.amount > 0) {
      inFlows += balance.amount
    } else {
      outFlows += balance.amount
    }
    when (transactionType) {
      Transaction.Type.EXPENSE -> totalPayments += balance.amount
      Transaction.Type.INCOME -> income += balance.amount
      Transaction.Type.UNKNOWN -> {}
      Transaction.Type.TRANSFER -> totalTransfers += balance.amount
    }
    if (!coversPrevious
      && balance.amount > 0
      && transfer.fromAccount.top.name == leafTreeNode.top.name) {
      coversProjectedPrevious = true
      if (balance.type != Balance.Type.PROJECTED) {
        coversPrevious = true
      }
    }
    transactions.add(
      Transaction(
        otherAccount,
        balance,
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
        "$month $startBalance for account $treeNode starts after its first " +
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
      treeNode!!,
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
