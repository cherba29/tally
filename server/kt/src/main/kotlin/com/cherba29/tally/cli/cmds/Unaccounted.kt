package com.cherba29.tally.cli.cmds

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.NodeId
import com.cherba29.tally.data.Loader
import com.cherba29.tally.data.watchedEventFlow
import com.cherba29.tally.statement.TransactionStatement
import com.cherba29.tally.statement.buildTransactionStatementTable
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import kotlin.io.path.extension
import kotlin.io.path.pathString
import kotlin.math.abs
import kotlin.math.min
import kotlinx.coroutines.runBlocking

class Unaccounted : CliktCommand() {
  override fun help(context: Context) = "List of periods with unaccounted balances."

  val tallyPath by option(envvar = "TALLY_PATH").path(mustExist = true).required()

  val account by argument(help = "Account name").optional()
  val startMonth by option(
    "--start-month",
    help = "Inclusive start month, eg Apr2026"
  ).convert { Month.fromString(it) }
  val endMonth by option(
    "--end-month",
    help = "Inclusive end month, eg May2026"
  ).convert { Month.fromString(it) }
  val owner: String? by option(
    "--owner",
    help = "Owner of the account"
  )
  val includeProjected: Boolean by option(
    "--include-projected",
    help = "Number of transactions to list"
  ).boolean().default(false)
  val limit: Int by option(
    "--limit",
    help = "Number of transactions to list"
  ).int().default(20)

  override fun run() {
    val loader = Loader(tallyPath.watchedEventFlow {
      it.extension == "yaml" && !ignorePathRegex.containsMatchIn(it.pathString)
    })
    val budget = runBlocking { loader.budget() }.budget
    val statementTable: List<TransactionStatement> = buildTransactionStatementTable(budget, owner)

    val unaccountedEntries = mutableListOf<UnaccountedEntry>()
    for (transactionStatement in statementTable) {
      val stmtAccount: NodeId = transactionStatement.nodeId;
      if (transactionStatement.isClosed) {
        continue;
      }
      if (owner != null && owner !in stmtAccount.owners) {
        continue;
      }
      if (account != null && stmtAccount.name != account) {
        continue;
      }
      if (startMonth != null && transactionStatement.monthRange.last < startMonth!!) {
        continue
      }
      if (endMonth != null && endMonth!! < transactionStatement.monthRange.first) {
        continue
      }
      val unaccounted = transactionStatement.unaccounted
      if (
        unaccounted != 0 &&
        (includeProjected || transactionStatement.endBalance?.type == Balance.Type.CONFIRMED)
      ) {
        unaccountedEntries += UnaccountedEntry(stmtAccount, transactionStatement)
      }
    }
    unaccountedEntries.sortWith { a, b ->
      if (a.statement.unaccounted != null && b.statement.unaccounted != null) {
        abs(b.statement.unaccounted!!) - abs(a.statement.unaccounted!!)
      } else if (a.statement.unaccounted == null) {
        if (b.statement.unaccounted == null) {
          b.statement.transactions.size - a.statement.transactions.size
        }
        abs((b.statement.unaccounted ?: 0) / 100) - a.statement.transactions.size
      } else b.statement.transactions.size - abs((a.statement.unaccounted ?: 0) / 100)
    }
    for (entry in unaccountedEntries.slice(0..< min(unaccountedEntries.size, limit))) {
      val unnacountedValue = if (entry.statement.unaccounted != 0) {
        entry.statement.unaccounted!!.asAmount().padStart(10)
      } else {
        "---"
      }
      val numTransfers = entry.statement.transactions.size
      echo(
        "${entry.statement.monthRange.first} $unnacountedValue ${entry.account.name} $numTransfers transfers"
      );
    }
  }

  companion object {
    private val ignorePathRegex = Regex("(^_)|(/_)")
    private fun Int.asAmount(): String = "%.2f".format(this / 100.0)
    data class UnaccountedEntry(
      val account: NodeId,
      val statement: TransactionStatement
    )
  }
}
