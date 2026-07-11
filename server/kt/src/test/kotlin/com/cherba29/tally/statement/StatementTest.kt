package com.cherba29.tally.statement

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.MonthName.MAR
import com.cherba29.tally.core.root
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate

class StatementTest : DescribeSpec({
  describe("Creation") {
    it("basic") {
      val tree = root { branch("external") { leaf("test") } }
      val stmt = SummaryStatement(
        tree[listOf("external", "test")]!!,
        MAR / 2021..MAR / 2021
      )
      stmt.treeNode.path shouldBe listOf("external", "test")
      stmt.monthRange shouldBe MAR / 2021..MAR / 2021
      stmt.inFlows shouldBe 0L
      stmt.income shouldBe 0L
      stmt.outFlows shouldBe 0L
      stmt.totalPayments shouldBe 0L
      stmt.totalTransfers shouldBe 0L
      stmt.addSub shouldBe 0L
      stmt.change shouldBe null
      stmt.percentChange shouldBe null
      stmt.unaccounted shouldBe null
    }
  }

  it("with inFlow outFlow no start-end balance") {
    val tree = root { branch("external") { leaf("test") } }
    val stmt = SummaryStatement(
      tree[listOf("external", "test")]!!,
      MAR / 2021..MAR / 2021,
      inFlows = 110,
      outFlows = -40
    )

    stmt.addSub shouldBe 70L
    stmt.change shouldBe null
    stmt.endBalance shouldBe null
    stmt.inFlows shouldBe 110L
    stmt.income shouldBe 0L
    stmt.outFlows shouldBe -40L
    stmt.percentChange shouldBe null
    stmt.totalPayments shouldBe 0L
    stmt.totalTransfers shouldBe 0L
    stmt.unaccounted shouldBe null
  }

  it("with inFlow outFlow with start-end balance") {
    val tree = root { branch("external") { leaf("test") } }
    val startBalance = Balance(1000, LocalDate.parse("2020-01-01"), Balance.Type.PROJECTED)
    val endBalance = Balance(2000, LocalDate.parse("2020-02-01"), Balance.Type.PROJECTED)
    val stmt = SummaryStatement(
      tree[listOf("external", "test")]!!,
      MAR / 2021..MAR / 2021,
      false,
      startBalance,
      endBalance,
      inFlows = 110,
      outFlows = -40
    )

    stmt.addSub shouldBe 70L
    stmt.change shouldBe 1000L
    stmt.endBalance shouldBe endBalance
    stmt.inFlows shouldBe 110L
    stmt.income shouldBe 0L
    stmt.outFlows shouldBe -40L
    stmt.percentChange shouldBe 100.0
    stmt.startBalance shouldBe startBalance
    stmt.totalPayments shouldBe 0L
    stmt.totalTransfers shouldBe 0L
    stmt.unaccounted shouldBe 930L
  }

  it("with empty statement") {
    val tree = root { branch("external") { leaf("test") } }
    val stmt = SummaryStatement(
      tree[listOf("external", "test")]!!,
      MAR / 2021..MAR / 2021
    )
    stmt.startBalance shouldBe null
    stmt.endBalance shouldBe null
    stmt.totalTransfers shouldBe 0L
    stmt.income shouldBe 0L
    stmt.inFlows shouldBe 0L
    stmt.outFlows shouldBe 0L
    stmt.totalPayments shouldBe 0L
    stmt.isEmpty shouldBe true
  }

  it("with no start-end balance") {
    val tree = root { branch("external") { leaf("test") } }
    val stmt = SummaryStatement(
      tree[listOf("external", "test")]!!,
      MAR / 2021..MAR / 2021,
      inFlows = 110,
      outFlows = -40
    )
    stmt.addSub shouldBe 70
    stmt.change shouldBe null
    stmt.percentChange shouldBe null
    stmt.unaccounted shouldBe null
  }

  it("with inFlow outFlow with start balance") {
    val tree = root { branch("external") { leaf("test") } }
    val startBalance = Balance(1000, LocalDate.parse("2020-01-01"), Balance.Type.PROJECTED)
    val stmt = SummaryStatement(
      tree[listOf("external", "test")]!!,
      MAR / 2021..MAR / 2021,
      false,
      startBalance,
      inFlows = 110,
      outFlows = -40
    )
    stmt.addSub shouldBe 70L
    stmt.change shouldBe null
    stmt.percentChange shouldBe null
    stmt.unaccounted shouldBe null
  }

  it("with inFlow outFlow with end balance") {
    val tree = root { branch("external") { leaf("test") } }
    val endBalance = Balance(2000, LocalDate.parse("2020-02-01"), Balance.Type.PROJECTED)
    val stmt = SummaryStatement(
      tree[listOf("external", "test")]!!,
      MAR / 2021..MAR / 2021,
      false,
      null,
      endBalance,
      inFlows = 110,
      outFlows = -40
    )
    stmt.addSub shouldBe 70L
    stmt.change shouldBe null
    stmt.percentChange shouldBe null
    stmt.unaccounted shouldBe null
  }

  it("percentChange") {
    val tree = root { branch("external") { leaf("test") } }
    val startBalance = Balance(1000, LocalDate.parse("2020-01-01"), Balance.Type.PROJECTED)
    val endBalance = Balance(2000, LocalDate.parse("2020-02-01"), Balance.Type.PROJECTED)

    val stmt = SummaryStatement(
      tree[listOf("external", "test")]!!,
      MAR / 2021..MAR / 2021,
      false,
      startBalance,
      endBalance,
      inFlows = 110,
      outFlows = -40
    )
    stmt.percentChange shouldBe 100.0
    stmt.annualizedPercentChange shouldBe null // since more than 1000%
  }

  it("change") {
    val tree = root { branch("external") { leaf("test") } }
    val startBalance = Balance(1000, LocalDate.parse("2020-01-01"), Balance.Type.PROJECTED)
    val endBalance = Balance(1020, LocalDate.parse("2020-02-01"), Balance.Type.PROJECTED)
    val stmt = SummaryStatement(
      tree[listOf("external", "test")]!!,
      MAR / 2021..MAR / 2021,
      false,
      startBalance,
      endBalance,
      inFlows = 110,
      outFlows = -40
    )
    stmt.change shouldBe 20L
    stmt.percentChange shouldBe 2.0
    stmt.annualizedPercentChange shouldBe (26.8242 plusOrMinus 0.0001)
  }
})
