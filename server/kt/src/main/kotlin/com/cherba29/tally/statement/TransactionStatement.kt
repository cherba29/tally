package com.cherba29.tally.statement

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.MonthRange
import com.cherba29.tally.core.NodeId
import com.cherba29.tally.core.Transfer

// Extension of Statement for transactions over an account.
class TransactionStatement(nodeId: NodeId, monthRange: MonthRange, isClosed: Boolean, startBalance: Balance?) :
  Statement(nodeId, monthRange, isClosed, startBalance) {
  // List of transaction in this statement.
  val transactions: MutableList<Transaction> = mutableListOf()

  // True if any transactions in this statement "cover" previous statement.
  var coversPrevious = false

  // True if any projected transactions in this statement "cover"
  // previous statement.
  var coversProjectedPrevious = false

  // True if any of the transactions are projects.
  var hasProjectedTransfer = false

  // True if this statement is covered by next.
  var isCovered = false

  // True if this statement is covered by any projected transactions in next statement.
  var isProjectedCovered = false

  companion object {
    fun fromTransfers(
      nodeId: NodeId,
      monthRange: MonthRange,
      isClosed: Boolean,
      transfers: List<Transfer>?,
      startBalance: Balance?
    ): TransactionStatement {
      val statement = TransactionStatement(nodeId, monthRange, isClosed, startBalance)
      val attributeTransfer: (NodeId, NodeId, Int) -> Transaction.Type = { fromAccount, toAccount, amount ->
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
          "Balance ${monthRange.first} $startBalance for account $nodeId starts after " +
              "transaction ${firstTransfer.fromAccount.nodeId.name} --> " +
              "${firstTransfer.toAccount.nodeId.name}/${firstTransfer.balance} desc '${firstTransfer.description}'"
        )
      }

      for (t in descTransfers) {
        statement.hasProjectedTransfer =
          statement.hasProjectedTransfer || t.balance.type == Balance.Type.PROJECTED
        var otherAccount: NodeId
        var balance: Balance
        var transactionType: Transaction.Type
        if (t.toAccount.nodeId.name === nodeId.name) {
          balance = t.balance
          otherAccount = t.fromAccount.nodeId
          transactionType = attributeTransfer(otherAccount, nodeId, balance.amount)
        } else if (t.fromAccount.nodeId.name === nodeId.name) {
          balance = Balance.negated(t.balance)
          otherAccount = t.toAccount.nodeId
          transactionType = attributeTransfer(nodeId, otherAccount, balance.amount)
        } else {
          // This should never occur since budget should have been validated by now.
          throw IllegalStateException(
            "Setting transfer (${t.fromAccount.nodeId.name} to ${t.toAccount.nodeId.name}) for ${nodeId.name} account statement!"
          )
        }
        if (!statement.coversPrevious && balance.amount > 0 && t.fromAccount.nodeId.hasCommonOwner(nodeId)) {
          statement.coversProjectedPrevious = true
          if (balance.type != Balance.Type.PROJECTED) {
            statement.coversPrevious = true
          }
        }
        statement.transactions.add(
          Transaction(
            nodeId = otherAccount,
            description = t.description,
            balance = balance,
            type = transactionType,
            balanceFromStart = null,
          )
        )
      }
      return statement
    }

    private fun getTransactionType(fromAccount: NodeId, toAccount: NodeId, amount: Int): Transaction.Type {
      return if (toAccount.hasCommonOwner(fromAccount) && !toAccount.isExternal && !fromAccount.isExternal) {
        Transaction.Type.TRANSFER
      } else {
        if (amount > 0) Transaction.Type.INCOME else Transaction.Type.EXPENSE
      }
    }
  }
}
