package com.cherba29.tally.data.builder

import com.cherba29.tally.core.*
import com.cherba29.tally.data.Budget
import com.cherba29.tally.statement.Statement
import com.cherba29.tally.statement.TransactionStatement
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.time.TimeSource
import kotlin.time.measureTimedValue

class BudgetBuilder {
  // Account name -> month -> balance map.
  private val balances: MutableMap<List<String>, MutableMap<Month, Balance>> = mutableMapOf()
  private val transferRecordList: MutableList<TransferRecord> = mutableListOf()
  private val timeSource: TimeSource = TimeSource.Monotonic

  private var monthRange: MonthRange? = null

  data class TransferRecord(
    val toAccountName: String,  // Full path is unknown at time of record.
    val fromAccountPath: List<String>,
    val month: Month,
    val balance: Balance,
    val description: String?,
    val tags: List<String>,
  )

  private val treeNodeBuilder = TreeNode.Builder()
  private val pathToAccount = mutableMapOf<List<String>, Account>()

  fun addAccount(account: Account, accountBalances: Map<Month, Balance>) {
    for (owner in account.owners) {
      val fullPath = listOf(owner) + account.path + listOf(account.name)
      setAccount(fullPath, account)
      for ((month, balance) in accountBalances) {
        setBalance(fullPath, month, balance)
      }
    }
    if (account.isExternal && account.closedOn != null) {
      val inactiveAccount = account.cloneInactive()
      val inactiveAccountBalances = mutableMapOf<Month, Balance>()

      val closingBalance = accountBalances[account.closedOn]
      if (closingBalance != null) {
        inactiveAccountBalances[inactiveAccount.openedOn] = Balance(
          0, inactiveAccount.openedOn.toDate(), Balance.Type.CONFIRMED)
        inactiveAccountBalances[inactiveAccount.openedOn.next()] = closingBalance
      }
      for (owner in inactiveAccount.owners) {
        val fullPath = listOf(owner) + inactiveAccount.path + listOf(inactiveAccount.name)
        setAccount(fullPath, inactiveAccount)
        for ((month, balance) in inactiveAccountBalances) {
          setBalance(fullPath, month, balance)
        }
      }
      // Zero out closed external account as its balance is transferred to virtual inactive.
      val nextMonth = account.closedOn.next()
      val zeroBalance = Balance(0, nextMonth.toDate(), Balance.Type.CONFIRMED)
      for (owner in account.owners) {
        val fullPath = listOf(owner) + account.path + listOf(account.name)
        setBalance(fullPath, nextMonth, zeroBalance)
      }

      if (closingBalance != null) {
        addAccountTransfer(
          account,
          inactiveAccount.name,
          account.closedOn,
          closingBalance.copy(date = inactiveAccount.openedOn.toDate()),
          "Moving ending balance to corresponding inactive account"
          )
      }
    }
  }

  fun setAccount(fullPath: List<String>, account: Account): BudgetBuilder {
    treeNodeBuilder.addPath(fullPath, account.rank)
    pathToAccount[fullPath] = account
    monthRange += account.openedOn
    monthRange += account.closedOn
    return this
  }

  fun setBalance(accountPath: List<String>, month: Month, balance: Balance): BudgetBuilder {
    val accountBalances = balances.getOrPut(accountPath) { mutableMapOf() }
    if (accountBalances.put(month, balance) != null) {
      throw IllegalArgumentException(
        "Balance for '${accountPath.joinToString("/")}' '$month' is already set to $balance"
      )
    }
    monthRange += month
    return this
  }

  fun addAccountTransfer(
    fromAccount: Account,
    toAccountName: String,
    month: Month,
    balance: Balance,
    description: String? = null,
    tags: List<String> = listOf()
  ) {
    for (owner in fromAccount.owners) {
      val fromAccountPath = listOf(owner) + fromAccount.path + listOf(fromAccount.name)
      addTransfer(TransferRecord(toAccountName, fromAccountPath, month, balance, description, tags))
    }
  }

  /**
   * Add a record of transfer from given account to potentially yet unknown account name.
   */
  fun addTransfer(record: TransferRecord) {
    transferRecordList.add(record)
    monthRange += record.month
  }

  private fun buildTransactionStatements(
    treeRoot: TreeNode,
    leafToAccount: Map<TreeNode.Leaf, Account>
  ): Map<TreeNode.Leaf, Map<Month, TransactionStatement>> {
    // Backfill external inactive account balances.
    for ((accountNode, account) in leafToAccount) {
      if (account.isInactive) {
        var lastBalance: Balance? = null
        for (month in monthRange!!) {
          val accountBalances = balances.getOrPut(accountNode.path) { mutableMapOf() }
          val currentBalance = accountBalances[month]
          if (currentBalance == null) {
            if (lastBalance != null) {
              accountBalances[month] = lastBalance.copy(date = month.toDate())
            }
          } else {
            lastBalance = currentBalance
          }
        }
      }
    }
    val leafToBalances = balances.mapKeys {
      treeRoot[it.key] as? TreeNode.Leaf ?: throw IllegalStateException("Could not find path ${it.key}")
    }

    val statementBuilders = mutableMapOf<TreeNode.Leaf, MonthTransactionStatementBuilder>()

    statementBuilders.putAll(
      leafToAccount.keys.associateWith {
        val builder = MonthTransactionStatementBuilder()
        builder.months = monthRange!!
        builder.monthlyBalances = leafToBalances[it] ?: mapOf()
        builder
      })

    val cachedNameToTreenodeMap = mutableMapOf<String, TreeNode.Leaf>()

    for (transferRecord in transferRecordList) {
      val toAccountNode = cachedNameToTreenodeMap.getOrPut(transferRecord.toAccountName) {
        val toAccounts = pathToAccount.keys.filter { it.last() == transferRecord.toAccountName }
        if (toAccounts.isEmpty()) {
          throw IllegalArgumentException(
            "Unknown to account ${transferRecord.toAccountName} in " +
                "${transferRecord.fromAccountPath.joinToString("/")}, " +
                "known accounts\n${treeRoot.toPrettyString()}"
          )
        } else if (toAccounts.size > 1) {
          throw IllegalArgumentException(
            "Ambiguous transfer from ${transferRecord.fromAccountPath.joinToString("/")} to " +
                "${transferRecord.toAccountName}, found multiple candidate accounts " +
                toAccounts.joinToString { it.joinToString("/") })
        }

        treeRoot[toAccounts.first()] as? TreeNode.Leaf
          ?: throw IllegalStateException("Unknown account path ${toAccounts.first().joinToString("/")}")
      }
      val fromAccountNode = treeRoot[transferRecord.fromAccountPath] as? TreeNode.Leaf ?: throw IllegalArgumentException(
        "Unknown account ${transferRecord.fromAccountPath.joinToString("/")}"
      )

      val fromOwner = fromAccountNode.top.name
      val toOwner = toAccountNode.top.name
      if (fromOwner != toOwner) {
        logger.warn {
          "WARNING: Transaction in ${transferRecord.month} has " +
              "to account ${toAccountNode.name} from ${fromAccountNode.name} with different owners " +
              "$fromOwner vs $toOwner"
        }
      }
      val fromAccount = leafToAccount[fromAccountNode]
        ?: throw java.lang.IllegalStateException("Node ${fromAccountNode.pathString} has no matching account entry")
      if (fromAccount.isClosed(transferRecord.month)) {
        throw IllegalArgumentException(
          "Account ${fromAccountNode.pathString} " +
              "has transfer during closed month ${transferRecord.month} to ${toAccountNode.pathString}"
        )
      }
      val toAccount = leafToAccount[toAccountNode]
        ?: throw java.lang.IllegalStateException("Node ${toAccountNode.pathString} has no matching account entry")
      if (toAccount.isClosed(transferRecord.month)) {
        throw IllegalArgumentException(
          "Account ${toAccountNode.pathString} " +
              "has transfer during closed month ${transferRecord.month} from ${fromAccountNode.pathString}"
        )
      }

      (statementBuilders[fromAccountNode] ?: throw IllegalStateException()).addTransfer(
          fromAccountNode,
          toAccountNode,
          transferRecord.month,
          -transferRecord.balance,
          transferRecord.description
        )
      (statementBuilders[toAccountNode] ?: throw IllegalStateException()).addTransfer(
          toAccountNode,
          fromAccountNode,
          transferRecord.month,
          transferRecord.balance,
          transferRecord.description
        )
    }
    return statementBuilders.mapValues {
      try {
        it.value.build()
      } catch (e: Exception) {
        e.addSuppressed(Exception("while processing ${it.key.pathString}"))
        throw e
      }

    }
  }

  fun build(): Budget {
    if (monthRange?.isEmpty() ?: true) {
      throw IllegalArgumentException("Budget must have at least one month.")
    }

    val treeRoot = treeNodeBuilder.build()
    val leafToAccount = pathToAccount.mapKeys {
      treeRoot[it.key] as? TreeNode.Leaf ?: throw IllegalStateException("Could not find path ${it.key}")
    }

    val nodeToStatement = mutableMapOf<TreeNode, Map<Month, Statement>>()
    val (numTransactions, elapsedTransactionTime) = timeSource.measureTimedValue {
      val transactionStatements = buildTransactionStatements(treeRoot, leafToAccount)
      nodeToStatement.putAll(transactionStatements)
      transactionStatements.values.sumOf { it.values.sumOf { stmt -> stmt.transactions.size } }
    }

    val (summaryNameMonthMap, elapsedBuildSummaryStatements) = timeSource.measureTimedValue {
      val summaryMapBuilder = SummaryMapBuilder()
      summaryMapBuilder.addAll(nodeToStatement)
      summaryMapBuilder.build(treeRoot)
    }
    nodeToStatement.putAll(summaryNameMonthMap)

    logger.info {
        "Build ${leafToAccount.size} accounts, " +
        "$numTransactions transactions in $elapsedTransactionTime, " +
        "${summaryNameMonthMap.size} summaries in $elapsedBuildSummaryStatements, " +
        "total in ${elapsedTransactionTime + elapsedBuildSummaryStatements}"
    }

    return Budget(
      monthRange!!,
      treeRoot,
      leafToAccount,
      nodeToStatement,
    )
  }

  companion object {
    private val logger = KotlinLogging.logger {}
  }
}

fun budget(block: BudgetBuilder.() -> Unit): Budget {
  val builder = BudgetBuilder()
  block(builder)
  return builder.build()
}
