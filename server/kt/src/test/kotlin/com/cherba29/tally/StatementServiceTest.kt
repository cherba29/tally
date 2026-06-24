package com.cherba29.tally

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.MonthName.APR
import com.cherba29.tally.core.MonthName.MAR
import com.cherba29.tally.data.Loader
import com.cherba29.tally.data.builder.budget
import com.cherba29.tally.schema.GqlBalance
import com.cherba29.tally.schema.GqlStatement
import com.cherba29.tally.schema.GqlTransaction
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.datetime.LocalDate

class StatementServiceTest : DescribeSpec({
  describe("transaction statement response") {
    it("empty") {
      val account = Account("test-account1", owners = setOf("john"), path = listOf("internal"), isSummary = true, openedOn = MAR / 2026)

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
      exception.message shouldBe "Did not find account 'test-account' for owner 'john'"
    }

    it("no given month") {
      val account = Account("test-account1", owners = setOf("john"), path = listOf("internal"), isSummary = true, openedOn = MAR / 2026)

      val loader = mockk<Loader> {
        coEvery { budget() } returns budget {
          setAccount(listOf("john", "internal", "test-account1"), account)
        }
      }

      val exception = shouldThrow<NotFoundException> {
        StatementService(loader).statement(
          owner = "john",
          account = "test-account1",
          month = APR / 2026,
        )
      }
      exception.message shouldBe "Did not find statement for month 'Apr2026' for owner 'john' in account 'test-account1'"
    }

    it("single statement no transactions") {
      val account = Account(name = "test-account", isSummary = false, owners = setOf("john"), path = listOf("internal"), openedOn = MAR / 2026)
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
      val account1 = Account(name = "test-account1", isSummary = false, owners = setOf("john"), path = listOf("internal"), openedOn = MAR / 2026)
      val account2 = Account(name = "test-account2", isSummary = false, owners = setOf("john"), path = listOf("internal"), openedOn = MAR / 2026)

      val loader = mockk<Loader> {
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
