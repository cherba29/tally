package com.cherba29.tally.statement

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.MonthName.APR
import com.cherba29.tally.core.MonthName.MAR
import com.cherba29.tally.core.MonthName.MAY
import com.cherba29.tally.core.NodeId
import com.cherba29.tally.utils.Map3
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate

class SummaryTest : DescribeSpec({
  describe("buildSummaryStatementTable") {
    it("empty") {
      val statements = buildSummaryStatementTable(listOf(), selectedOwner = null).toList()
      statements shouldBe listOf()
    }

    it("single closed account - produces summary without it") {
      val node1 = NodeId(
        name = "test-account1",
        path = listOf("external"),
        owners = setOf("john"),
      )
      val startBalance = Balance(
        100,
        LocalDate(2023, 12, 2),
        Balance.Type.CONFIRMED
      )
      val tranStmt = TransactionStatement(
        node1,
        MAR / 2021 .. MAR / 2021,
        false,
        startBalance
      )
      val statements: Map3<SummaryStatement> = buildSummaryStatementTable(listOf(tranStmt), selectedOwner = null)
      withClue("should contain: $statements") {
        statements.isEmpty shouldBe false
        statements.size shouldBe 2
        statements.keys.keys shouldBe setOf("john")
        statements.keys["john"] shouldBe setOf("/external", "/")
      }
      val stmt1 = statements["john", "/external", "Mar2021"]
      stmt1?.nodeId shouldBe NodeId(
        name = "/external",
        path = listOf(),
        owners = setOf("john")
      )
      withClue("statement: $stmt1") {
        stmt1?.startBalance shouldBe startBalance
        stmt1?.endBalance shouldBe null
        stmt1?.inFlows shouldBe 0
        stmt1?.income shouldBe 0
        stmt1?.monthRange shouldBe MAR / 2021 .. MAR / 2021
        stmt1?.outFlows shouldBe 0
        stmt1?.statements shouldBe listOf(tranStmt)
        stmt1?.totalPayments shouldBe 0
        stmt1?.totalTransfers shouldBe 0
      }

      val stmt2 = statements["john", "/", "Mar2021"]
      stmt2?.nodeId shouldBe NodeId(
        name = "/",
        path = listOf(),
        owners = setOf("john")
      )
      stmt2?.startBalance shouldBe startBalance
      stmt2?.endBalance shouldBe null
      stmt2?.inFlows shouldBe 0
      stmt2?.income shouldBe 0
      stmt2?.monthRange shouldBe MAR / 2021 .. MAR / 2021
      stmt2?.outFlows shouldBe 0
      stmt2?.statements shouldBe listOf(stmt1)
      stmt2?.totalPayments shouldBe 0
      stmt2?.totalTransfers shouldBe 0
    }

    it("single external account - no SUMMARY") {
      val node1 = NodeId(
        name = "test-account1",
        path = listOf("external"),
        owners = setOf("john")
      )

      val tranStmt = TransactionStatement(
        node1,
        MAR / 2021 .. MAR / 2021,
        false,
        startBalance = null
      )
      tranStmt.startBalance = Balance(100, LocalDate(2023, 12, 2), Balance.Type.CONFIRMED)
      val statements: Map3<SummaryStatement> = buildSummaryStatementTable(listOf(tranStmt), selectedOwner = null)
      statements.isEmpty shouldBe false
      statements.size shouldBe 2
      statements.keys.keys shouldBe setOf("john")
      statements.keys["john"] shouldBe setOf("/external", "/")

      val stmt = statements["john", "/", "Mar2021"]!!
      stmt.nodeId shouldBe NodeId(
        name = "/",
        path = listOf(),
        owners = setOf("john"),
      )
      stmt.startBalance shouldBe Balance(100, LocalDate(2023, 12, 2), Balance.Type.CONFIRMED)
      stmt.endBalance shouldBe null
      stmt.inFlows shouldBe 0
      stmt.income shouldBe 0
      stmt.monthRange shouldBe MAR / 2021 .. MAR / 2021
      stmt.outFlows shouldBe 0
      stmt.statements shouldBe listOf(statements["john", "/external", "Mar2021"]!!)
      stmt.totalPayments shouldBe 0
      stmt.totalTransfers shouldBe 0
    }

    it("single account - no transfers") {
      val node1 = NodeId(
        name = "test-account1",
        path = listOf("external"),
        owners = setOf("john")
      )

      val tranStmt = TransactionStatement(
        node1,
        MAR / 2021 .. MAR / 2021,
        false,
        startBalance = null
      )
      tranStmt.startBalance = Balance(
        100,
        LocalDate(2023, 12, 2),
        Balance.Type.CONFIRMED
      )
      val statements: Map3<SummaryStatement> = buildSummaryStatementTable(listOf(tranStmt), selectedOwner = "john")
      statements.isEmpty shouldBe false
      statements.size shouldBe 2
      statements.keys.keys shouldBe setOf("john")
      statements.keys["john"] shouldBe setOf("/external", "/")

      val stmt1 = statements["john", "/external", "Mar2021"]!!
      stmt1.nodeId shouldBe NodeId(
        name = "/external",
        path = listOf(),
        owners = setOf("john")
      )
      stmt1.startBalance shouldBe Balance(100, LocalDate(2023, 12, 2), Balance.Type.CONFIRMED)
      stmt1.endBalance shouldBe null
      stmt1.inFlows shouldBe 0
      stmt1.income shouldBe 0
      stmt1.monthRange shouldBe MAR / 2021 .. MAR / 2021
      stmt1.outFlows shouldBe 0
      stmt1.statements shouldBe listOf(tranStmt)
      stmt1.totalPayments shouldBe 0
      stmt1.totalTransfers shouldBe 0

      val stmt2 = statements["john", "/", "Mar2021"]!!
      stmt2.nodeId shouldBe NodeId(
        name = "/",
        path = listOf(),
        owners = setOf("john"),
      )
      stmt2.startBalance shouldBe Balance(100, LocalDate(2023, 12, 2), Balance.Type.CONFIRMED)
      stmt2.endBalance shouldBe null
      stmt2.inFlows shouldBe 0
      stmt2.income shouldBe 0
      stmt2.monthRange shouldBe MAR / 2021 .. MAR / 2021
      stmt2.outFlows shouldBe 0
      stmt2.statements shouldBe listOf(stmt1)
      stmt2.totalPayments shouldBe 0
      stmt2.totalTransfers shouldBe 0
    }

    it("multiple accounts - selected owner") {
      val node1 = NodeId(
        name = "test-account1",
        path = listOf("external"),
        owners = setOf("john")
      )

      val tranStmt1 = TransactionStatement(
        node1,
        MAR / 2021 .. MAR / 2021,
        false,
        startBalance = null
      )
      tranStmt1.startBalance = Balance(
        100,
        LocalDate(2023, 12, 2),
        Balance.Type.CONFIRMED
      )
      // Should skip since different owner.
      val node2 = NodeId(
        name = "test-account1",
        path = listOf("external"),
        owners = setOf("bob")
      )

      val tranStmt2 = TransactionStatement(
        node2,
        MAR / 2021 .. MAR / 2021,
        false,
        startBalance = null
      )
      tranStmt2.startBalance = Balance(
        100,
        LocalDate(2023, 12, 2),
        Balance.Type.CONFIRMED
      )
      // Should skip since path is empty.
      val node3 = NodeId(
        name = "test-account1",
        path = listOf(),
        owners = setOf("john")
      )

      val tranStmt3 = TransactionStatement(
        node3,
        MAR / 2021 .. MAR / 2021,
        false,
        startBalance = null
      )
      tranStmt3.startBalance = Balance(
        100,
        LocalDate(2023, 12, 2),
        Balance.Type.CONFIRMED
      )

      val statements: Map3<SummaryStatement> = buildSummaryStatementTable(
        listOf(tranStmt1, tranStmt2, tranStmt3), selectedOwner = "john")
      statements.isEmpty shouldBe false
      statements.size shouldBe 2
      statements.keys.keys shouldBe setOf("john")
      statements.keys["john"] shouldBe setOf("/external", "/")

      val stmt1 = statements["john", "/external", "Mar2021"]!!
      stmt1.nodeId shouldBe NodeId(
        name = "/external",
        path = listOf(),
        owners = setOf("john")
      )
      stmt1.startBalance shouldBe Balance(100, LocalDate(2023, 12, 2), Balance.Type.CONFIRMED)
      stmt1.endBalance shouldBe null
      stmt1.inFlows shouldBe 0
      stmt1.income shouldBe 0
      stmt1.monthRange shouldBe MAR / 2021 .. MAR / 2021
      stmt1.outFlows shouldBe 0
      stmt1.statements shouldBe listOf(tranStmt1)
      stmt1.totalPayments shouldBe 0
      stmt1.totalTransfers shouldBe 0

      val stmt2 = statements["john", "/", "Mar2021"]!!
      stmt2.nodeId shouldBe NodeId(
        name = "/",
        path = listOf(),
        owners = setOf("john"),
      )
      stmt2.startBalance shouldBe Balance(100, LocalDate(2023, 12, 2), Balance.Type.CONFIRMED)
      stmt2.endBalance shouldBe null
      stmt2.inFlows shouldBe 0
      stmt2.income shouldBe 0
      stmt2.monthRange shouldBe MAR / 2021 .. MAR / 2021
      stmt2.outFlows shouldBe 0
      stmt2.statements shouldBe listOf(stmt1)
      stmt2.totalPayments shouldBe 0
      stmt2.totalTransfers shouldBe 0
    }
  }

  describe("combineSummaryStatements") {
    it("empty") {
      val exception = shouldThrow<IllegalArgumentException> {
        combineSummaryStatements(summaryStatements = listOf())
      }
      exception.message shouldBe "Cant combine empty list of summary statements"
    }
    it("single") {
      val summaryStatement = SummaryStatement(
          nodeId = NodeId("test-account1"),
          monthRange = APR / 2026 .. MAY / 2026

      )
      val result = combineSummaryStatements(listOf(summaryStatement))
      result.nodeId shouldBe NodeId("test-account1")
      result.monthRange shouldBe APR / 2026..MAY / 2026
      result.totalPayments shouldBe 0
      result.totalTransfers shouldBe 0
      result.statements shouldBe listOf()
    }
    it("two different node statements fail") {
      val stmt1 = SummaryStatement(
        nodeId = NodeId("test-account1"),
        monthRange = APR / 2026 .. MAY / 2026

      )
      val stmt2 = SummaryStatement(
        nodeId = NodeId("test-account2"),
        monthRange = APR / 2026 .. MAY / 2026

      )
      val exception = shouldThrow<IllegalArgumentException> {
        combineSummaryStatements(listOf(stmt1, stmt2))
      }
      exception.message shouldBe "Cant combine different summary statements test-account1 and test-account2"
    }

    it("two node statements with different months") {
      val stmt1 = SummaryStatement(
        nodeId = NodeId("test-account1"),
        monthRange = APR / 2026 .. APR / 2026

      )
      val stmt2 = SummaryStatement(
        nodeId = NodeId("test-account1"),
        monthRange = MAY / 2026 .. MAY / 2026

      )
      val result = combineSummaryStatements(listOf(stmt1, stmt2))
      result.nodeId shouldBe NodeId("test-account1")
      result.monthRange shouldBe APR / 2026..MAY / 2026
      result.totalPayments shouldBe 0
      result.totalTransfers shouldBe 0
      result.statements shouldBe listOf()
    }
    it("two node statements with same months") {
      val stmt1 = SummaryStatement(
        nodeId = NodeId("test-account1"),
        monthRange = APR / 2026 .. APR / 2026

      )
      stmt1.addStatement(TransactionStatement(
        nodeId = NodeId("test-account1"),
        monthRange = APR / 2026 .. APR / 2026,
        isClosed = false,
        startBalance = Balance(100, LocalDate(2026, 4, 1), Balance.Type.CONFIRMED),
      ))
      val stmt2 = SummaryStatement(
        nodeId = NodeId("test-account1"),
        monthRange = APR / 2026 .. APR / 2026

      )
      stmt2.addStatement(TransactionStatement(
        nodeId = NodeId("test-account1"),
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
        nodeId = NodeId("/test-account1"),
        monthRange = APR / 2026 .. APR / 2026
      )
      stmt1.addStatement(TransactionStatement(
          nodeId = NodeId("test-account1"),
          monthRange = APR / 2026 .. APR / 2026,
          isClosed = false,
          startBalance = Balance(100, LocalDate(2026, 4, 1), Balance.Type.CONFIRMED),
      ))
      val stmt2 = SummaryStatement(
        nodeId = NodeId("/test-account1"),
        monthRange = MAY / 2026 .. MAY / 2026
      )
      stmt2.addStatement(TransactionStatement(
        nodeId = NodeId("test-account1"),
        monthRange = MAY / 2026 .. MAY / 2026,
        isClosed = false,
        startBalance = Balance(200, LocalDate(2026, 5, 1), Balance.Type.CONFIRMED),
      ))
      val result = combineSummaryStatements(listOf(stmt1, stmt2))
      result.nodeId shouldBe NodeId("/test-account1")
      result.monthRange shouldBe APR / 2026..MAY / 2026
      result.totalPayments shouldBe 0
      result.totalTransfers shouldBe 0
      result.income shouldBe 0
      result.change shouldBe -100
      result.statements.size shouldBe 1
      val statement = result.statements.first()
      statement.nodeId shouldBe NodeId("test-account1")
      statement.monthRange shouldBe APR / 2026..MAY / 2026
      statement.totalPayments shouldBe 0
      statement.totalTransfers shouldBe 0
      statement.income shouldBe 0
      statement.change shouldBe -100
    }
  }
})
