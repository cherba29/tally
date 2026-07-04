package com.cherba29.tally.data.builder

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.MonthRange
import com.cherba29.tally.core.Transfer
import com.cherba29.tally.core.TreeNode
import com.cherba29.tally.statement.Transaction
import com.cherba29.tally.statement.TransactionStatement

class TransactionStatementBuilder {
  fun fromTransfers(
    leafTreeNode: TreeNode.Leaf,
    monthRange: MonthRange,
    isClosed: Boolean,
    transfers: List<Transfer>?,
    startBalance: Balance?
  ): TransactionStatement {
    val statement = TransactionStatement(leafTreeNode, monthRange, isClosed, startBalance)
    val attributeTransfer: (TreeNode, TreeNode, Long) -> Transaction.Type = { fromAccount, toAccount, amount ->
      if (amount > 0) {
        statement.inFlows += amount
      } else {
        statement.outFlows += amount
      }
      val transactionType = getTransactionType(fromAccount, toAccount, amount)
      when (transactionType) {
        Transaction.Type.EXPENSE -> statement.totalPayments += amount
        Transaction.Type.INCOME -> statement.income += amount
        Transaction.Type.UNKNOWN -> {}
        Transaction.Type.TRANSFER -> statement.totalTransfers += amount
      }
      transactionType
    }
    val descTransfers = transfers?.sortedDescending() ?: listOf()

    val firstTransfer: Transfer? = descTransfers.lastOrNull()
    if (firstTransfer != null && startBalance != null && firstTransfer.balance.date < startBalance.date) {
      throw IllegalStateException(
        "Balance ${monthRange.first} $startBalance for account $leafTreeNode starts after " +
            "transaction ${firstTransfer.fromAccount.name} --> " +
            "${firstTransfer.toAccount.name}/${firstTransfer.balance} desc '${firstTransfer.description}'"
      )
    }

    for (t in descTransfers) {
      statement.hasProjectedTransfer =
        statement.hasProjectedTransfer || t.balance.type == Balance.Type.PROJECTED
      var otherAccount: TreeNode
      var balance: Balance
      var transactionType: Transaction.Type
      if (t.toAccount.name == leafTreeNode.name) {
        balance = t.balance
        otherAccount = t.fromAccount
        transactionType = attributeTransfer(otherAccount, leafTreeNode, balance.amount)
      } else if (t.fromAccount.name == leafTreeNode.name) {
        balance = -t.balance
        otherAccount = t.toAccount
        transactionType = attributeTransfer(leafTreeNode, otherAccount, balance.amount)
      } else {
        // This should never occur since budget should have been validated by now.
        throw IllegalStateException(
          "Setting transfer from (${t.fromAccount} to ${t.toAccount}) for '${leafTreeNode.name}' account statement!"
        )
      }
      if (!statement.coversPrevious && balance.amount > 0 && t.fromAccount.top.name == leafTreeNode.top.name) {
        statement.coversProjectedPrevious = true
        if (balance.type != Balance.Type.PROJECTED) {
          statement.coversPrevious = true
        }
      }
      statement.transactions.add(
        Transaction(
          treeNode = otherAccount,
          description = t.description,
          balance = balance,
          type = transactionType,
          balanceFromStart = null,
        )
      )
    }
    return statement
  }

  private fun getTransactionType(fromAccount: TreeNode, toAccount: TreeNode, amount: Long): Transaction.Type {
    return if ((toAccount.path.first() == fromAccount.path.first()) && !toAccount.isExternal && !fromAccount.isExternal) {
      Transaction.Type.TRANSFER
    } else {
      if (amount > 0) Transaction.Type.INCOME else Transaction.Type.EXPENSE
    }
  }
}