package com.cherba29.tally.statement

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.BalanceType
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.Transfer

// Extension of Statement for transactions over an account.
class TransactionStatement(account: Account, month: Month, startBalance: Balance?) :
  Statement(account, month, startBalance) {
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

  override val isClosed: Boolean = account.isClosed(this.month)

  override fun toSnapshot(): String {
    return """TransactionStatement {
          ${super.toSnapshot()}
          coversPrevious = $coversPrevious
          coversProjectedPrevious = $coversProjectedPrevious
          hasProjectedTransfer = $hasProjectedTransfer
          isCovered = $isCovered
          isProjectedCovered = $isProjectedCovered
          isClosed = $isClosed
          transactions {
            ${transactions.joinToString("\n") { it.toSnapshot() }}
          }
        }""".trimIndent()
  }
  companion object {
    fun fromTransfers(
      account: Account,
      month: Month,
      transfers: List<Transfer>?,
      startBalance: Balance?
    ): TransactionStatement {
      val statement = TransactionStatement(account, month, startBalance)
      val attributeTransfer: (Account, Account, Int) -> Transaction.Type = { fromAccount, toAccount, amount ->
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
      val descTransfers = transfers?.sortedWith { b, a ->
        var eq: Int = a.balance.compareTo(b.balance)
        if (eq != 0) eq
        else {
          eq = a.fromMonth.compareTo(b.fromMonth)
          if (eq != 0) eq
          else {
            eq = a.toMonth.compareTo(b.toMonth)
            if (eq != 0) eq
            else if (a.fromAccount.name != b.fromAccount.name) {
              if (a.fromAccount.name < b.fromAccount.name) -1 else 1
            } else if (a.toAccount.name != b.toAccount.name) {
              if (a.toAccount.name < b.toAccount.name) -1 else 1
            } else if (a.description != b.description) {
              if (a.description.orEmpty() < b.description.orEmpty()) -1 else 1
            } else {
              0
            }
          }
        }
      } ?: listOf()

      val firstTransfer: Transfer? = descTransfers.lastOrNull()
      if (firstTransfer != null && startBalance != null && firstTransfer.balance.date < startBalance.date) {
        throw IllegalStateException(
          "Balance $month $startBalance for account ${account.name} starts after " +
              "transaction ${firstTransfer.fromAccount.name} --> " +
              "${firstTransfer.toAccount.name}/${firstTransfer.balance} desc '${firstTransfer.description}'"
        )
      }

      for (t in descTransfers) {
        statement.hasProjectedTransfer =
          statement.hasProjectedTransfer || t.balance.type == BalanceType.PROJECTED
        var otherAccount: Account
        var balance: Balance
        var transactionType: Transaction.Type
        if (t.toAccount.name === account.name) {
          balance = t.balance
          otherAccount = t.fromAccount
          transactionType = attributeTransfer(otherAccount, account, balance.amount)
        } else if (t.fromAccount.name === account.name) {
          balance = Balance.negated(t.balance)
          otherAccount = t.toAccount
          transactionType = attributeTransfer(account, otherAccount, balance.amount)
        } else {
          // This should never occur since budget should have been validated by now.
          throw IllegalStateException(
            "Setting transfer (${t.fromAccount.name} to ${t.toAccount.name}) for ${account.name} account statement!"
          )
        }
        if (!statement.coversPrevious && balance.amount > 0 && t.fromAccount.hasCommonOwner(account)) {
          statement.coversProjectedPrevious = true
          if (balance.type != BalanceType.PROJECTED) {
            statement.coversPrevious = true
          }
        }
        statement.transactions.add(
          Transaction(
            account = otherAccount,
            description = t.description,
            balance = balance,
            type = transactionType,
            balanceFromStart = null,
          )
        )
      }
      return statement
    }

    private fun getTransactionType(fromAccount: Account, toAccount: Account, amount: Int): Transaction.Type {
      return if (toAccount.hasCommonOwner(fromAccount) && !toAccount.isExternal && !fromAccount.isExternal) {
        Transaction.Type.TRANSFER
      } else {
        if (amount > 0) Transaction.Type.INCOME else Transaction.Type.EXPENSE
      }
    }
  }
}
