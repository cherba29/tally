package com.cherba29.tally.data.builder

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.MonthName.JUL
import com.cherba29.tally.core.root
import com.cherba29.tally.statement.Statement
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate

class MonthSummaryStatementBuilderTest : DescribeSpec({
  describe("basic") {
    it("error when no treeNode set") {
      val error = shouldThrow<IllegalArgumentException> {
        MonthSummaryStatementBuilder.builder {}
      }
      error.message shouldBe "summary build failed: treeNode is not set"
    }

    it("error when no month range is set") {
      val error = shouldThrow<IllegalArgumentException> {
        MonthSummaryStatementBuilder.builder {
          treeNode = root { }
        }
      }
      error.message shouldBe "summary build failed: month range is not set"
    }

    it("with single zero statements") {
      val testTree = root { }
      val testMonthRange = JUL / 2026 .. JUL / 2026
      val testStatement = Statement(testTree, testMonthRange)
      val summary = MonthSummaryStatementBuilder.builder {
        treeNode = testTree
        addStatement(testStatement)
      }

      summary.treeNode shouldBe testTree
      summary.monthRange shouldBe testMonthRange
      summary.statements shouldBe listOf(testStatement)
      summary.startBalance shouldBe null
      summary.endBalance shouldBe null
      summary.inFlows shouldBe 0
      summary.outFlows shouldBe 0
      summary.totalTransfers shouldBe 0
      summary.totalPayments shouldBe 0
      summary.income shouldBe 0
    }

    it("with single non-zero statements") {
      val testTree = root { }
      val testMonthRange = JUL / 2026 .. JUL / 2026
      val testStartBalance = Balance(100, LocalDate(2026, 7, 4), Balance.Type.CONFIRMED)
      val testEndBalance = Balance(200, LocalDate(2026, 8, 1), Balance.Type.PROJECTED)
      val testStatement = Statement(
        testTree,
        testMonthRange,
        startBalance = testStartBalance,
        endBalance = testEndBalance,
        inFlows = 10,
        outFlows = 20,
        totalTransfers = 15,
        totalPayments = 25,
        income = 30
      )
      val summary = MonthSummaryStatementBuilder.builder {
        treeNode = testTree
        addStatement(testStatement)
      }

      summary.treeNode shouldBe testTree
      summary.monthRange shouldBe testMonthRange
      summary.statements shouldBe listOf(testStatement)
      summary.startBalance shouldBe testStartBalance
      summary.endBalance shouldBe testEndBalance
      summary.inFlows shouldBe 10
      summary.outFlows shouldBe 20
      summary.totalTransfers shouldBe 15
      summary.totalPayments shouldBe 25
      summary.income shouldBe 30
    }
  }
})