package com.cherba29.tally.statement

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.MonthName.MAR
import com.cherba29.tally.core.NodeId
import com.cherba29.tally.utils.Map3
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate

class SummaryTest : DescribeSpec({
  describe("Build") {
    it("empty") {
      val statements = buildSummaryStatementTable(listOf(), selectedOwner = null).toList()
      statements shouldBe listOf()
    }

    it("single closed account - produces summary without it") {
      val node1 = NodeId(
        name = "test-account1",
        path = listOf("external"),
        owners = listOf("john"),
      )
      val startBalance = Balance(
        100,
        LocalDate(2023, 12, 2),
        Balance.Type.CONFIRMED
      )
      val tranStmt = TransactionStatement(
        node1,
        MAR / 2021,
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
        owners = listOf("john")
      )
      withClue("statement: $stmt1") {
        stmt1?.startBalance shouldBe startBalance
        stmt1?.endBalance shouldBe null
        stmt1?.inFlows shouldBe 0
        stmt1?.income shouldBe 0
        stmt1?.month shouldBe MAR / 2021
        stmt1?.startMonth shouldBe MAR / 2021
        stmt1?.outFlows shouldBe 0
        stmt1?.statements shouldBe listOf(tranStmt)
        stmt1?.totalPayments shouldBe 0
        stmt1?.totalTransfers shouldBe 0
      }

      val stmt2 = statements["john", "/", "Mar2021"]
      stmt2?.nodeId shouldBe NodeId(
        name = "/",
        path = listOf(),
        owners = listOf("john")
      )
      stmt2?.startBalance shouldBe startBalance
      stmt2?.endBalance shouldBe null
      stmt2?.inFlows shouldBe 0
      stmt2?.income shouldBe 0
      stmt2?.month shouldBe MAR / 2021
      stmt2?.startMonth shouldBe MAR / 2021
      stmt2?.outFlows shouldBe 0
      stmt2?.statements shouldBe listOf(stmt1)
      stmt2?.totalPayments shouldBe 0
      stmt2?.totalTransfers shouldBe 0
    }

    it("single external account - no SUMMARY") {
      val node1 = NodeId(
        name = "test-account1",
        path = listOf("external"),
        owners = listOf("john")
      )

      val tranStmt = TransactionStatement(node1, Month.fromString("Mar2021"), false, startBalance = null)
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
        owners = listOf("john"),
      )
      stmt.startBalance shouldBe Balance(100, LocalDate(2023, 12, 2), Balance.Type.CONFIRMED)
      stmt.endBalance shouldBe null
      stmt.inFlows shouldBe 0
      stmt.income shouldBe 0
      stmt.month shouldBe Month.fromString("Mar2021")
      stmt.startMonth shouldBe Month.fromString("Mar2021")
      stmt.outFlows shouldBe 0
      stmt.statements shouldBe listOf(statements["john", "/external", "Mar2021"]!!)
      stmt.totalPayments shouldBe 0
      stmt.totalTransfers shouldBe 0
    }

    it("single account - no transfers") {
      val node1 = NodeId(
        name = "test-account1",
        path = listOf("external"),
        owners = listOf("john")
      )

      val tranStmt = TransactionStatement(node1, Month.fromString("Mar2021"), false, startBalance = null)
      tranStmt.startBalance = Balance(100, LocalDate(2023, 12, 2), Balance.Type.CONFIRMED)
      val statements: Map3<SummaryStatement> = buildSummaryStatementTable(listOf(tranStmt), selectedOwner = null)
      statements.isEmpty shouldBe false
      statements.size shouldBe 2
      statements.keys.keys shouldBe setOf("john")
      statements.keys["john"] shouldBe setOf("/external", "/")

      val stmt1 = statements["john", "/external", "Mar2021"]!!
      stmt1.nodeId shouldBe NodeId(
        name = "/external",
        path = listOf(),
        owners = listOf("john")
      )
      stmt1.startBalance shouldBe Balance(100, LocalDate(2023, 12, 2), Balance.Type.CONFIRMED)
      stmt1.endBalance shouldBe null
      stmt1.inFlows shouldBe 0
      stmt1.income shouldBe 0
      stmt1.month shouldBe Month.fromString("Mar2021")
      stmt1.startMonth shouldBe Month.fromString("Mar2021")
      stmt1.outFlows shouldBe 0
      stmt1.statements shouldBe listOf(tranStmt)
      stmt1.totalPayments shouldBe 0
      stmt1.totalTransfers shouldBe 0

      val stmt2 = statements["john", "/", "Mar2021"]!!
      stmt2.nodeId shouldBe NodeId(
        name = "/",
        path = listOf(),
        owners = listOf("john"),
      )
      stmt2.startBalance shouldBe Balance(100, LocalDate(2023, 12, 2), Balance.Type.CONFIRMED)
      stmt2.endBalance shouldBe null
      stmt2.inFlows shouldBe 0
      stmt2.income shouldBe 0
      stmt2.month shouldBe Month.fromString("Mar2021")
      stmt2.startMonth shouldBe Month.fromString("Mar2021")
      stmt2.outFlows shouldBe 0
      stmt2.statements shouldBe listOf(stmt1)
      stmt2.totalPayments shouldBe 0
      stmt2.totalTransfers shouldBe 0
    }
  }
})