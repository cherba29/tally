package com.cherba29.tally.data.yaml

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Balance
import com.cherba29.tally.statement.Statement
import com.cherba29.tally.statement.Transaction
import com.cherba29.tally.statement.TransactionStatement
import com.fasterxml.jackson.databind.node.ObjectNode

fun Account.toObjectNode(root: ObjectNode) {
  root.put("__type", this.javaClass.simpleName)
  root.put("name", name)
  if (description != null) {
    root.put("desc", description)
  }
  if (path.isNotEmpty()) {
    val pathNode = root.putArray("path")
    path.forEach { pathNode.add(it) }
  }
  root.put("type", type.toString())
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
    val ownersNode = root.putArray("owner")
    owners.forEach { ownersNode.add(it) }
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
  account.toObjectNode(root.putObject("account"))
  root.put("month", month.toString())
  startBalance?.toObjectNode(root.putObject("startBalance"))
  endBalance?.toObjectNode(root.putObject("endBalance"))
  if (inFlows != 0) {
    root.put("inFlows", inFlows)
  }
  if (outFlows != 0) {
    root.put("outFlows", outFlows)
  }
  if (totalTransfers != 0) {
    root.put("totalTransfers", totalTransfers)
  }
  if (totalPayments != 0) {
    root.put("totalPayments", totalPayments)
  }
  if (income != 0) {
    root.put("income", income)
  }
}

fun Balance.toObjectNode(root: ObjectNode) {
  root.put("__type", this.javaClass.simpleName)
  root.put("amount", amount)
  root.put("date", date.toString())
  root.put("type", type.toString())
}

fun Transaction.toObjectNode(root: ObjectNode) {
  root.put("__type", this.javaClass.simpleName)
  account.toObjectNode(root.putObject("account"))
  balance.toObjectNode(root.putObject("balance"))
  if (description != null) {
    root.put("description", description)
  }
  root.put("type", type.toString())
  if (balanceFromStart != null) {
    root.put("balanceFromStart", balanceFromStart)
  }
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
