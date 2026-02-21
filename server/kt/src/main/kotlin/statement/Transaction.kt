package com.cherba29.tally.statement

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.BalanceType
import com.cherba29.tally.core.Budget
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.Transfer


data class Transaction(
  val account: Account,
  val balance: Balance,
  val description: String?,
  val type: Type,
  // TODO: make immutable.
  var balanceFromStart: Double?
) {
  enum class Type {
    UNKNOWN,
    TRANSFER,
    INCOME,
    EXPENSE,
  }
}

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
}

fun getTransactionType(fromAccount: Account, toAccount: Account, amount: Double): Transaction.Type {
  return if (toAccount.hasCommonOwner(fromAccount) && !toAccount.isExternal && !fromAccount.isExternal) {
    Transaction.Type.TRANSFER
  } else {
    if (amount > 0) Transaction.Type.INCOME else Transaction.Type.EXPENSE
  }
}

fun makeTransactionStatement(
  account: Account,
  month: Month,
  transfers: Set<Transfer>?,
  startBalance: Balance?
): TransactionStatement {
  val statement = TransactionStatement(account, month, startBalance)
  val attributeTransfer: (Account, Account, Double) -> Transaction.Type = { fromAccount, toAccount, amount ->
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

fun buildTransactionStatementTable(budget: Budget, owner: String?): List<TransactionStatement> {
  val statementTable = mutableListOf<TransactionStatement>()

  val makeStatement: (Account, Month) -> TransactionStatement = { account, month ->
    makeTransactionStatement(
      account,
      month,
      budget.transfers[account.name]?.get(month.toString()),
      budget.balances[account.name]?.get(month.toString())
    )
  }

  // Working backwards.
  val months = budget.months.sortedDescending()
  if (months.isEmpty()) {
    throw IllegalArgumentException("Budget must have at least one month.")
  }

  for (account in budget.accounts.values) {
    if (owner != null && !account.owners.contains(owner)) {
      continue
    }
    val accountStatements = mutableListOf<TransactionStatement>()
    // Make statement outside range so that its attributes relating to previous can be used.
    var nextMonthStatement = makeStatement(account, months[0].next())
    for (month in months) {
      val statement = makeStatement(account, month)
      statement.endBalance = nextMonthStatement.startBalance
      statement.isCovered =
        statement.endBalance == null || statement.endBalance!!.amount >= 0 || nextMonthStatement.coversPrevious
      statement.isProjectedCovered = statement.isCovered || nextMonthStatement.coversProjectedPrevious
      nextMonthStatement = statement
      if (statement.startBalance != null) {
        var prevBalance = statement.startBalance.amount
        for (t in statement.transactions.asReversed()) {
          t.balanceFromStart = prevBalance + t.balance.amount
          prevBalance = t.balanceFromStart!!
        }
      }
      accountStatements.add(statement)
    }
    // Do not include account if for all months it was closed.
    if (accountStatements.any { !it.isClosed }) {
      statementTable.addAll(accountStatements)
    }
  }
  return statementTable
}
