package com.cherba29.tally.schema

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.MonthName.AUG
import com.cherba29.tally.statement.SummaryStatement
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate

class ToGqlTest : DescribeSpec({
  describe("SummaryStatement.toGqlSummaryData") {
    it("empty") {
      val statement = SummaryStatement(
        monthRange = AUG / 2026 .. AUG / 2026,
        startBalance = Balance.confirmed(100, "2026-08-01"),
      )
      statement.toGqlSummaryData("summary1") { treeNode, month -> false } shouldBe GqlSummaryData(
        statements = listOf(),
        total = GqlSummaryStatement(
          name = "summary1",
          month = AUG / 2026,
          addSub = 0,
          income = 0,
          change = 0,
          inFlows = 0,
          outFlows = 0,
          percentChange = 0f,
          annualizedPercentChange = 0f,
          totalPayments = 0,
          totalTransfers = 0,
          unaccounted = 0,
          endBalance = null,
          startBalance = GqlBalance(amount=100, date= LocalDate(2026, 8, 1), type="CONFIRMED", desc="")
        )
      )
    }
  }
})
