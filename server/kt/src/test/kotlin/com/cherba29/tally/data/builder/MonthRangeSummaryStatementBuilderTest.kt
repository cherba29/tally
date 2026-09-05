package com.cherba29.tally.data.builder

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.MonthName.APR
import com.cherba29.tally.core.MonthName.JAN
import com.cherba29.tally.core.MonthName.MAR
import com.cherba29.tally.core.MonthName.MAY
import com.cherba29.tally.core.root
import com.cherba29.tally.data.Profile
import com.cherba29.tally.statement.SummaryStatement
import com.cherba29.tally.statement.TransactionStatement
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate

class MonthRangeSummaryStatementBuilderTest : DescribeSpec({
  describe("combineSummaryStatements") {
    it("empty") {
      val builder = MonthRangeSummaryStatementBuilder()
      val exception = shouldThrow<IllegalArgumentException> {
        builder.build()
      }
      exception.message shouldBe "summary build failed: no statements have been added"
    }
    it("single") {
      val tree = root(Profile()) {
        branch("john", Profile()) {
          branch("external", Profile(isExternal = true)) {
            leaf("test-account1", Profile(isExternal = true))
          }
        }
      }
      val testStatement = TransactionStatement(
        APR / 2026..APR / 2026,
        startBalance = null
      )
      val builder = MonthRangeSummaryStatementBuilder()
      val accountNode = tree[listOf("john", "external", "test-account1")]!!
      builder.addStatement(accountNode, APR / 2026, testStatement)
      val result = builder.build()
      result.monthRange shouldBe APR / 2026..APR / 2026
      result.totalPayments shouldBe 0
      result.totalTransfers shouldBe 0
      result.statements.keys shouldBe setOf(accountNode)
      result.statements[accountNode] shouldBe SummaryStatement(
        APR / 2026..APR / 2026,
        startBalance = Balance(0, LocalDate(2026, 4, 1), Balance.Type.PROJECTED),
        endBalance = Balance(0, LocalDate(2026, 5, 1), Balance.Type.PROJECTED)
      )
    }
    it("two node statements with different months") {
      val tree = root(Profile()) {
        branch("john", Profile()) {
          branch("internal", Profile()) {
            leaf("test-account1", Profile())
          }
          branch("external", Profile(isExternal = true)) {
            leaf("test-account2", Profile(isExternal = true))
          }
        }
      }
      val testStatement1 = TransactionStatement(
        APR / 2026..APR / 2026,
        startBalance = null
      )
      val testStatement2 = TransactionStatement(
        MAY / 2026..MAY / 2026,
        startBalance = null
      )
      val node1 = tree[listOf("john", "internal", "test-account1")]!!
      val node2 = tree[listOf("john", "external", "test-account2")]!!
      val builder = MonthRangeSummaryStatementBuilder()
      builder.addStatement(node1, APR / 2026, testStatement1)
      builder.addStatement(node2, MAY / 2026, testStatement2)
      val result = builder.build()
      result.monthRange shouldBe APR / 2026..MAY / 2026
      result.totalPayments shouldBe 0
      result.totalTransfers shouldBe 0
      result.statements.keys shouldBe setOf(node1, node2)
      val firstStmt = result.statements[node1] as SummaryStatement
      firstStmt shouldBe SummaryStatement(
        APR / 2026..MAY / 2026,
        startBalance = Balance(0, LocalDate(2026, 4, 1), Balance.Type.PROJECTED),
        endBalance = Balance(0, LocalDate(2026, 6, 1), Balance.Type.PROJECTED)
      )
      val secondStmt = result.statements[node2] as SummaryStatement
      secondStmt shouldBe SummaryStatement(
        APR / 2026..MAY / 2026,
        startBalance = Balance(0, LocalDate(2026, 4, 1), Balance.Type.PROJECTED),
        endBalance = Balance(0, LocalDate(2026, 6, 1), Balance.Type.PROJECTED)
      )
    }
    it("two node statements with substatements") {
      val tree = root(Profile()) {
        branch("john", Profile()) {
          branch("internal", Profile()) {
            leaf("test-account1", Profile())
          }
        }
      }
      val node1 = tree[listOf("john", "internal", "test-account1")]!!
      val node2 = tree[listOf("john", "internal", "test-account1")]!!
      val startBalance1 = Balance(100, LocalDate(2026, 4, 1), Balance.Type.CONFIRMED)
      val stmt1 = TransactionStatement(
        monthRange = APR / 2026..APR / 2026,
        startBalance = startBalance1
      )
      val startBalance2 = Balance(200, LocalDate(2026, 5, 1), Balance.Type.CONFIRMED)
      val stmt2 = TransactionStatement(
        monthRange = MAY / 2026..MAY / 2026,
        startBalance = startBalance2
      )

      val builder = MonthRangeSummaryStatementBuilder()
      builder.addStatement(node1, APR / 2026, stmt1)
      builder.addStatement(node2, MAY / 2026, stmt2)
      val result = builder.build()
      result.monthRange shouldBe APR / 2026..MAY / 2026
      result.totalPayments shouldBe 0
      result.totalTransfers shouldBe 0
      result.income shouldBe 0
      result.startBalance shouldBe startBalance1
      result.endBalance shouldBe Balance(0, LocalDate(2026, 6, 1), Balance.Type.PROJECTED)
      result.change shouldBe -100
      result.statements.keys shouldBe setOf(node1)
      val statement = result.statements[node1] as SummaryStatement
      statement.monthRange shouldBe APR / 2026..MAY / 2026
      statement.totalPayments shouldBe 0
      statement.totalTransfers shouldBe 0
      statement.income shouldBe 0
      statement.change shouldBe -100
    }
  }

  describe("fromStatements") {
    it("from empty list of statements") {
      val combined = MonthRangeSummaryStatementBuilder.makeSummaryStatementFromSubstatements(
        JAN / 2026..MAR / 2026,
        statements = mapOf()
      )
      combined.change shouldBe 0
      combined.percentChange shouldBe 0.0
      combined.annualizedPercentChange shouldBe 0.0
    }

    it("from single statement") {
      val startMonth = JAN / 2026
      val statement = TransactionStatement(
        startMonth..startMonth,
        startBalance = Balance(
          100,
          date = LocalDate(2026, 1, 1),
          type = Balance.Type.CONFIRMED
        )
      )
      val combined = MonthRangeSummaryStatementBuilder.makeSummaryStatementFromSubstatements(
        startMonth..MAR / 2026,
        statements = mapOf(startMonth to statement)
      )
      combined.change shouldBe -100
      combined.percentChange shouldBe -100.0
      combined.annualizedPercentChange shouldBe null
    }
  }
})
