package com.cherba29.tally.data.builder

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Account.Companion.EXTERNAL_NAME
import com.cherba29.tally.core.Account.Companion.INACTIVE_NAME
import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.MonthRange
import com.cherba29.tally.utils.TreeNode
import com.cherba29.tally.core.plus
import com.cherba29.tally.data.Budget
import com.cherba29.tally.data.BudgetLoadingException
import com.cherba29.tally.data.Profile
import com.cherba29.tally.statement.Statement
import com.cherba29.tally.statement.TransactionStatement
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.math.abs
import kotlin.time.TimeSource
import kotlin.time.measureTimedValue

class BudgetBuilder {
  // Account name -> month -> balance map.
  private val balances: MutableMap<List<String>, MutableMap<Month, Balance>> = mutableMapOf()
  private val transferRecordList: MutableList<TransferRecord> = mutableListOf()
  private val accountSourceFiles: MutableMap<List<String>, Path> = mutableMapOf()
  private val timeSource: TimeSource = TimeSource.Monotonic

  private var monthRange: MonthRange? = null

  data class TransferRecord(
    val toAccountName: String,  // Full path is unknown at time of record.
    val fromAccountPath: List<String>,
    val month: Month,
    val balance: Balance,
    val description: String?,
    val tags: List<String>,
    val sourceFile: Path? = null,
    val lineNumber: Int? = null,
  )

  private val treeNodeBuilder = TreeNode.Builder<Profile>()
  private val pathToAccount = mutableMapOf<List<String>, Account>()

  fun addAccount(
    account: Account,
    accountBalances: Map<Month, Balance>,
    sourceFile: Path? = null,
  ) {
    for (owner in account.owners) {
      val fullPath = listOf(owner) + account.path + listOf(account.name)
      if (sourceFile != null) {
        accountSourceFiles[fullPath] = sourceFile
      }
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

  private val processedPath = mutableSetOf<List<String>>()
  fun setAccount(fullPath: List<String>, account: Account): BudgetBuilder {
    for (i in fullPath.indices) {
      val subPath = fullPath.subList(0, i)
      if (processedPath.add(subPath)) {
        treeNodeBuilder.addPath(
          subPath, Profile(
            isExternal = subPath.contains(EXTERNAL_NAME),
            isInactive = subPath.contains(INACTIVE_NAME)
          )
        )
      }
    }
    treeNodeBuilder.addPath(fullPath, Profile(
      account,
      isExternal = fullPath.contains(EXTERNAL_NAME),
      isInactive = fullPath.contains(INACTIVE_NAME)
    ), account.rank)

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
    tags: List<String> = listOf(),
    sourceFile: Path? = null,
    lineNumber: Int? = null,
  ) {
    for (owner in fromAccount.owners) {
      val fromAccountPath = listOf(owner) + fromAccount.path + listOf(fromAccount.name)
      addTransfer(TransferRecord(toAccountName, fromAccountPath, month, balance, description, tags, sourceFile, lineNumber))
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
    treeRoot: TreeNode<Profile>
  ): Map<TreeNode.Leaf<Profile>, Map<Month, TransactionStatement>> {
    // Backfill external inactive account balances.
    for (accountNode in treeRoot.traverseLeaves()) {
      if (accountNode.data.isInactive) {
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

    val statementBuilders = mutableMapOf<TreeNode.Leaf<Profile>, MonthTransactionStatementBuilder>()

    statementBuilders.putAll(
      treeRoot.traverseLeaves().associateWith {
        val builder = MonthTransactionStatementBuilder()
        builder.months = monthRange!!
        builder.monthlyBalances = leafToBalances[it] ?: mapOf()
        builder
      })

    val cachedNameToTreenodeMap = mutableMapOf<String, TreeNode.Leaf<Profile>>()

    for ((toAccountName, fromAccountPath, month, balance, description) in transferRecordList) {
      val toAccountNode = cachedNameToTreenodeMap.getOrPut(toAccountName) {
        val toAccounts = pathToAccount.keys.filter { it.last() == toAccountName }
        if (toAccounts.isEmpty()) {
          throw IllegalArgumentException(
            "Unknown to account $toAccountName in " +
                "${fromAccountPath.joinToString("/")}, " +
                "known accounts\n${treeRoot.toPrettyString()}"
          )
        } else if (toAccounts.size > 1) {
          throw IllegalArgumentException(
            "Ambiguous transfer from ${fromAccountPath.joinToString("/")} to " +
                "$toAccountName, found multiple candidate accounts " +
                toAccounts.joinToString { it.joinToString("/") })
        }

        treeRoot[toAccounts.first()] as? TreeNode.Leaf
          ?: throw IllegalStateException("Unknown account path ${toAccounts.first().joinToString("/")}")
      }
      val fromAccountNode = treeRoot[fromAccountPath] as? TreeNode.Leaf ?: throw IllegalArgumentException(
        "Unknown account ${fromAccountPath.joinToString("/")}"
      )

      val fromOwner = fromAccountNode.top.name
      val toOwner = toAccountNode.top.name
      if (fromOwner != toOwner) {
        logger.warn {
          "WARNING: Transaction in $month has " +
              "to account ${toAccountNode.name} from ${fromAccountNode.name} with different owners " +
              "$fromOwner vs $toOwner"
        }
      }
      val fromAccount = fromAccountNode.data.account
        ?: throw java.lang.IllegalStateException("Node ${fromAccountNode.pathString} has no matching account entry")
      if (fromAccount.isClosed(month)) {
        throw IllegalArgumentException(
          "Account ${fromAccountNode.pathString} " +
              "has transfer during closed month $month to ${toAccountNode.pathString}"
        )
      }
      val toAccount = toAccountNode.data.account
        ?: throw java.lang.IllegalStateException("Node ${toAccountNode.pathString} has no matching account entry")
      if (toAccount.isClosed(month)) {
        throw IllegalArgumentException(
          "Account ${toAccountNode.pathString} " +
              "has transfer during closed month $month from ${fromAccountNode.pathString}"
        )
      }

      (statementBuilders[fromAccountNode] ?: throw IllegalStateException()).addTransfer(
          fromAccountNode,
          toAccountNode,
          month,
          -balance,
          description
        )
      (statementBuilders[toAccountNode] ?: throw IllegalStateException()).addTransfer(
          toAccountNode,
          fromAccountNode,
          month,
          balance,
          description
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

  fun validate(treeRoot: TreeNode<Profile> = treeNodeBuilder.build()): BudgetBuilder {
    validateBalances()
    validateTransfers(treeRoot)
    return this
  }

  private fun validateBalances() {
    for ((accountPath, accountBalances) in balances) {
      val sourceFile = accountSourceFiles[accountPath] ?: continue
      for ((month, balance) in accountBalances) {
        val balanceMonth = Month.fromDate(balance.date)
        val balanceMonthDiff = abs(balanceMonth - month)
        if (balanceMonthDiff > 2) {
          val accountName = accountPath.last()
          val fileContext = ", while processing $sourceFile"
          throw BudgetLoadingException(
            "For $accountName account $balance and month $month are $balanceMonthDiff months apart (2 max)$fileContext",
            fileName = sourceFile.toString()
          )
        }
      }
    }
  }

  private fun validateTransfers(treeRoot: TreeNode<Profile>) {
    val cachedNameToTreenodeMap = mutableMapOf<String, TreeNode.Leaf<Profile>>()
    for ((toAccountName, fromAccountPath, month, balance, _, _, sourceFile1, lineNumber) in transferRecordList) {
      val balanceMonth = Month.fromDate(balance.date)
      val balanceMonthDiff = abs(balanceMonth - month)
      val sourceFile = sourceFile1 ?: accountSourceFiles[fromAccountPath]
      val fileContext = sourceFile?.let {
        val lineInfo = lineNumber?.let { line -> ":$line" } ?: ""
        ", while processing $it$lineInfo"
      } ?: ""

      if (balanceMonthDiff > 2 && sourceFile != null) {
        val fromAccountName = fromAccountPath.last()
        throw BudgetLoadingException(
          "For account '$fromAccountName' transfer to '$toAccountName' " +
              "for $month date ${balance.date} ($balanceMonth) are too far apart$fileContext",
          fileName = sourceFile.toString(),
          lineNumber = lineNumber
        )
      }

      val toAccountNode = cachedNameToTreenodeMap.getOrPut(toAccountName) {
        val toAccounts = pathToAccount.keys.filter { it.last() == toAccountName }
        if (toAccounts.isEmpty()) {
          throw BudgetLoadingException(
            "Unknown to account $toAccountName in " +
                "${fromAccountPath.joinToString("/")}, " +
                "known accounts\n${treeRoot.toPrettyString()}$fileContext",
            fileName = sourceFile?.toString(),
            lineNumber = lineNumber
          )
        } else if (toAccounts.size > 1) {
          throw BudgetLoadingException(
            "Ambiguous transfer from ${fromAccountPath.joinToString("/")} to " +
                "$toAccountName, found multiple candidate accounts " +
                toAccounts.joinToString { it.joinToString("/") } + fileContext,
            fileName = sourceFile?.toString(),
            lineNumber = lineNumber
          )
        }

        treeRoot[toAccounts.first()] as? TreeNode.Leaf
          ?: throw IllegalStateException("Unknown account path ${toAccounts.first().joinToString("/")}")
      }

      val fromAccountNode = treeRoot[fromAccountPath] as? TreeNode.Leaf
        ?: throw BudgetLoadingException(
          "Unknown account ${fromAccountPath.joinToString("/")}$fileContext",
          fileName = sourceFile?.toString(),
          lineNumber = lineNumber
        )

      val fromAccount = fromAccountNode.data.account
        ?: throw IllegalStateException("Node ${fromAccountNode.pathString} has no matching account entry")
      if (fromAccount.isClosed(month)) {
        throw BudgetLoadingException(
          "Account ${fromAccountNode.pathString} " +
              "has transfer during closed month $month to ${toAccountNode.pathString}$fileContext",
          fileName = sourceFile?.toString(),
          lineNumber = lineNumber
        )
      }

      val toAccount = toAccountNode.data.account
        ?: throw IllegalStateException("Node ${toAccountNode.pathString} has no matching account entry")
      if (toAccount.isClosed(month)) {
        throw BudgetLoadingException(
          "Account ${toAccountNode.pathString} " +
              "has transfer during closed month $month from ${fromAccountNode.pathString}$fileContext",
          fileName = sourceFile?.toString(),
          lineNumber = lineNumber
        )
      }
    }
  }

  fun build(): Budget {
    if (monthRange?.isEmpty() ?: true) {
      throw IllegalArgumentException("Budget must have at least one month.")
    }

    val treeRoot = treeNodeBuilder.build()
    val leafNodes = pathToAccount.map {
      treeRoot[it.key] as? TreeNode.Leaf ?: throw IllegalStateException("Could not find path ${it.key}")
    }.toSet()

    validate(treeRoot)

    val nodeToStatement = mutableMapOf<TreeNode<Profile>, Map<Month, Statement>>()
    val (numTransactions, elapsedTransactionTime) = timeSource.measureTimedValue {
      val transactionStatements = buildTransactionStatements(treeRoot)
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
        "Build ${leafNodes.size} accounts, " +
        "$numTransactions transactions in $elapsedTransactionTime, " +
        "${summaryNameMonthMap.size} summaries in $elapsedBuildSummaryStatements, " +
        "total in ${elapsedTransactionTime + elapsedBuildSummaryStatements}"
    }

    return Budget(
      monthRange!!,
      treeRoot,
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
