package com.cherba29.tally.data.builder

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.MonthName.APR
import com.cherba29.tally.core.MonthName.JAN
import com.cherba29.tally.core.MonthName.MAR
import com.cherba29.tally.core.MonthName.MAY
import com.cherba29.tally.core.NodeId
import com.cherba29.tally.statement.Statement
import com.cherba29.tally.statement.SummaryStatement
import com.cherba29.tally.statement.TransactionStatement
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate

class SummaryTest : DescribeSpec({
  describe("combineSummaryStatements") {
    it("empty") {
      val exception = shouldThrow<IllegalArgumentException> {
        combineSummaryStatements(summaryStatements = listOf())
      }
      exception.message shouldBe "Cant combine empty list of summary statements"
    }
    it("single") {
      val summaryStatement = SummaryStatement(
          nodeId = NodeId("test-account1", isSummary = true),
          monthRange = APR / 2026 .. MAY / 2026

      )
      val result = combineSummaryStatements(listOf(summaryStatement))
      result.nodeId shouldBe NodeId("test-account1", isSummary = true)
      result.monthRange shouldBe APR / 2026..MAY / 2026
      result.totalPayments shouldBe 0
      result.totalTransfers shouldBe 0
      result.statements shouldBe listOf()
    }
    it("two different node statements fail") {
      val stmt1 = SummaryStatement(
        nodeId = NodeId("test-account1", isSummary = true),
        monthRange = APR / 2026 .. MAY / 2026

      )
      val stmt2 = SummaryStatement(
        nodeId = NodeId("test-account2", isSummary = true),
        monthRange = APR / 2026 .. MAY / 2026

      )
      val exception = shouldThrow<IllegalArgumentException> {
        combineSummaryStatements(listOf(stmt1, stmt2))
      }
      exception.message shouldBe "Cant combine different summary statements test-account1 and test-account2"
    }

    it("two node statements with different months") {
      val stmt1 = SummaryStatement(
        nodeId = NodeId("test-account1", isSummary = true),
        monthRange = APR / 2026 .. APR / 2026

      )
      val stmt2 = SummaryStatement(
        nodeId = NodeId("test-account1", isSummary = true),
        monthRange = MAY / 2026 .. MAY / 2026

      )
      val result = combineSummaryStatements(listOf(stmt1, stmt2))
      result.nodeId shouldBe NodeId("test-account1", isSummary = true)
      result.monthRange shouldBe APR / 2026..MAY / 2026
      result.totalPayments shouldBe 0
      result.totalTransfers shouldBe 0
      result.statements shouldBe listOf()
    }
    it("two node statements with same months") {
      val stmt1 = SummaryStatement(
        nodeId = NodeId("test-account1", isSummary = true),
        monthRange = APR / 2026 .. APR / 2026

      )
      stmt1.addStatement(TransactionStatement(
        nodeId = NodeId("test-account1", isSummary = true),
        monthRange = APR / 2026 .. APR / 2026,
        isClosed = false,
        startBalance = Balance(100, LocalDate(2026, 4, 1), Balance.Type.CONFIRMED),
      ))
      val stmt2 = SummaryStatement(
        nodeId = NodeId("test-account1", isSummary = true),
        monthRange = APR / 2026 .. APR / 2026

      )
      stmt2.addStatement(TransactionStatement(
        nodeId = NodeId("test-account1", isSummary = true),
        monthRange = APR / 2026 .. APR / 2026,
        isClosed = false,
        startBalance = Balance(100, LocalDate(2026, 4, 1), Balance.Type.CONFIRMED),
      ))
      val exception = shouldThrow<IllegalArgumentException> {
        combineSummaryStatements(listOf(stmt1, stmt2))
      }
      exception.message shouldBe "Duplicate month statement for test-account1 for Apr2026..Apr2026"
    }
    it("two node statements with substatements") {
      val stmt1 = SummaryStatement(
        nodeId = NodeId("/test-account1", isSummary = true),
        monthRange = APR / 2026 .. APR / 2026
      )
      stmt1.addStatement(TransactionStatement(
          nodeId = NodeId("test-account1", isSummary = true),
          monthRange = APR / 2026 .. APR / 2026,
          isClosed = false,
          startBalance = Balance(100, LocalDate(2026, 4, 1), Balance.Type.CONFIRMED),
      ))
      val stmt2 = SummaryStatement(
        nodeId = NodeId("/test-account1", isSummary = true),
        monthRange = MAY / 2026 .. MAY / 2026
      )
      stmt2.addStatement(TransactionStatement(
        nodeId = NodeId("test-account1", isSummary = true),
        monthRange = MAY / 2026 .. MAY / 2026,
        isClosed = false,
        startBalance = Balance(200, LocalDate(2026, 5, 1), Balance.Type.CONFIRMED),
      ))
      val result = combineSummaryStatements(listOf(stmt1, stmt2))
      result.nodeId shouldBe NodeId("/test-account1", isSummary = true)
      result.monthRange shouldBe APR / 2026..MAY / 2026
      result.totalPayments shouldBe 0
      result.totalTransfers shouldBe 0
      result.income shouldBe 0
      result.change shouldBe -100
      result.statements.size shouldBe 1
      val statement = result.statements.first()
      statement.nodeId shouldBe NodeId("test-account1", isSummary = true)
      statement.monthRange shouldBe APR / 2026..MAY / 2026
      statement.totalPayments shouldBe 0
      statement.totalTransfers shouldBe 0
      statement.income shouldBe 0
      statement.change shouldBe -100
    }
  }

  describe("fromStatements") {
    it("empty") {
      val nodeId = NodeId(name = "test-account", isSummary = false, path = listOf("external"))
      val statement = Statement(
        nodeId,
        JAN / 2026 .. MAR / 2026,
      )
      statement.isClosed shouldBe false
      statement.percentChange shouldBe null
      statement.annualizedPercentChange shouldBe null
    }

    it("from empty list of statements") {
      val nodeId = NodeId(name = "test-account", isSummary = false, path = listOf("external"))
      val combined = makeSummaryStatementFromSubstatements(
        nodeId,
        JAN / 2026 .. MAR / 2026,
        statements = mapOf()
      )
      combined.isClosed shouldBe false
      combined.change shouldBe 0
      combined.percentChange shouldBe 0.0
      combined.annualizedPercentChange shouldBe 0.0
    }

    it("from single statement") {
      val nodeId = NodeId(name = "test-account", isSummary = false, path = listOf("external"))
      val startMonth = JAN / 2026
      val statement = TransactionStatement(
        nodeId,
        startMonth..startMonth,
        isClosed = false,
        startBalance = Balance(
          100,
          date = LocalDate(2026, 1, 1),
          type = Balance.Type.CONFIRMED
        )
      )
      val combined = makeSummaryStatementFromSubstatements(
        nodeId,
        startMonth .. MAR / 2026,
        statements = mapOf(startMonth to statement)
      )
      combined.isClosed shouldBe false
      combined.change shouldBe -100
      combined.percentChange shouldBe -100.0
      combined.annualizedPercentChange shouldBe null
    }
  }
})
