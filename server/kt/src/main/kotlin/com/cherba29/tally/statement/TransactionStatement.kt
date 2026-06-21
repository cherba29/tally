package com.cherba29.tally.statement

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.Group
import com.cherba29.tally.core.MonthRange
import com.cherba29.tally.core.Transfer

// Extension of Statement for transactions over an account.
class TransactionStatement(nodeId: Group, monthRange: MonthRange, isClosed: Boolean, startBalance: Balance?) :
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

  override fun toString(): String {
    return super.toString() + " coversPrevious=$coversPrevious coversProjectPrevious=$coversProjectedPrevious" +
        " hasProjectedTransfer=$hasProjectedTransfer isCovered=$isCovered isProjectedCovered=$isProjectedCovered" +
        " transactions=$transactions"
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!super.equals(other)) return false

    other as TransactionStatement

    if (coversPrevious != other.coversPrevious) return false
    if (coversProjectedPrevious != other.coversProjectedPrevious) return false
    if (hasProjectedTransfer != other.hasProjectedTransfer) return false
    if (isCovered != other.isCovered) return false
    if (isProjectedCovered != other.isProjectedCovered) return false
    if (transactions != other.transactions) return false

    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + coversPrevious.hashCode()
    result = 31 * result + coversProjectedPrevious.hashCode()
    result = 31 * result + hasProjectedTransfer.hashCode()
    result = 31 * result + isCovered.hashCode()
    result = 31 * result + isProjectedCovered.hashCode()
    result = 31 * result + transactions.hashCode()
    return result
  }

  companion object {
    fun fromTransfers(
      tree: Group,
      nodeId: Group.Leaf,
      monthRange: MonthRange,
      isClosed: Boolean,
      transfers: List<Transfer>?,
      startBalance: Balance?
    ): TransactionStatement {
      val statement = TransactionStatement(nodeId, monthRange, isClosed, startBalance)
      val attributeTransfer: (Group, Group, Int) -> Transaction.Type = { fromAccount, toAccount, amount ->
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
              "transaction ${firstTransfer.fromAccount.last()} --> " +
              "${firstTransfer.toAccount.last()}/${firstTransfer.balance} desc '${firstTransfer.description}'"
        )
      }

      for (t in descTransfers) {
        statement.hasProjectedTransfer =
          statement.hasProjectedTransfer || t.balance.type == Balance.Type.PROJECTED
        var otherAccount: Group
        var balance: Balance
        var transactionType: Transaction.Type
        if (t.toAccount.last() === nodeId.name) {
          balance = t.balance
          otherAccount = tree[t.fromAccount] ?: throw IllegalStateException("${t.fromAccount} is not in $tree")
          transactionType = attributeTransfer(otherAccount, nodeId, balance.amount)
        } else if (t.fromAccount.last() === nodeId.name) {
          balance = -t.balance
          otherAccount = tree[t.toAccount] ?: throw IllegalStateException("${t.toAccount} is not in $tree")
          transactionType = attributeTransfer(nodeId, otherAccount, balance.amount)
        } else {
          // This should never occur since budget should have been validated by now.
          throw IllegalStateException(
            "Setting transfer from (${t.fromAccount} to ${t.toAccount}) for '${nodeId.name}' account statement!"
          )
        }
        if (!statement.coversPrevious && balance.amount > 0 && tree[t.fromAccount]?.path?.first() == nodeId.path.first()) {
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

    private fun getTransactionType(fromAccount: Group, toAccount: Group, amount: Int): Transaction.Type {
      return if ((toAccount.path.first() == fromAccount.path.first()) && !toAccount.isExternal && !fromAccount.isExternal) {
        Transaction.Type.TRANSFER
      } else {
        if (amount > 0) Transaction.Type.INCOME else Transaction.Type.EXPENSE
      }
    }
  }
}
