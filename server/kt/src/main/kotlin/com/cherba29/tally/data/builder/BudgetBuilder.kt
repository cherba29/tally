package com.cherba29.tally.data.builder

import com.cherba29.tally.core.*
import com.cherba29.tally.data.Budget
import com.cherba29.tally.statement.Statement
import com.cherba29.tally.statement.TransactionStatement
import io.github.oshai.kotlinlogging.KotlinLogging
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
      val toAccount = cachedNameToTreenodeMap.getOrPut(transferRecord.toAccountName) {
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
      val fromAccount = treeRoot[transferRecord.fromAccountPath] as? TreeNode.Leaf ?: throw IllegalArgumentException(
        "Unknown account ${transferRecord.fromAccountPath.joinToString("/")}"
      )

      val fromOwner = fromAccount.top.name
      val toOwner = toAccount.top.name
      if (fromOwner != toOwner) {
        logger.warn {
          "WARNING: Transaction in ${transferRecord.month} has " +
              "to account ${toAccount.name} from ${fromAccount.name} with different owners " +
              "$fromOwner vs $toOwner"
        }
      }

      (statementBuilders[fromAccount] ?: throw IllegalStateException()).addTransfer(
          fromAccount,
          toAccount,
          transferRecord.month,
          -transferRecord.balance,
          transferRecord.description
        )
      (statementBuilders[toAccount] ?: throw IllegalStateException()).addTransfer(
          toAccount,
          fromAccount,
          transferRecord.month,
          transferRecord.balance,
          transferRecord.description
        )
    }
    return statementBuilders.mapValues { it.value.build() }
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
