package com.cherba29.tally.core

import io.github.oshai.kotlinlogging.KotlinLogging

data class Budget(
  // Period over which the budget is defined.
  val months: List<Month>,
  // Account name to account map.
  val accounts: Map<String, Account>,
  // Account name -> month -> balance map.
  val balances: Map<String, Map<Month, Balance>>,
  // Account name -> month -> transfers map.
  val transfers: Map<String, Map<Month, List<Transfer>>>
) {
  // TODO: is this needed, since this will always return all accounts.
  fun findActiveAccounts(): List<Account> = accounts.values.filter {
    account -> months.any { !account.isClosed(it) }
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
  transfers: MutableMap<String, MutableMap<Month, MutableList<T>>>,
  accountName: String,
  month: Month
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

// TODO: define DSL for building the budget.
class BudgetBuilder(
  private var minMonth: Month? = null,
  private var maxMonth: Month? = null,
  // Account name to account map.
  val accounts: MutableMap<String, Account> = mutableMapOf(),
  // Account name -> month -> balance map.
  val balances: MutableMap<String, MutableMap<Month, Balance>> = mutableMapOf(),
  val transfers: MutableList<TransferData> = mutableListOf()
) {
  fun setAccount(account: Account): BudgetBuilder {
    accounts[account.name] = account
    if (account.openedOn != null) {
      minMonth = if (minMonth != null) Month.min(minMonth!!, account.openedOn) else account.openedOn
      maxMonth = if (maxMonth != null) Month.max(maxMonth!!, account.openedOn) else account.openedOn
    }
    if (account.closedOn != null) {
      minMonth = if (minMonth != null) Month.min(minMonth!!, account.closedOn) else account.closedOn
      maxMonth = if (maxMonth != null) Month.max(maxMonth!!, account.closedOn) else account.closedOn
    }
    return this
  }

  fun setBalance(accountName: String, month: Month, balance: Balance): BudgetBuilder {
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

    minMonth = if (minMonth != null) Month.min(minMonth!!, month) else month
    maxMonth = if (maxMonth != null) Month.max(maxMonth!!, month) else month
    return this
  }

  fun addTransfer(transferData: TransferData) {
    transfers.add(transferData)
    minMonth = Month.min(
      minMonth ?: transferData.toMonth,
      transferData.toMonth,
      transferData.fromMonth
    )
    maxMonth = Month.max(
      maxMonth ?: transferData.toMonth,
      transferData.toMonth,
      transferData.fromMonth
    )
  }

  fun build(): Budget {
    val budgetTransfers: MutableMap<String, MutableMap<Month, MutableList<Transfer>>> = mutableMapOf()
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
        transferData.toMonth
      )
      toMonthTransfers.add(transfer)
      val fromMonthTransfers = getMonthTransfers(
        budgetTransfers,
        fromAccount.name,
        transferData.fromMonth
      )
      fromMonthTransfers.add(transfer)
    }
    val months =
      if (minMonth != null && maxMonth != null) (minMonth!!..maxMonth!!).toList() else listOf()
    return Budget(months, accounts, balances, budgetTransfers)
  }

  companion object {
    private val logger = KotlinLogging.logger {}
  }
}

fun budget(block: BudgetBuilder.()->Unit): Budget {
  val builder = BudgetBuilder()
  block(builder)
  return builder.build()
}