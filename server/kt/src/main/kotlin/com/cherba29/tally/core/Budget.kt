package com.cherba29.tally.core

import io.github.oshai.kotlinlogging.KotlinLogging

data class Budget(
  // Period over which the budget is defined.
  val months: List<Month>,
  // Account name to account map.
  val accounts: Map<String, Account>,
  // Account name -> month -> balance map.
  val balances: Map<String, Map<String, Balance>>,
  // Account name -> month -> transfers map.
  val transfers: Map<String, Map<String, List<Transfer>>>
) {
  fun findActiveAccounts(): List<Account> {
    val activeAccounts = mutableListOf<Account>()
    for (account in accounts.values) {
      if (months.find { m -> !account.isClosed(m) } != null) {
        activeAccounts.add(account)
      }
    }
    return activeAccounts
  }
}

data class TransferData(
  val toAccount: String,
  val toMonth: Month,
  val fromAccount: String,
  val fromMonth: Month,
  val balance: Balance,
  val description: String?,
)

private fun <T> getMonthTransfers(
  transfers: MutableMap<String, MutableMap<String, MutableList<T>>>,
  accountName: String,
  month: String
): MutableList<T> {
  var accountTransfers = transfers[accountName]
  if (accountTransfers == null) {
    accountTransfers = mutableMapOf()
    transfers[accountName] = accountTransfers
  }
  var monthTransfers = accountTransfers[month]
  if (monthTransfers == null) {
    monthTransfers = mutableListOf()
    accountTransfers[month] = monthTransfers
  }
  return monthTransfers
}

class BudgetBuilder(
  private var minMonth: Month? = null,
  private var maxMonth: Month? = null,
  // Account name to account map.
  val accounts: MutableMap<String, Account> = mutableMapOf(),
  // Account name -> month -> balance map.
  val balances: MutableMap<String, MutableMap<String, Balance>> = mutableMapOf(),
  val transfers: MutableList<TransferData> = mutableListOf()
) {
  fun setAccount(account: Account) {
    accounts[account.name] = account
    if (account.openedOn != null) {
      minMonth = if (minMonth != null) Month.min(minMonth!!, account.openedOn) else account.openedOn
      maxMonth = if (maxMonth != null) Month.max(maxMonth!!, account.openedOn) else account.openedOn
    }
    if (account.closedOn != null) {
      minMonth = if (minMonth != null) Month.min(minMonth!!, account.closedOn) else account.closedOn
      maxMonth = if (maxMonth != null) Month.max(maxMonth!!, account.closedOn) else account.closedOn
    }
  }

  fun setBalance(accountName: String, month: String, balance: Balance) {
    var accountBalances = balances[accountName]
    if (accountBalances == null) {
      accountBalances = mutableMapOf()
      balances[accountName] = accountBalances
    }
    val existingBalance = accountBalances[month]
    if (existingBalance != null) {
      throw IllegalArgumentException("Balance for '$accountName' '$month' is already set to $balance")
    }
    accountBalances[month] = balance
    val parsedMonth = Month.fromString(month)
    minMonth = if (minMonth != null) Month.min(minMonth!!, parsedMonth) else parsedMonth
    maxMonth = if (maxMonth != null) Month.max(maxMonth!!, parsedMonth) else parsedMonth
  }

  fun addTransfer(transferData: TransferData) {
    transfers.add(transferData)
    minMonth = Month.min(
      minMonth ?: transferData.toMonth,
      transferData.toMonth,
      transferData.fromMonth
    )
    this.maxMonth = Month.max(
      maxMonth ?: transferData.toMonth,
      transferData.toMonth,
      transferData.fromMonth
    )
  }

  fun build(): Budget {
    val budgetTransfers: MutableMap<String, MutableMap<String, MutableList<Transfer>>> = mutableMapOf()
    for (transferData in transfers) {
      val toAccount = accounts[transferData.toAccount] ?: throw IllegalArgumentException(
        "Unknown account ${transferData.toAccount}"
      )

      val fromAccount = accounts[transferData.fromAccount] ?: throw IllegalArgumentException(
        "Unknown account ${transferData.fromAccount}"
      )

      if (toAccount.owners.sorted().joinToString() != fromAccount.owners.sorted().joinToString()) {
        logger.warn {
          "WARNING: Transaction ${transferData.fromMonth} -> ${transferData.toMonth} has " +
              "to account ${toAccount.name} from ${fromAccount.name} with different owners " +
              "${toAccount.owners.joinToString()} vs ${fromAccount.owners.joinToString()}"
        }
      }
      val transfer = Transfer(
        fromAccount,
        toAccount,
        transferData.fromMonth,
        transferData.toMonth,
        transferData.description,
        transferData.balance
      )
      val toMonthTransfers = getMonthTransfers(
        budgetTransfers,
        toAccount.name,
        transferData.toMonth.toString()
      )
      toMonthTransfers.add(transfer)
      val fromMonthTransfers = getMonthTransfers(
        budgetTransfers,
        fromAccount.name,
        transferData.fromMonth.toString()
      )
      fromMonthTransfers.add(transfer)
    }
    val months =
      if (minMonth != null && maxMonth != null) (minMonth!!..maxMonth!!).toList() else listOf()
    return Budget(months, this.accounts, this.balances, budgetTransfers)
  }

  companion object {
    private val logger = KotlinLogging.logger {}
  }
}
