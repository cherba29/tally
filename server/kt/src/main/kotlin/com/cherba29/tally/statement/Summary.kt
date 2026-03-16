package com.cherba29.tally.statement

import com.cherba29.tally.Map3
import com.cherba29.tally.core.Account
import com.cherba29.tally.core.AccountType
import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.BalanceType
import com.cherba29.tally.core.Month
import kotlin.collections.iterator
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.sign

class SummaryStatement(account: Account, month: Month, val startMonth: Month) : Statement(account, month) {
  val statements: MutableList<Statement> = mutableListOf()

  override val isClosed: Boolean
    get() = statements.any { statement -> statement.isClosed }

  override val annualizedPercentChange: Double?
    get() {
      val prctChange = percentChange ?: return null
      val numberOfMonths = month - startMonth + 1
      val annualFrequency = 12.0 / numberOfMonths
      val result = (1 + prctChange.absoluteValue / 100).pow(annualFrequency) - 1
      // Annualized percentage change is not that meaningful if large.
      return if (result < 10) 100 * prctChange.sign * result else null
    }

  fun addStatement(statement: Statement) {
    if (statement.month.compareTo(month) != 0) {
      throw IllegalArgumentException(
        "${statement.account.name} statement for month ${statement.month} is being added to summary for month $month"
      )
    }
    if (statement.isClosed) {
      // Does not contribute to the summary.
      return
    }
    if (startBalance == null) {
      startBalance = statement.startBalance
    } else if (statement.startBalance != null) {
      startBalance = Balance.add(startBalance!!, statement.startBalance!!)
    }
    if (endBalance == null) {
      endBalance = statement.endBalance
    } else if (statement.endBalance != null) {
      endBalance = Balance.add(endBalance!!, statement.endBalance!!)
    }
    addInFlow(statement.inFlows)
    addOutFlow(statement.outFlows)
    totalTransfers += statement.totalTransfers
    totalPayments += statement.totalPayments
    income += statement.income
    statements.add(statement)
  }
}

// TODO: add tests for this class.
class SummaryStatementAggregator {
  // Map of owner -> 'summary name' -> month -> 'summary statement'.
  val summaryStatements = Map3<SummaryStatement>()
  private val summaryAccounts: MutableMap<String, Account> = mutableMapOf()

  fun addStatement(summaryName: String, owner: String, statement: Statement) {
    val summaryAccount = getAccount(summaryName, owner, statement.account.path)

    val accountMonthSummaryStatement = summaryStatements.getDefault(
      owner, summaryAccount.name, statement.month.toString()
    ) {
      SummaryStatement(summaryAccount, statement.month, startMonth = statement.month)
    }
    accountMonthSummaryStatement.addStatement(statement)
  }

  private fun getAccount(name: String, owner: String, path: List<String>): Account {
    val key = "$owner - $name"
    var account = summaryAccounts[key]
    if (account == null) {
      account = Account(name, type = AccountType.SUMMARY, owners = listOf(owner), path = path.slice(0..path.size - 2))
      summaryAccounts[key] = account
    }
    return account
  }

  // Make sure totals are computed for parent summary accounts up the path to the root.
  fun propagateUpThePath2() {
    // Build a multi-root tree based on account paths for each owner.
    val tree: MutableMap<String, MutableSet<String>> = mutableMapOf()  // node -> set of children.
    val owners: MutableSet<String> = mutableSetOf()
    for (account in summaryAccounts.values) {
      for (owner in account.owners) {
        owners.add(owner)
        if (!account.name.startsWith('/')) continue
        val path = account.path
        var entry = "/" + owner + account.name
        for (sub in path.size downTo 0) {
          val subPath = path.slice(0..sub - 1)
          val subPathId = "/" + owner + "/" + subPath.joinToString("/")
          var subTreeEntry = tree[subPathId]
          if (subTreeEntry == null) {
            subTreeEntry = mutableSetOf()
            tree[subPathId] = subTreeEntry
          }
          if (subPathId != entry) {  // Make sure root does not reference itself.
            subTreeEntry.add(entry)
          }
          entry = subPathId
        }
      }
    }
    // For each owner bottom up, build up summaries.
    for (owner in owners) {
      val ownerRoot = "/$owner/"
      for (node in traverseBottomUp(ownerRoot, tree)) {
        val fullPath = node.split('/')
        val summaryId = "/" + fullPath.subList(2, fullPath.size).joinToString("/")
        // skip this is root node it does not need to be added to anything.
        if (summaryId == "/") continue
        val monthlyStatements = summaryStatements.get2(owner, summaryId)
          ?: throw IllegalStateException(
            "$node has no monthly statements, [$owner, $summaryId] key not found."
          )  // Should never happen.

        val parentSummaryId = '/' + fullPath.subList(2, fullPath.lastIndex).joinToString("/")
        for (monthlyStatement in monthlyStatements.values) {
          addStatement(parentSummaryId, owner, monthlyStatement)
        }
      }
    }
  }
}

fun traverseBottomUp(root: String, tree: Map<String, Set<String>>): Sequence<String> = sequence {
  for (child in tree[root] ?: setOf()) {
    yieldAll(traverseBottomUp(child, tree))
  }
  yield(root)
}

// TODO: add tests.
fun buildSummaryStatementTable(
  statements: List<TransactionStatement>,
  selectedOwner: String?
): Map3<SummaryStatement> {
  val statementsAggregator = SummaryStatementAggregator()
  for (statement in statements) {
    for (owner in statement.account.owners) {
      if (selectedOwner != null && owner != selectedOwner || statement.isEmpty()) {
        continue
      }
      val summariesToAddTo = mutableListOf("$owner ${statement.account.typeIdName}", "$owner SUMMARY")
      if (statement.account.path.isNotEmpty()) {
        summariesToAddTo.add("/" + statement.account.path.joinToString("/"))
      }
      for (summaryName in summariesToAddTo) {
        if (statement.account.isExternal && summaryName.contains("SUMMARY")) {
          continue
        }
        statementsAggregator.addStatement(summaryName, owner, statement)
      }
    }
  }
  statementsAggregator.propagateUpThePath2()
  return statementsAggregator.summaryStatements
}

// TODO: add tests.
fun combineSummaryStatements(summaryStatements: List<SummaryStatement>): SummaryStatement {
  if (summaryStatements.isEmpty()) {
    throw IllegalArgumentException("Cant combine empty list of summary statements")
  }
  var stmtName: String? = null
  var owners: List<String> = listOf()
  var minMonth: Month? = null
  var maxMonth: Month? = null
  // Map of 'account name' -> month -> 'summary statement'.
  val accountStatements = mutableMapOf<String, MutableMap<String, Statement>>()

  for (summaryStmt in summaryStatements) {
    if (stmtName == null) {
      stmtName = summaryStmt.account.name
      owners = summaryStmt.account.owners
    } else if (stmtName !== summaryStmt.account.name) {
      throw IllegalArgumentException(
        "Cant combine different summary statements $stmtName and ${summaryStmt.account.name}"
      )
    }
    if (minMonth == null || summaryStmt.month < minMonth) {
      minMonth = summaryStmt.month
    }
    if (maxMonth == null || maxMonth < summaryStmt.month) {
      maxMonth = summaryStmt.month
    }
    for (stmt in summaryStmt.statements) {
      var accountMonthlyStatements = accountStatements[stmt.account.name]
      if (accountMonthlyStatements == null) {
        accountMonthlyStatements = mutableMapOf()
        accountStatements[stmt.account.name] = accountMonthlyStatements
      }
      val monthStatement = accountMonthlyStatements[stmt.month.toString()]
      if (monthStatement != null) {
        throw IllegalArgumentException("Duplicate month statement for ${stmt.account.name} for ${stmt.month}")
      }
      accountMonthlyStatements[stmt.month.toString()] = stmt
    }
  }
  if (stmtName == null) {
    throw IllegalArgumentException("Statements do not have name set.")
  }
  if (minMonth == null) {
    throw IllegalArgumentException("Could not determine start month")
  }
  if (maxMonth == null) {
    throw IllegalArgumentException("Could not determine end month")
  }
  val summaryAccount = Account(name = stmtName, type = AccountType.SUMMARY, owners = owners)
  val combined = SummaryStatement(summaryAccount, maxMonth, minMonth)
  for ((acctName, acctStatements) in accountStatements) {
    // Combine all statements for a given account over all months in the range.
    val stmt = combineAccountStatements(
      Account(name = acctName, type = AccountType.SUMMARY, owners = listOf()),
      minMonth,
      maxMonth,
      acctStatements
    )
    combined.addStatement(stmt)
  }
  return combined
}

class CombinedStatement(account: Account, month: Month, val startMonth: Month) : Statement(account, month) {
  override val annualizedPercentChange: Double?
    get() {
      val prctChange = percentChange ?: return null
      val numberOfMonths = month - startMonth + 1
      val annualFrequency = 12.0 / numberOfMonths
      val result = (1 + abs(prctChange) / 100).pow(annualFrequency) - 1
      // Annualized percentage change is not that meaningful if large.
      return if (result < 10) 100 * prctChange.sign * result else null
    }

  override val isClosed: Boolean get() = false
}

class EmptyStatement(account: Account, month: Month) : Statement(account, month) {
  override val isClosed: Boolean get() = false
}

fun makeProxyStatement(
  account: Account,
  month: Month,
  currStmt: Statement?,
  prevStmt: Statement?,
  nextStmt: Statement?
): Statement {
  val stmt = currStmt ?: EmptyStatement(account, month)
  if (stmt.startBalance == null) {
    stmt.startBalance = prevStmt?.endBalance ?: Balance(0, month.toDate(), BalanceType.PROJECTED)
  }
  if (stmt.endBalance == null) {
    stmt.endBalance = nextStmt?.startBalance ?: Balance(0, month.toDate(), BalanceType.PROJECTED)
  }
  return stmt
}

fun combineAccountStatements(
  account: Account,
  startMonth: Month,
  endMonth: Month,
  statements: Map<String, Statement>
): Statement {
  val combined = CombinedStatement(account, endMonth, startMonth)
  for (currentMonth in startMonth..endMonth) {
    val stmt: Statement = makeProxyStatement(
      account,
      currentMonth,
      statements[currentMonth.toString()],
      statements[currentMonth.previous().toString()],
      statements[currentMonth.next().toString()]
    )
    if (
      combined.startBalance == null ||
      (stmt.startBalance != null && combined.startBalance!!.date > stmt.startBalance!!.date)
    ) {
      combined.startBalance = stmt.startBalance
    }
    if (
      combined.endBalance == null ||
      (stmt.endBalance != null && combined.endBalance!!.date < stmt.endBalance!!.date)
    ) {
      combined.endBalance = stmt.endBalance
    }
    combined.addInFlow(stmt.inFlows)
    combined.addOutFlow(stmt.outFlows)
    combined.totalTransfers += stmt.totalTransfers
    combined.totalPayments += stmt.totalPayments
    combined.income += stmt.income
  }
  return combined
}

// private val logger = KotlinLogging.logger {}