package com.cherba29.tally.statement

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.MonthName.JAN
import com.cherba29.tally.core.MonthName.MAR
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate

class CombinedStatementTest : DescribeSpec({
  describe("Build") {
    it("empty") {
      val account = Account(
        name = "test-account",
        type = Account.Type.EXTERNAL,
      )
      val statement = CombinedStatement(
        account,
        month = MAR / 2026,
        startMonth = JAN / 2026,
      )
      statement.isClosed shouldBe false
      statement.percentChange shouldBe null
      statement.annualizedPercentChange shouldBe null
    }

    it("from empty list of statements") {
      val account = Account(
        name = "test-account",
        type = Account.Type.EXTERNAL,
      )

      val combined = CombinedStatement.fromStatements(
        account,
        startMonth = JAN / 2026,
        endMonth = MAR / 2026,
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
        type = Account.Type.EXTERNAL,
      )
      val startMonth = JAN / 2026
      val statement = TransactionStatement(
        account = account,
        month = startMonth,
        startBalance = Balance(
          100,
          date = LocalDate(2026, 1, 1),
          type = Balance.Type.CONFIRMED
        )
      )
      val combined = CombinedStatement.fromStatements(
        account,
        startMonth,
        endMonth = MAR / 2026,
        statements = mapOf(startMonth to statement)
      )
      combined.isClosed shouldBe false
      combined.change shouldBe -100
      combined.percentChange shouldBe -100.0
      combined.annualizedPercentChange shouldBe null
    }
  }
})