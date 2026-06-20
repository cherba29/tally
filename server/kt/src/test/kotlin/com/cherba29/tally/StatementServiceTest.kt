package com.cherba29.tally

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.MonthName.APR
import com.cherba29.tally.core.MonthName.MAR
import com.cherba29.tally.core.NodeId
import com.cherba29.tally.data.Loader
import com.cherba29.tally.data.builder.budget
import com.cherba29.tally.schema.GqlBalance
import com.cherba29.tally.schema.GqlStatement
import com.cherba29.tally.schema.GqlTransaction
import com.cherba29.tally.statement.Transaction
import com.cherba29.tally.statement.TransactionStatement
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.datetime.LocalDate

class StatementServiceTest : DescribeSpec({
  describe("transaction statement response") {
    it("empty") {
      val nodeId = NodeId("test-account1", owners = setOf("john"), path = listOf("internal"), isSummary = true)
      val account = Account(nodeId, openedOn = MAR / 2026)

      val loader = mockk<Loader> {
        coEvery { budget() } returns budget {
          setAccount(listOf("john", "internal", "test-account1"), account)
        }
      }

      val exception = shouldThrow<NotFoundException> {
        StatementService(loader).statement(
          owner = "john",
          account = "test-account",
          month = MAR / 2026,
        )
      }
      exception.message shouldBe "Did not find statement for john test-account Mar2026"
    }

    it("single statement no transactions") {
      val nodeId = NodeId(
        name = "test-account", isSummary = false, owners = setOf("john"), path = listOf("internal")
      )
      val account = Account(nodeId = nodeId, openedOn = MAR / 2026)
      val loader = mockk<Loader> {
        coEvery { budget() } returns budget {
          setAccount(listOf("john", "internal", "test-account"), account)
        }
      }

      val result = StatementService(loader).statement(
        owner = "john",
        account = "test-account",
        month = MAR / 2026,
      )
      result shouldBe GqlStatement(
        name = "test-account",
        month = MAR / 2026,
        isClosed = false,
        isCovered = true,
        isProjectedCovered = true,
        hasProjectedTransfer = false,
        startBalance = null,
        endBalance = null,
        inFlows = 0,
        outFlows = 0,
        income = 0,
        totalPayments = 0,
        totalTransfers = 0,
        change = 0,
        addSub = 0,
        percentChange = 0.0f,
        annualizedPercentChange = 0.0f,
        unaccounted = 0,
        transactions = listOf()
      )
    }

    it("statement with transactions") {
      val nodeId1 = NodeId(
        name = "test-account1", isSummary = false, owners = setOf("john"), path = listOf("internal")
      )
      val account1 = Account(nodeId = nodeId1, openedOn = MAR / 2026)
      val nodeId2 = NodeId(
        name = "test-account2", isSummary = false, owners = setOf("john"), path = listOf("internal")
      )
      val account2 = Account(nodeId = nodeId2, openedOn = MAR / 2026)

      val transactionStatement11 = TransactionStatement(
        nodeId = nodeId1, monthRange = MAR / 2026..MAR / 2026, isClosed = false, startBalance = Balance(
          amount = 100, date = LocalDate(2026, 3, 1), type = Balance.Type.CONFIRMED, description = "start balance"
        )
      )
      transactionStatement11.transactions.add(
        Transaction(
          nodeId = nodeId1, balance = Balance(
            amount = 200, date = LocalDate(2026, 3, 2), type = Balance.Type.CONFIRMED, description = "transfer1"
          ), description = "transfer1", type = Transaction.Type.TRANSFER, balanceFromStart = -100
        )
      )
      val transactionStatement12 = TransactionStatement(
        nodeId = nodeId1, monthRange = APR / 2026..APR / 2026, isClosed = false, startBalance = null
      )
      val transactionStatement21 = TransactionStatement(
        nodeId = nodeId2, monthRange = MAR / 2026..MAR / 2026, isClosed = false, startBalance = null
      )
      val loader = mockk<Loader> {
//        coEvery { budget() } returns Budget(
//          months = MAR / 2026..MAR / 2026,
//          tree = root {},
//          leafToAccount = mapOf(),
//          accounts = mapOf(nodeId1 to account1, nodeId2 to account2),
//          nodeToStatement = mapOf(),
//          statements = mapOf(
//            nodeId1 to mapOf(MAR / 2026 to transactionStatement11, APR / 2026 to transactionStatement12),
//            nodeId2 to mapOf(MAR / 2026 to transactionStatement21)
//          )
//        )
        coEvery { budget() } returns budget {
          setAccount(listOf("john", "internal", "test-account1"), account1)
          setAccount(listOf("john", "internal", "test-account2"), account2)
          setBalance(
            listOf("john", "internal", "test-account1"), MAR / 2026,
            Balance(
              amount = 100, date = LocalDate(2026, 3, 1), type = Balance.Type.CONFIRMED, description = "start balance"
            )
          )
          addTransfer(
            listOf("john", "internal", "test-account2"),
            MAR / 2026,
            "test-account1",
            MAR / 2026,
            Balance(
              amount = 200,
              date = LocalDate(2026, 3, 2),
              type = Balance.Type.CONFIRMED,
              description = "transfer1"
            ),
            "transfer1"
          )
        }
      }

      val result = StatementService(loader).statement(
        owner = "john",
        account = "test-account1",
        month = MAR / 2026,
      )
      result shouldBe GqlStatement(
        name = "test-account1",
        month = MAR / 2026,
        isClosed = false,
        isCovered = true,
        isProjectedCovered = true,
        hasProjectedTransfer = false,
        startBalance = GqlBalance(
          amount = 100, date = LocalDate(2026, 3, 1), type = "CONFIRMED", desc = "start balance"
        ),
        endBalance = null,
        inFlows = 200,
        outFlows = 0,
        income = 0,
        totalPayments = 0,
        totalTransfers = 200,
        change = 0,
        addSub = 200,
        percentChange = 0.0f,
        annualizedPercentChange = 0.0f,
        unaccounted = 0,
        transactions = listOf(
          GqlTransaction(
            toAccountName = "test-account2",
            isIncome = false,
            isExpense = false,
            balance = GqlBalance(
              amount = 200,
              date = LocalDate(2026, 3, 2),
              type = "CONFIRMED",
              desc = "transfer1"
            ),
            balanceFromStart = 300,
            description = "transfer1"
          )
        )
      )
    }
  }
})
