package com.cherba29.tally.statement

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.MonthName.MAR
import com.cherba29.tally.utils.Map3
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
      val account1 = Account(
        name = "test-account1",
        type = Account.Type.CHECKING,
        owners = listOf("john"),
        openedOn = Month.fromString("Jan2019"),
        closedOn = Month.fromString("Jan2020"),
      )

      val tranStmt = TransactionStatement(account1, Month.fromString("Mar2021"), startBalance = null)
      tranStmt.startBalance = Balance(100, LocalDate(2023, 12, 2), Balance.Type.CONFIRMED)
      val statements: Map3<SummaryStatement> = buildSummaryStatementTable(listOf(tranStmt), selectedOwner = null)
      val stmt1 = statements["john", "john CHECKING", "Mar2021"]
      stmt1?.account shouldBe Account(
        name = "john CHECKING",
        type = Account.Type.SUMMARY,
        owners = listOf("john"),
        openedOn = MAR / 2021,
      )
      stmt1?.startBalance shouldBe null
      stmt1?.endBalance shouldBe null
      stmt1?.inFlows shouldBe 0
      stmt1?.income shouldBe 0
      stmt1?.month shouldBe Month.fromString("Mar2021")
      stmt1?.startMonth shouldBe Month.fromString("Mar2021")
      stmt1?.outFlows shouldBe 0
      stmt1?.statements shouldBe listOf()
      stmt1?.totalPayments shouldBe 0
      stmt1?.totalTransfers shouldBe 0

      val stmt2 = statements["john", "john SUMMARY", "Mar2021"]
      stmt2?.account shouldBe Account(
        name = "john SUMMARY",
        type = Account.Type.SUMMARY,
        owners = listOf("john"),
        openedOn = MAR / 2021,
      )
      stmt2?.startBalance shouldBe null
      stmt2?.endBalance shouldBe null
      stmt2?.inFlows shouldBe 0
      stmt2?.income shouldBe 0
      stmt2?.month shouldBe Month.fromString("Mar2021")
      stmt2?.startMonth shouldBe Month.fromString("Mar2021")
      stmt2?.outFlows shouldBe 0
      stmt2?.statements shouldBe listOf()
      stmt2?.totalPayments shouldBe 0
      stmt2?.totalTransfers shouldBe 0
    }

    it("single external account - no SUMMARY") {
      val account1 = Account(
        name = "test-account1",
        type = Account.Type.EXTERNAL,
        owners = listOf("john"),
        openedOn = Month.fromString("Jan2019"),
      )

      val tranStmt = TransactionStatement(account1, Month.fromString("Mar2021"), startBalance = null)
      tranStmt.startBalance = Balance(100, LocalDate(2023, 12, 2), Balance.Type.CONFIRMED)
      val statements: Map3<SummaryStatement> = buildSummaryStatementTable(listOf(tranStmt), selectedOwner = null)
      val stmt = statements["john", "john EXTERNAL", "Mar2021"]!!
      stmt.account shouldBe Account(
        name = "john EXTERNAL",
        type = Account.Type.SUMMARY,
        owners = listOf("john"),
        openedOn = MAR / 2021,
      )
      stmt.startBalance shouldBe Balance(100, LocalDate(2023, 12, 2), Balance.Type.CONFIRMED)
      stmt.endBalance shouldBe null
      stmt.inFlows shouldBe 0
      stmt.income shouldBe 0
      stmt.month shouldBe Month.fromString("Mar2021")
      stmt.startMonth shouldBe Month.fromString("Mar2021")
      stmt.outFlows shouldBe 0
      stmt.statements shouldBe listOf(tranStmt)
      stmt.totalPayments shouldBe 0
      stmt.totalTransfers shouldBe 0
    }

    it("single account - no transfers") {
      val account1 = Account(
        name = "test-account1",
        type = Account.Type.CHECKING,
        owners = listOf("john"),
        openedOn = Month.fromString("Jan2021"),
      )

      val tranStmt = TransactionStatement(account1, Month.fromString("Mar2021"), startBalance = null)
      tranStmt.startBalance = Balance(100, LocalDate(2023, 12, 2), Balance.Type.CONFIRMED)
      val statements: Map3<SummaryStatement> = buildSummaryStatementTable(listOf(tranStmt), selectedOwner = null)
      val stmt1 = statements["john", "john CHECKING", "Mar2021"]!!
      stmt1.account shouldBe Account(
        name = "john CHECKING",
        type = Account.Type.SUMMARY,
        owners = listOf("john"),
        openedOn = MAR / 2021,
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

      val stmt2 = statements["john", "john SUMMARY", "Mar2021"]!!
      stmt2.account shouldBe Account(
        name = "john SUMMARY",
        type = Account.Type.SUMMARY,
        owners = listOf("john"),
        openedOn = MAR / 2021,
      )
      stmt2.startBalance shouldBe Balance(100, LocalDate(2023, 12, 2), Balance.Type.CONFIRMED)
      stmt2.endBalance shouldBe null
      stmt2.inFlows shouldBe 0
      stmt2.income shouldBe 0
      stmt2.month shouldBe Month.fromString("Mar2021")
      stmt2.startMonth shouldBe Month.fromString("Mar2021")
      stmt2.outFlows shouldBe 0
      stmt2.statements shouldBe listOf(tranStmt)
      stmt2.totalPayments shouldBe 0
      stmt2.totalTransfers shouldBe 0
    }
  }
})