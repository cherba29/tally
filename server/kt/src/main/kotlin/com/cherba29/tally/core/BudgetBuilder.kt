package com.cherba29.tally.core

import io.github.oshai.kotlinlogging.KotlinLogging

class BudgetBuilder(
  private var minMonth: Month? = null,
  private var maxMonth: Month? = null,
  // Account name to account map.
  private val accounts: MutableMap<NodeId, Account> = mutableMapOf(),
  // Account name -> month -> balance map.
  private val balances: MutableMap<List<String>, MutableMap<Month, Balance>> = mutableMapOf(),
  private val transfers: MutableList<TransferData> = mutableListOf()
) {
  data class TransferData(
    val toAccountName: String,  // Full path is unknown at time of record.
    val toMonth: Month,
    val fromAccountPath: List<String>,
    val fromMonth: Month,
    val balance: Balance,
    val description: String?,
  )

  private val groupTreeBuilder = Group.Companion.Builder()
  private val pathToAccount = mutableMapOf<List<String>, Account>()

  fun setAccount(fullPath: List<String>, account: Account): BudgetBuilder {
    groupTreeBuilder.addPath(fullPath)
    pathToAccount[fullPath] = account
    accounts[account.nodeId] = account
    minMonth = if (minMonth != null) Month.min(minMonth!!, account.openedOn) else account.openedOn
    maxMonth = if (maxMonth != null) Month.max(maxMonth!!, account.openedOn) else account.openedOn
    if (account.closedOn != null) {
      minMonth = if (minMonth != null) Month.min(minMonth!!, account.closedOn) else account.closedOn
      maxMonth = if (maxMonth != null) Month.max(maxMonth!!, account.closedOn) else account.closedOn
    }
    return this
  }

  fun setBalance(accountPath: List<String>, month: Month, balance: Balance): BudgetBuilder {
    val accountBalances = balances.getOrPut(accountPath) { mutableMapOf() }
    if (accountBalances.put(month, balance) != null) {
      throw IllegalArgumentException(
        "Balance for '${accountPath.joinToString("/")}' '$month' is already set to $balance")
    }

    minMonth = if (minMonth != null) Month.min(minMonth!!, month) else month
    maxMonth = if (maxMonth != null) Month.max(maxMonth!!, month) else month
    return this
  }

  /**
   * Add a record of transfer from given account to potentially yet unknown account name.
   */
  fun addTransfer(fromAccountPath: List<String>,
                  fromMonth: Month,
                  toAccountName: String,
                  toMonth: Month,
                  balance: Balance,
                  description: String?) {
    val transferData = TransferData(
      toAccountName,
      toMonth,
      fromAccountPath,
      fromMonth,
      balance,
      description,
    )
    transfers.add(transferData)
    minMonth = Month.min(minMonth ?: toMonth, toMonth, fromMonth)
    maxMonth = Month.max(maxMonth ?: toMonth, toMonth, fromMonth)
  }

  private fun buildTransfers(tree: Group): MutableMap<Group.Leaf, MutableMap<Month, MutableList<Transfer>>> {
    val budgetTransfers: MutableMap<Group.Leaf, MutableMap<Month, MutableList<Transfer>>> = mutableMapOf()
    for (transferData in transfers) {

      val toAccounts = pathToAccount.keys.filter { it.last() == transferData.toAccountName }
      if (toAccounts.isEmpty()) {
        throw IllegalArgumentException("Unknown account ${transferData.toAccountName}, " +
            "known accounts [${pathToAccount.keys.joinToString { it.joinToString("/") }}]")
      } else if (toAccounts.size > 1) {
        throw IllegalArgumentException(
          "Ambiguous transfer from ${transferData.fromAccountPath.joinToString("/")} to ${transferData.toAccountName}, " +
              "found multiple candidate accounts " + toAccounts.joinToString { it.joinToString("/") })
      }

      val toAccount = tree[toAccounts.first()] as? Group.Leaf
        ?: throw IllegalStateException("Unknown account path ${toAccounts.first().joinToString("/")}")

      val fromAccount = tree[transferData.fromAccountPath] as? Group.Leaf ?: throw IllegalArgumentException(
        "Unknown account ${transferData.fromAccountPath.joinToString("/")}"
      )

      val fromOwner = fromAccount.top.name
      val toOwner = toAccount.top.name
      if (fromOwner != toOwner) {
        logger.warn {
          "WARNING: Transaction ${transferData.fromMonth} -> ${transferData.toMonth} has " +
              "to account ${toAccount.name} from ${fromAccount.name} with different owners " +
              "$fromOwner vs $toOwner"
        }
      }

      val transfer = Transfer(
        pathToAccount[fromAccount.path]
          ?: throw IllegalStateException("Could not find path ${fromAccount.path}"),
        pathToAccount[toAccount.path]
          ?: throw IllegalStateException("Could not find path ${fromAccount.path}"),
        transferData.fromMonth,
        transferData.toMonth,
        transferData.description,
        transferData.balance
      )
      val toMonthTransfers = getMonthTransfers(
        budgetTransfers,
        toAccount,
        transferData.toMonth
      )
      toMonthTransfers.add(transfer)
      val fromMonthTransfers = getMonthTransfers(
        budgetTransfers,
        fromAccount,
        transferData.fromMonth
      )
      fromMonthTransfers.add(transfer)
    }
    return budgetTransfers
  }

  fun build(): Budget {
    val months = if (minMonth != null && maxMonth != null) (minMonth!!..maxMonth!!) else MonthRange.EMPTY
    val tree = groupTreeBuilder.build()
    val leafToAccount = pathToAccount.mapKeys {
      tree[it.key] as? Group.Leaf ?: throw IllegalStateException("Could not find path ${it.key}")
    }
    val leafToBalances = balances.mapKeys {
      tree[it.key] as? Group.Leaf ?: throw IllegalStateException("Could not find path ${it.key}")
    }
    val transfers = buildTransfers(tree)
    return Budget(
      months,
      tree,
      leafToAccount,
      accounts,
      balances.mapKeys {
        pathToAccount[it.key]?.nodeId ?: throw java.lang.IllegalStateException("Could not find path ${it.key}")
      },
      transfers.mapKeys {
        pathToAccount[it.key.path]?.nodeId
          ?: throw java.lang.IllegalStateException("Could not find path ${it.key.path}")
      }
    )
  }

  companion object {
    private val logger = KotlinLogging.logger {}

    private fun <T> getMonthTransfers(
      transfers: MutableMap<Group.Leaf, MutableMap<Month, MutableList<T>>>,
      nodeId: Group.Leaf,
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
