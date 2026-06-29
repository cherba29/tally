package com.cherba29.tally.testing

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Balance
import com.cherba29.tally.schema.GqlAccount
import com.cherba29.tally.schema.GqlBalance
import com.cherba29.tally.schema.GqlStatement
import com.cherba29.tally.schema.GqlSummaryData
import com.cherba29.tally.schema.GqlSummaryStatement
import com.cherba29.tally.schema.GqlTable
import com.cherba29.tally.schema.GqlTableCell
import com.cherba29.tally.schema.GqlTableRow
import com.cherba29.tally.schema.GqlTransaction
import com.cherba29.tally.statement.Statement
import com.cherba29.tally.statement.Transaction
import com.cherba29.tally.statement.TransactionStatement
import com.fasterxml.jackson.databind.node.ObjectNode
import kotlin.math.absoluteValue

fun Account.toObjectNode(root: ObjectNode) {
  root.put("__type", this.javaClass.simpleName)
  root.put("name", name)
  if (path.isNotEmpty()) {
    val pathNode = root.putArray("path")
    path.forEach { pathNode.add(it) }
  }
  if (owners.isNotEmpty()) {
    val ownersNode = root.putArray("owner")
    owners.forEach { ownersNode.add(it) }
  }
  if (description != null) {
    root.put("desc", description)
  }
  if (number != null) {
    root.put("number", number)
  }
  root.put("openedOn", openedOn.toString())

  if (closedOn != null) {
    root.put("closedOn", closedOn.toString())
  }
  if (url != null) {
    root.put("url", url)
  }
  if (address != null) {
    root.put("address", address)
  }
  if (phone != null) {
    root.put("phone", phone)
  }
  if (userName != null) {
    root.put("userName", userName)
  }
  if (password != null) {
    root.put("password", password)
  }
}

fun Statement.toObjectNode(root: ObjectNode) {
  root.put("__type", "Statement")
  val pathNode = root.putArray("path")
  treeNode.path.forEach { pathNode.add(it) }

  root.put("months", monthRange.toString())
  startBalance?.toObjectNode(root.putObject("startBalance"))
  endBalance?.toObjectNode(root.putObject("endBalance"))
  if (inFlows != 0L) {
    root.put("inFlows", inFlows)
  }
  if (outFlows != 0L) {
    root.put("outFlows", outFlows)
  }
  if (totalTransfers != 0L) {
    root.put("totalTransfers", totalTransfers)
  }
  if (totalPayments != 0L) {
    root.put("totalPayments", totalPayments)
  }
  if (income != 0L) {
    root.put("income", income)
  }
}

fun GqlStatement.toObjectNode(root: ObjectNode) {
  root.put("__type", "GqlStatement")
  root.put("name", name)
  root.put("month", month.toString())
  root.put("isClosed", isClosed)
  root.put("isCovered", isCovered)
  root.put("isProjectedCovered", isProjectedCovered)
  root.put("hasProjectedTransfer", hasProjectedTransfer)
  startBalance?.toObjectNode(root.putObject("startBalance"))
  endBalance?.toObjectNode(root.putObject("endBalance"))
  if (inFlows != 0L) {
    root.put("inFlows", inFlows)
  }
  if (outFlows != 0L) {
    root.put("outFlows", outFlows)
  }
  if (income != 0L) {
    root.put("income", income)
  }
  if (totalPayments != 0L) {
    root.put("totalPayments", totalPayments)
  }
  if (totalTransfers != 0L) {
    root.put("totalTransfers", totalTransfers)
  }
  if (change != 0L) {
    root.put("change", change)
  }
  if (addSub != 0L) {
    root.put("addSub", addSub)
  }
  if (percentChange.absoluteValue > 0.00001f) {
    root.put("percentChange", percentChange)
  }
  if (annualizedPercentChange.absoluteValue > 0.00001f) {
    root.put("annualizedPercentChange", annualizedPercentChange)
  }
  if (unaccounted != 0L) {
    root.put("unaccounted", unaccounted)
  }
  if (transactions.isNotEmpty()) {
    val transactionsNode = root.putArray("transactions")
    transactions.forEach { it.toObjectNode(transactionsNode.addObject()) }
  }
}

fun Balance.toObjectNode(root: ObjectNode) {
  root.put("__type", this.javaClass.simpleName)
  root.put("amount", amount)
  root.put("date", date.toString())
  root.put("type", type.toString())
  if (description.isNotEmpty()) {
    root.put("desc", description)
  }
}

fun GqlBalance.toObjectNode(root: ObjectNode) {
  root.put("__type", this.javaClass.simpleName)
  root.put("amount", amount)
  root.put("date", date.toString())
  root.put("type", type)
  if (desc.isNotEmpty()) {
    root.put("desc", desc)
  }
}

fun Transaction.toObjectNode(root: ObjectNode) {
  root.put("__type", this.javaClass.simpleName)
  val pathNode = root.putArray("path")
  treeNode.path.forEach { pathNode.add(it) }

  balance.toObjectNode(root.putObject("balance"))
  if (description != null) {
    root.put("description", description)
  }
  root.put("type", type.toString())
  if (balanceFromStart != null) {
    root.put("balanceFromStart", balanceFromStart)
  }
}

fun GqlTransaction.toObjectNode(root: ObjectNode) {
  root.put("__type", this.javaClass.simpleName)
  root.put("toAccountName", toAccountName)
  root.put("isIncome", isIncome)
  root.put("isExpense", isExpense)
  balance.toObjectNode(root.putObject("balance"))
  root.put("balanceFromStart", balanceFromStart)
  root.put("description", description)
}

fun TransactionStatement.toObjectNode(root: ObjectNode) {
  root.put("__type", this.javaClass.simpleName)
  (this as Statement).toObjectNode(root.putObject("__base"))
  root.put("coversPrevious", coversPrevious)
  root.put("coversProjectedPrevious", coversProjectedPrevious)
  root.put("hasProjectedTransfer", hasProjectedTransfer)
  root.put("isCovered", isCovered)
  root.put("isProjectedCovered", isProjectedCovered)
  root.put("isClosed", isClosed)
  if (transactions.isNotEmpty()) {
    val transactionsNode = root.putArray("transactions")
    transactions.forEach { it.toObjectNode(transactionsNode.addObject()) }
  }
}

fun GqlSummaryStatement.toObjectNode(root: ObjectNode) {
  root.put("__type", this.javaClass.simpleName)
  root.put("name", name)
  root.put("month", month.toString())
  if (accounts.isNotEmpty()) {
    val accountsNode = root.putArray("accounts")
    accounts.forEach { accountsNode.add(it) }
  }
  root.put("addSub", addSub)
  root.put("income", income)
  root.put("change", change)
  root.put("inFlows", inFlows)
  root.put("outFlows", outFlows)
  root.put("percentChange", percentChange)
  root.put("annualizedPercentChange", annualizedPercentChange)
  root.put("totalPayments", totalPayments)
  root.put("totalTransfers", totalTransfers)
  root.put("unaccounted", unaccounted)
  endBalance?.toObjectNode(root.putObject("endBalance"))
  startBalance?.toObjectNode(root.putObject("startBalance"))
}

fun GqlTable.toObjectNode(root: ObjectNode) {
  root.put("__type", this.javaClass.simpleName)
  root.put("currentOwner", currentOwner)
  if (owners.isNotEmpty()) {
    val ownersNode = root.putArray("owners")
    owners.forEach { ownersNode.add(it) }
  }
  if (months.isNotEmpty()) {
    val monthsNode = root.putArray("months")
    months.forEach { monthsNode.add(it.toString()) }
  }
  if (rows.isNotEmpty()) {
    val rowsNode = root.putArray("rows")
    rows.forEach { it.toObjectNode(rowsNode.addObject()) }
  }
}

fun GqlTableRow.toObjectNode(root: ObjectNode) {
  root.put("__type", this.javaClass.simpleName)
  root.put("title", title)
  account.toObjectNode(root.putObject("account"))
  root.put("indent", indent)
  root.put("isSpace", isSpace)
  root.put("isTotal", isTotal)
  root.put("isNormal", isNormal)
  if (cells.isNotEmpty()) {
    val cellsNode = root.putArray("cells")
    cells.forEach { it.toObjectNode(cellsNode.addObject()) }
  }
}

fun GqlAccount.toObjectNode(root: ObjectNode) {
  root.put("__type", this.javaClass.simpleName)
  root.put("name", name)
  if (description.isNotEmpty()) {
    root.put("description", description)
  }
  if (path.isNotEmpty()) {
    val pathNode = root.putArray("path")
    path.forEach { pathNode.add(it) }
  }
  root.put("external", external)
  root.put("summary", summary)
  if (number != null) {
    root.put("number", number)
  }
  if (openedOn != null) {
    root.put("openedOn", openedOn.toString())
  }
  if (closedOn != null) {
    root.put("closedOn", closedOn.toString())
  }
  if (owners.isNotEmpty()) {
    val ownersNode = root.putArray("owners")
    owners.forEach { ownersNode.add(it) }
  }
  if (url.isNotEmpty()) {
    root.put("url", url)
  }
  if (address.isNotEmpty()) {
    root.put("address", address)
  }
  if (userName.isNotEmpty()) {
    root.put("userName", userName)
  }
  if (password.isNotEmpty()) {
    root.put("password", password)
  }
  if (phone.isNotEmpty()) {
    root.put("phone", phone)
  }
}

fun GqlTableCell.toObjectNode(root: ObjectNode) {
  root.put("__type", this.javaClass.simpleName)
  root.put("month", month.toString())
  root.put("isClosed", isClosed)
  root.put("addSub", addSub)
  if (balance != null) {
    root.put("balance", balance)
  }
  root.put("isProjected", isProjected)
  root.put("isCovered", isCovered)
  root.put("isProjectedCovered", isProjectedCovered)
  root.put("hasProjectedTransfer", hasProjectedTransfer)
  root.put("percentChange", percentChange)
  root.put("annualizedPercentChange", annualizedPercentChange)
  if (unaccounted != null) {
    root.put("unaccounted", unaccounted)
  }
  root.put("balanced", balanced)
}

fun GqlSummaryData.toObjectNode(root: ObjectNode) {
  val statementsNode = root.putArray("statements")
  statements.forEach { it.toObjectNode(statementsNode.addObject()) }
  total.toObjectNode(root.putObject("total"))
}