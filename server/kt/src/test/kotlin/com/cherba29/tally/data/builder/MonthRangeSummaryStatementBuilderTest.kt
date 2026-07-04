package com.cherba29.tally.data.builder

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.MonthName.APR
import com.cherba29.tally.core.MonthName.JAN
import com.cherba29.tally.core.MonthName.MAR
import com.cherba29.tally.core.MonthName.MAY
import com.cherba29.tally.core.root
import com.cherba29.tally.statement.Statement
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
        builder.build(root {})
      }
      exception.message shouldBe "Cant combine empty list of summary statements"
    }
    it("single") {
      val tree = root {
        branch("john") {
          branch("external") {
            leaf("test-account1")
          }
        }
      }
      val testStatement = Statement(tree[listOf("john", "external", "test-account1")]!!, APR / 2026..APR / 2026)
      val summaryStatement = SummaryStatement(
        tree[listOf("john", "external")]!!,
        monthRange = APR / 2026..MAY / 2026,
        statements = listOf(testStatement)
      )
      val builder = MonthRangeSummaryStatementBuilder()
      builder.addStatement(APR / 2026, summaryStatement)
      val result = builder.build(tree["john"]!!)
      result.treeNode.path shouldBe listOf("john")
      result.monthRange shouldBe APR / 2026..APR / 2026
      result.totalPayments shouldBe 0
      result.totalTransfers shouldBe 0
      result.statements shouldBe listOf(testStatement)
    }
    it("two node statements with different months") {
      val tree = root {
        branch("john") {
          branch("internal") {
            leaf("test-account1")
          }
          branch("external") {
            leaf("test-account2")
          }
        }
      }
      val testStatement1 = Statement(tree[listOf("john", "internal", "test-account1")]!!, APR / 2026..APR / 2026)
      val stmt1 = SummaryStatement(
        tree[listOf("john", "internal")]!!,
        monthRange = APR / 2026..APR / 2026,
        statements = listOf(testStatement1)
      )
      val testStatement2 = Statement(tree[listOf("john", "external", "test-account2")]!!, MAY / 2026..MAY / 2026)
      val stmt2 = SummaryStatement(
        tree[listOf("john", "external")]!!,
        monthRange = MAY / 2026..MAY / 2026,
        statements = listOf(testStatement2)

      )
      val builder = MonthRangeSummaryStatementBuilder()
      builder.addStatement(APR / 2026, stmt1)
      builder.addStatement(MAY / 2026, stmt2)
      val result = builder.build(tree["john"]!!)
      result.treeNode.path shouldBe listOf("john")
      result.monthRange shouldBe APR / 2026..MAY / 2026
      result.totalPayments shouldBe 0
      result.totalTransfers shouldBe 0
      result.statements.size shouldBe 2
      val firstStmt = result.statements[0]
      firstStmt.treeNode.path shouldBe listOf("john", "internal", "test-account1")
      val secondStmt = result.statements[1]
      secondStmt.treeNode.path shouldBe listOf("john", "external", "test-account2")
    }
    it("two node statements with substatements") {
      val tree = root {
        branch("john") {
          branch("internal") {
            leaf("test-account1")
          }
        }
      }

      val startBalance1 = Balance(100, LocalDate(2026, 4, 1), Balance.Type.CONFIRMED)
      val stmt1 = MonthSummaryStatementBuilder.builder {
        treeNode = tree[listOf("john", "internal")]
        addStatement(
          TransactionStatement(
            tree[listOf("john", "internal", "test-account1")]!!,
            monthRange = APR / 2026..APR / 2026,
            isClosed = false,
            startBalance = startBalance1,
          )
        )
      }
      val startBalance2 = Balance(200, LocalDate(2026, 5, 1), Balance.Type.CONFIRMED)
      val stmt2 = MonthSummaryStatementBuilder.builder {
        treeNode = tree[listOf("john", "internal")]
        addStatement(
          TransactionStatement(
            tree[listOf("john", "internal", "test-account1")]!!,
            monthRange = MAY / 2026..MAY / 2026,
            isClosed = false,
            startBalance = startBalance2,
          )
        )
      }
      val builder = MonthRangeSummaryStatementBuilder()
      builder.addStatement(APR / 2026, stmt1)
      builder.addStatement(MAY / 2026, stmt2)
      val result = builder.build(tree["john"]!!)
      result.treeNode.path shouldBe listOf("john")
      result.monthRange shouldBe APR / 2026..MAY / 2026
      result.totalPayments shouldBe 0
      result.totalTransfers shouldBe 0
      result.income shouldBe 0
      result.startBalance shouldBe startBalance1
      result.endBalance shouldBe Balance(0, LocalDate(2026, 5, 1), Balance.Type.PROJECTED)
      result.change shouldBe -100
      result.statements.size shouldBe 1
      val statement = result.statements.first()
      statement.treeNode.path shouldBe listOf("john", "internal", "test-account1")
      statement.monthRange shouldBe APR / 2026..MAY / 2026
      statement.totalPayments shouldBe 0
      statement.totalTransfers shouldBe 0
      statement.income shouldBe 0
      statement.change shouldBe -100
    }
  }

  describe("fromStatements") {
    it("empty") {
      val tree = root {
        branch("john") {
          branch("internal") {
            leaf("test-account1")
          }
        }
      }

      val statement = Statement(
        tree[listOf("john", "internal", "test-account1")]!!,
        JAN / 2026..MAR / 2026,
      )
      statement.isClosed shouldBe false
      statement.percentChange shouldBe null
      statement.annualizedPercentChange shouldBe null
    }

    it("from empty list of statements") {
      val tree = root {
        branch("john") {
          branch("internal") {
            leaf("test-account1")
          }
        }
      }

      val combined = MonthRangeSummaryStatementBuilder.makeSummaryStatementFromSubstatements(
        tree[listOf("john", "internal", "test-account1")]!!,
        JAN / 2026..MAR / 2026,
        statements = mapOf()
      )
      combined.isClosed shouldBe false
      combined.change shouldBe 0
      combined.percentChange shouldBe 0.0
      combined.annualizedPercentChange shouldBe 0.0
    }

    it("from single statement") {
      val tree = root {
        branch("john") {
          branch("internal") {
            leaf("test-account1")
          }
        }
      }

      val startMonth = JAN / 2026
      val statement = TransactionStatement(
        tree[listOf("john", "internal", "test-account1")]!!,
        startMonth..startMonth,
        isClosed = false,
        startBalance = Balance(
          100,
          date = LocalDate(2026, 1, 1),
          type = Balance.Type.CONFIRMED
        )
      )
      val combined = MonthRangeSummaryStatementBuilder.makeSummaryStatementFromSubstatements(
        tree[listOf("john", "internal", "test-account1")]!!,
        startMonth..MAR / 2026,
        statements = mapOf(startMonth to statement)
      )
      combined.isClosed shouldBe false
      combined.change shouldBe -100
      combined.percentChange shouldBe -100.0
      combined.annualizedPercentChange shouldBe null
    }
  }
})
