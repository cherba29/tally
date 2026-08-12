package com.cherba29.tally.data.builder

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.TreeNode
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.MonthRange
import com.cherba29.tally.core.Transfer
import com.cherba29.tally.core.plus
import com.cherba29.tally.data.Budget
import com.cherba29.tally.statement.Statement
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.time.TimeSource
import kotlin.time.measureTimedValue

class BudgetBuilder(
  // Account name -> month -> balance map.
  private val balances: MutableMap<List<String>, MutableMap<Month, Balance>> = mutableMapOf(),
  private val transferRecordList: MutableList<TransferRecord> = mutableListOf(),
  private val timeSource: TimeSource = TimeSource.Monotonic,
) {
  private var monthRange: MonthRange? = null
  data class TransferRecord(
    val toAccountName: String,  // Full path is unknown at time of record.
    val fromAccountPath: List<String>,
    val month: Month,
    val balance: Balance,
    val description: String?,
    val tags: List<String>,
  )

  private val groupTreeBuilder = TreeNode.Companion.Builder()
  private val pathToAccount = mutableMapOf<List<String>, Account>()

  fun setAccount(fullPath: List<String>, account: Account): BudgetBuilder {
    groupTreeBuilder.addPath(fullPath, account.rank)
    pathToAccount[fullPath] = account
    monthRange += account.openedOn
    monthRange += account.closedOn
    return this
  }

  fun setBalance(accountPath: List<String>, month: Month, balance: Balance): BudgetBuilder {
    val accountBalances = balances.getOrPut(accountPath) { mutableMapOf() }
    if (accountBalances.put(month, balance) != null) {
      throw IllegalArgumentException(
        "Balance for '${accountPath.joinToString("/")}' '$month' is already set to $balance")
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

  private fun buildTransfers(treeRoot: TreeNode): MutableMap<TreeNode.Leaf, MutableMap<Month, MutableList<Transfer>>> {
    val budgetTransfers: MutableMap<TreeNode.Leaf, MutableMap<Month, MutableList<Transfer>>> = mutableMapOf()
    for (transferRecord in transferRecordList) {
      val toAccounts = pathToAccount.keys.filter { it.last() == transferRecord.toAccountName }
      if (toAccounts.isEmpty()) {
        throw IllegalArgumentException(
          "Unknown to account ${transferRecord.toAccountName} in " +
              "${transferRecord.fromAccountPath.joinToString("/")}, " +
            "known accounts\n${treeRoot.toPrettyString()}")
      } else if (toAccounts.size > 1) {
        throw IllegalArgumentException(
          "Ambiguous transfer from ${transferRecord.fromAccountPath.joinToString("/")} to ${transferRecord.toAccountName}, " +
              "found multiple candidate accounts " + toAccounts.joinToString { it.joinToString("/") })
      }

      val toAccount = treeRoot[toAccounts.first()] as? TreeNode.Leaf
        ?: throw IllegalStateException("Unknown account path ${toAccounts.first().joinToString("/")}")

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

      val transfer = Transfer(
        fromAccount,
        toAccount,
        transferRecord.month,
        transferRecord.balance,
        transferRecord.description,
        transferRecord.tags,
      )
      budgetTransfers.get(toAccount, transferRecord.month).add(transfer)
      budgetTransfers.get(fromAccount, transferRecord.month).add(transfer)
    }
    return budgetTransfers
  }

  fun build(): Budget {
    val months = monthRange ?: MonthRange.EMPTY
    if (months.isEmpty()) {
      throw IllegalArgumentException("Budget must have at least one month.")
    }

    val treeRoot = groupTreeBuilder.build()
    val leafToAccount = pathToAccount.mapKeys {
      treeRoot[it.key] as? TreeNode.Leaf ?: throw IllegalStateException("Could not find path ${it.key}")
    }
    val leafToBalances = balances.mapKeys {
      treeRoot[it.key] as? TreeNode.Leaf ?: throw IllegalStateException("Could not find path ${it.key}")
    }
    val (transfers, elapsedBudgetTime) = timeSource.measureTimedValue { buildTransfers(treeRoot) }
    val nodeToStatement: MutableMap<TreeNode, Map<Month, Statement>> = mutableMapOf()

    val (transactionStatementTable, elapsedTransactionTime) = timeSource.measureTimedValue {
      val transactionStatementTable = leafToAccount.map { (leafTreeNode, account) ->
        leafTreeNode to TransactionTableBuilder.buildAccountTransactionStatements(
          leafTreeNode,
          account,
          months,
          leafToBalances[leafTreeNode] ?: mapOf(),
          transfers[leafTreeNode] ?: mapOf()
        )
      }.toMap()
      nodeToStatement.putAll(transactionStatementTable)
      transactionStatementTable
    }

    val (summaryNameMonthMap, elapsedBuildSummaryStatements) = timeSource.measureTimedValue {
      val summaryMapBuilder = SummaryMapBuilder()
      for (monthStatements in transactionStatementTable.values) {
        for (statement in monthStatements.values) {
          summaryMapBuilder.addStatement(statement)
        }
      }
      summaryMapBuilder.build(treeRoot)
    }
    nodeToStatement.putAll(summaryNameMonthMap)
    val numSummaryStatements = summaryNameMonthMap.size
    logger.info {
        "Build ${leafToAccount.size} accounts, " +
        "transfers in $elapsedBudgetTime, " +
        "${transactionStatementTable.size} transactions in $elapsedTransactionTime, " +
        "$numSummaryStatements summaries in $elapsedBuildSummaryStatements, " +
        "total in ${elapsedBudgetTime + elapsedTransactionTime + elapsedBuildSummaryStatements}"
    }

    return Budget(
      months,
      treeRoot,
      leafToAccount,
      nodeToStatement,
    )
  }

  companion object {
    private val logger = KotlinLogging.logger {}
  }
}

private fun <K1, K2, V> MutableMap<K1, MutableMap<K2, MutableList<V>>>.get(k1: K1, k2: K2): MutableList<V> {
  return getOrPut(k1) { mutableMapOf() }.getOrPut(k2) { mutableListOf() }
}

fun budget(block: BudgetBuilder.()->Unit): Budget {
  val builder = BudgetBuilder()
  block(builder)
  return builder.build()
}
