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
import com.cherba29.tally.statement.TransactionStatement
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.collections.iterator
import kotlin.collections.set
import kotlin.time.TimeSource
import kotlin.time.measureTimedValue

class BudgetBuilder(
  // Account name -> month -> balance map.
  private val balances: MutableMap<List<String>, MutableMap<Month, Balance>> = mutableMapOf(),
  private val transfers: MutableList<TransferData> = mutableListOf(),
  private val timeSource: TimeSource = TimeSource.Monotonic,
) {
  private var monthRange: MonthRange? = null
  data class TransferData(
    val toAccountName: String,  // Full path is unknown at time of record.
    val toMonth: Month,
    val fromAccountPath: List<String>,
    val fromMonth: Month,
    val balance: Balance,
    val description: String?,
  )

  private val groupTreeBuilder = TreeNode.Companion.Builder()
  private val pathToAccount = mutableMapOf<List<String>, Account>()

  fun setAccount(fullPath: List<String>, account: Account): BudgetBuilder {
    groupTreeBuilder.addPath(fullPath)
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
    monthRange += toMonth
    monthRange += fromMonth
  }

  private fun buildTransfers(tree: TreeNode): MutableMap<TreeNode.Leaf, MutableMap<Month, MutableList<Transfer>>> {
    val budgetTransfers: MutableMap<TreeNode.Leaf, MutableMap<Month, MutableList<Transfer>>> = mutableMapOf()
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

      val toAccount = tree[toAccounts.first()] as? TreeNode.Leaf
        ?: throw IllegalStateException("Unknown account path ${toAccounts.first().joinToString("/")}")

      val fromAccount = tree[transferData.fromAccountPath] as? TreeNode.Leaf ?: throw IllegalArgumentException(
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
        fromAccount.path,
        toAccount.path,
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
    val months = monthRange ?: MonthRange.Companion.EMPTY
    val tree = groupTreeBuilder.build()
    val leafToAccount = pathToAccount.mapKeys {
      tree[it.key] as? TreeNode.Leaf ?: throw IllegalStateException("Could not find path ${it.key}")
    }
    val leafToBalances = balances.mapKeys {
      tree[it.key] as? TreeNode.Leaf ?: throw IllegalStateException("Could not find path ${it.key}")
    }
    val (transfers, elapsedBudgetTime) = timeSource.measureTimedValue { buildTransfers(tree) }
    val nodeToStatement: MutableMap<TreeNode, MutableMap<Month, Statement>> = mutableMapOf()
    val leafToTransfers = transfers.mapKeys {
      tree[it.key.path] as? TreeNode.Leaf
        ?: throw java.lang.IllegalStateException("Could not find path ${it.key.path}")
    }

    val (transactionStatementTable, elapsedTransactionTime) = timeSource.measureTimedValue {
      // TODO: this might throw due to so invariant being violated. Need to recover to previous state.
      val transactionStatementTable = buildTransactionStatementTable(
        tree,
        months, leafToAccount,
        leafToBalances, leafToTransfers,  owner = null)
      for (stmt in transactionStatementTable) {
        nodeToStatement.getOrPut(stmt.treeNode) { mutableMapOf() }[stmt.monthRange.first] = stmt
      }
      transactionStatementTable
    }
    logger.info {
      "Done building ${transactionStatementTable.size} transaction statements in ${elapsedTransactionTime}ms"
    }

    val (summaryNameMonthMap, elapsedBuildSummaryStatements) = timeSource.measureTimedValue {
      val summaryStatementBuilder = SummaryStatementBuilder()
      for (statement in transactionStatementTable) {
        summaryStatementBuilder.addStatement(statement)
      }
      summaryStatementBuilder.build(tree)
    }
    for ((treeNode, monthToSummary) in summaryNameMonthMap) {
      nodeToStatement.getOrPut(treeNode) { mutableMapOf() }.putAll(monthToSummary)
    }
    val numSummaryStatements = summaryNameMonthMap.size
    logger.info {
      "Done building $numSummaryStatements summary statements in $elapsedBuildSummaryStatements"
    }
    // TODO: Show all timing info in one log line.
    logger.info {
      "Done reprocessing ${leafToAccount.size} file(s) ${transactionStatementTable.size} tran statements and $numSummaryStatements summaries in ${
        elapsedBudgetTime + elapsedTransactionTime + elapsedBuildSummaryStatements
      }"
    }

    return Budget(
      months,
      tree,
      leafToAccount,
      nodeToStatement,
    )
  }

  fun buildTransactionStatementTable(
    treeRoot: TreeNode,
    months: MonthRange,
    leafToAccountMap: Map<TreeNode.Leaf, Account>,
    leafToMonthlyBalancesMap: Map<TreeNode.Leaf, Map<Month, Balance>>,
    leafToMonthlyTransfersMap: Map<TreeNode.Leaf, Map<Month, List<Transfer>>>,
    owner: String?
  ): List<TransactionStatement> {
    val statementTable = mutableListOf<TransactionStatement>()

    // Working backwards.
    val months = months.sortedDescending()
    if (months.isEmpty()) {
      throw IllegalArgumentException("Budget must have at least one month.")
    }

    for ((leafTreeNode, account) in leafToAccountMap) {
      if (owner != null && owner !in leafTreeNode.path.first()) {
        continue
      }
      val accountStatements = mutableListOf<TransactionStatement>()
      val monthlyTransfers = leafToMonthlyTransfersMap[leafTreeNode] ?: mapOf()
      val monthlyBalances = leafToMonthlyBalancesMap[leafTreeNode] ?: mapOf()

      // Make statement outside range so that its attributes relating to previous can be used.
      val nextMonth = months.first().next()
      var nextMonthStatement = TransactionStatement.fromTransfers(
        treeRoot,
        leafTreeNode,
        nextMonth..nextMonth,
        account.isClosed(nextMonth),
        monthlyTransfers[nextMonth],
        monthlyBalances[nextMonth]
      )
      for (month in months) {
        val statement = TransactionStatement.fromTransfers(
          treeRoot,
          leafTreeNode,
          month..month,
          account.isClosed(month),
          monthlyTransfers[month],
          monthlyBalances[month]
        )
        statement.endBalance = nextMonthStatement.startBalance
        statement.isCovered =
          statement.endBalance == null || statement.endBalance!!.amount >= 0 || nextMonthStatement.coversPrevious
        statement.isProjectedCovered = statement.isCovered || nextMonthStatement.coversProjectedPrevious
        nextMonthStatement = statement
        if (statement.startBalance != null) {
          var prevBalance = statement.startBalance!!.amount
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

  companion object {
    private val logger = KotlinLogging.logger {}

    private fun <T> getMonthTransfers(
      transfers: MutableMap<TreeNode.Leaf, MutableMap<Month, MutableList<T>>>,
      leafTreeNode: TreeNode.Leaf,
      month: Month
    ): MutableList<T> {
      var accountTransfers = transfers[leafTreeNode]
      if (accountTransfers == null) {
        accountTransfers = mutableMapOf()
        transfers[leafTreeNode] = accountTransfers
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
