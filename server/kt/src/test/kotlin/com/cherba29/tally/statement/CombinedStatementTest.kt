package com.cherba29.tally.statement

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.AccountType
import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.BalanceType
import com.cherba29.tally.core.Month
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate

class CombinedStatementTest : DescribeSpec({
  describe("Build") {
    it("empty") {
      val account = Account(
        name = "test-account",
        type = AccountType.EXTERNAL,
      )
      val statement = CombinedStatement(
        account,
        month = Month(2026, Month.MAR),
        startMonth = Month(2026, Month.JAN),
      )
      statement.isClosed shouldBe false
      statement.percentChange shouldBe null
      statement.annualizedPercentChange shouldBe null
    }

    it("from empty list of statements") {
      val account = Account(
        name = "test-account",
        type = AccountType.EXTERNAL,
      )

      val combined = CombinedStatement.fromStatements(
        account,
        startMonth = Month(2026, Month.JAN),
        endMonth = Month(2026, Month.MAR),
        statements = mapOf()
      )
      combined.isClosed shouldBe false
      combined.change shouldBe 0
      combined.percentChange shouldBe 0.0
      combined.annualizedPercentChange shouldBe 0.0
    }

    it("from single statement") {
      val account = Account(
        name = "test-account",
        type = AccountType.EXTERNAL,
      )
      val startMonth = Month(2026, Month.JAN)
      val statement = TransactionStatement(
        account = account,
        month = startMonth,
        startBalance = Balance(
          100,
          date = LocalDate(2026, 1, 1),
          type = BalanceType.CONFIRMED
        )
      )
      val combined = CombinedStatement.fromStatements(
        account,
        startMonth,
        endMonth = Month(2026, Month.MAR),
        statements = mapOf(startMonth to statement)
      )
      combined.isClosed shouldBe false
      combined.change shouldBe -100
      combined.percentChange shouldBe -100.0
      combined.annualizedPercentChange shouldBe null
    }
  }
})