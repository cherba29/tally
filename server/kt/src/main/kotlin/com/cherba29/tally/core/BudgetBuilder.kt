package com.cherba29.tally.core

import io.github.oshai.kotlinlogging.KotlinLogging

class BudgetBuilder(
  private var minMonth: Month? = null,
  private var maxMonth: Month? = null,
  // Account name to account map.
  private val accounts: MutableMap<NodeId, Account> = mutableMapOf(),
  // Account name -> month -> balance map.
  private val balances: MutableMap<NodeId, MutableMap<Month, Balance>> = mutableMapOf(),
  private val transfers: MutableList<TransferData> = mutableListOf()
) {
  data class TransferData(
    val toAccount: String,  // not NodeId as actual account still unknown.
    val toMonth: Month,
    val fromAccount: NodeId,
    val fromMonth: Month,
    val balance: Balance,
    val description: String?,
  )

  fun setAccount(account: Account): BudgetBuilder {
    accounts[account.nodeId] = account
    minMonth = if (minMonth != null) Month.min(minMonth!!, account.openedOn) else account.openedOn
    maxMonth = if (maxMonth != null) Month.max(maxMonth!!, account.openedOn) else account.openedOn
    if (account.closedOn != null) {
      minMonth = if (minMonth != null) Month.min(minMonth!!, account.closedOn) else account.closedOn
      maxMonth = if (maxMonth != null) Month.max(maxMonth!!, account.closedOn) else account.closedOn
    }
    return this
  }

  fun setBalance(nodeId: NodeId, month: Month, balance: Balance): BudgetBuilder {
    var accountBalances = balances[nodeId]
    if (accountBalances == null) {
      accountBalances = mutableMapOf()
      balances[nodeId] = accountBalances
    }
    val existingBalance = accountBalances[month]
    if (existingBalance != null) {
      throw IllegalArgumentException("Balance for '$nodeId' '$month' is already set to $balance")
    }
    accountBalances[month] = balance

    minMonth = if (minMonth != null) Month.min(minMonth!!, month) else month
    maxMonth = if (maxMonth != null) Month.max(maxMonth!!, month) else month
    return this
  }

  fun addTransfer(fromAccount: NodeId,
                  fromMonth: Month,
                  toAccount: String,
                  toMonth: Month,
                  balance: Balance,
                  description: String?) {
    val transferData = TransferData(
      toAccount,
      toMonth,
      fromAccount,
      fromMonth,
      balance,
      description,
    )
    transfers.add(transferData)
    minMonth = Month.min(minMonth ?: toMonth, toMonth, fromMonth)
    maxMonth = Month.max(maxMonth ?: toMonth, toMonth, fromMonth)
  }

  fun build(): Budget {
    val budgetTransfers: MutableMap<NodeId, MutableMap<Month, MutableList<Transfer>>> = mutableMapOf()
    for (transferData in transfers) {
      val toAccounts = accounts.filterKeys { it.name == transferData.toAccount }
      if (toAccounts.isEmpty()) {
        throw IllegalArgumentException("Unknown account ${transferData.toAccount}")
      } else if (toAccounts.size > 1) {
        throw IllegalArgumentException(
          "Ambiguous transfer from ${transferData.fromAccount} to ${transferData.toAccount}, " +
              "found multiple candidate accounts " +
              "${toAccounts.keys.map { "name=${it.name} path=${it.path} owners=${it.owners}" }}"
        )
      }
      val toAccount = toAccounts.values.first()

      val fromAccount = accounts[transferData.fromAccount] ?: throw IllegalArgumentException(
        "Unknown account ${transferData.fromAccount}"
      )

      if (toAccount.nodeId.owners.sorted().joinToString() != fromAccount.nodeId.owners.sorted().joinToString()) {
        logger.warn {
          "WARNING: Transaction ${transferData.fromMonth} -> ${transferData.toMonth} has " +
              "to account ${toAccount.nodeId.name} from ${fromAccount.nodeId.name} with different owners " +
              "${toAccount.nodeId.owners.joinToString()} vs ${fromAccount.nodeId.owners.joinToString()}"
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
        toAccount.nodeId,
        transferData.toMonth
      )
      toMonthTransfers.add(transfer)
      val fromMonthTransfers = getMonthTransfers(
        budgetTransfers,
        fromAccount.nodeId,
        transferData.fromMonth
      )
      fromMonthTransfers.add(transfer)
    }
    val months =
      if (minMonth != null && maxMonth != null) (minMonth!!..maxMonth!!) else MonthRange.EMPTY
    return Budget(months, accounts, balances, budgetTransfers)
  }

  companion object {
    private val logger = KotlinLogging.logger {}

    private fun <T> getMonthTransfers(
      transfers: MutableMap<NodeId, MutableMap<Month, MutableList<T>>>,
      nodeId: NodeId,
      month: Month
    ): MutableList<T> {
      var accountTransfers = transfers[nodeId]
      if (accountTransfers == null) {
        accountTransfers = mutableMapOf()
        transfers[nodeId] = accountTransfers
      }
      var monthTransfers = accountTransfers[month]
      if (monthTransfers == null) {
        monthTransfers = mutableListOf()
        accountTransfers[month] = monthTransfers
      }
      return monthTransfers
    }

  }
}

fun budget(block: BudgetBuilder.()->Unit): Budget {
  val builder = BudgetBuilder()
  block(builder)
  return builder.build()
}
