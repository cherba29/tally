package com.cherba29.tally.cli.cmds

import com.cherba29.tally.core.Month
import com.cherba29.tally.core.NodeId
import com.cherba29.tally.data.Loader
import com.cherba29.tally.data.watchedEventFlow
import com.cherba29.tally.statement.Transaction
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
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import kotlin.collections.contains
import kotlin.io.path.extension
import kotlin.io.path.pathString
import kotlin.math.abs
import kotlinx.coroutines.runBlocking

class Transactions : CliktCommand() {
  override fun help(context: Context) = "List of transactions within given period."

  val tallyPath by option(envvar = "TALLY_PATH").path(mustExist = true).required()

  val account by argument(help = "Account name").optional()
  val startMonth by option(
    "--start-month",
    help="Inclusive start month, eg Apr2026"
  ).convert { Month.fromString(it) }
  val endMonth by option(
    "--end-month",
    help="Inclusive end month, eg May2026"
  ).convert { Month.fromString(it) }
  val owner: String? by option(
    "--owner",
    help="Owner of the account"
  )
  val limit: Int by option(
    "--limit",
    help="Number of transactions to list"
  ).int().default(20)

  override fun run() {
    val loader = Loader(tallyPath.watchedEventFlow {
      it.extension == "yaml" && !ignorePathRegex.containsMatchIn(it.pathString)
    })
    val budget = runBlocking { loader.budget() }.budget
    val statementTable: List<TransactionStatement> = buildTransactionStatementTable(budget, owner)
    val entries = mutableMapOf<String, MutableList<Transaction>>()
    for (transactionStatement in statementTable) {
      val stmtAccount: NodeId = transactionStatement.nodeId
      if (owner != null && owner !in stmtAccount.owners) {
        continue
      }
      if (account != null && stmtAccount.name != account) {
        continue
      }
      if (startMonth != null && transactionStatement.monthRange.last < startMonth!!) {
        continue
      }
      if (endMonth != null && endMonth!! < transactionStatement.monthRange.first) {
        continue
      }
      var accountEntries = entries[transactionStatement.nodeId.name]
      if (accountEntries == null) {
        accountEntries = mutableListOf()
        entries[transactionStatement.nodeId.name] = accountEntries
      }
      accountEntries += transactionStatement.transactions
    }
    for (accountEntries in entries.values) {
      accountEntries.sortWith(Comparator { a, b ->
        if (a.balance.date == b.balance.date) {
          abs(a.balance.amount) - abs(b.balance.amount)
        } else if (a.balance.date < b.balance.date) -1 else 1
      })
    }
    echo("Date,Amount,From,To,Description")
    for ((accountName, accountEntries) in entries) {
      for (t in accountEntries.slice(0..limit)) {
        val amount = t.balance.amount
        echo(
          "${t.balance.date},${amount.asAmount().padStart(8)}, ${
            accountName.padEnd(20)
          },${t.nodeId.name},${t.description ?: ""}\n"
        )
      }
    }
  }

  companion object {
    private val ignorePathRegex = Regex("(^_)|(/_)")
    private fun Int.asAmount(): String = "%.2f".format(this / 100.0)
  }
}