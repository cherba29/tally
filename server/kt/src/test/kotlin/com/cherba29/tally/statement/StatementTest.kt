package com.cherba29.tally.statement

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.MonthName.MAR
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate

internal class TestStatement(
  account: Account,
  month: Month,
  startBalance: Balance? = null,
  endBalance: Balance? = null,
  inFlows: Int = 0,
  outFlows: Int = 0,
  totalTransfers: Int = 0,
  totalPayments: Int = 0,
  income: Int = 0,
) : Statement(account, month, startBalance, endBalance, inFlows, outFlows, totalTransfers, totalPayments, income) {
  override val isClosed: Boolean = true
}

class StatementTest : DescribeSpec({
  describe("Creation") {
    it("basic") {
      val stmt = TestStatement(
        Account(
          name = "test",
          owners = listOf(),
          openedOn = MAR / 2021
        ),
        Month.fromString("Mar2021")
      )
      stmt.account shouldBe Account(name = "test", owners = listOf(), openedOn = MAR / 2021)
      stmt.month shouldBe Month(2021, 2)
      stmt.inFlows shouldBe 0.0
      stmt.income shouldBe 0.0
      stmt.outFlows shouldBe 0.0
      stmt.totalPayments shouldBe 0.0
      stmt.totalTransfers shouldBe 0.0
      stmt.addSub shouldBe 0
      stmt.change shouldBe null
      stmt.percentChange shouldBe null
      stmt.unaccounted shouldBe null
      stmt.isClosed shouldBe true
    }
  }

  it("with inFlow outFlow no start-end balance") {
    val stmt = TestStatement(
      Account(
        name = "test",
        path = listOf("external"),
        owners = listOf(),
        openedOn = MAR / 2021
      ),
      Month.fromString("Mar2021")
    )
    stmt.addInFlow(100)
    stmt.addInFlow(-10)
    stmt.addOutFlow(-30)
    stmt.addOutFlow(10)

    stmt.addSub shouldBe 70.0
    stmt.change shouldBe null
    stmt.endBalance shouldBe null
    stmt.inFlows shouldBe 110.0
    stmt.income shouldBe 0.0
    stmt.isClosed shouldBe true  // TODO: remove not useful.
    stmt.outFlows shouldBe -40.0
    stmt.percentChange shouldBe null
    stmt.totalPayments shouldBe 0.0
    stmt.totalTransfers shouldBe 0.0
    stmt.unaccounted shouldBe null
  }

  it("with inFlow outFlow with start-end balance") {
    val startBalance = Balance(1000, LocalDate.parse("2020-01-01"), Balance.Type.PROJECTED)
    val endBalance = Balance(2000, LocalDate.parse("2020-02-01"), Balance.Type.PROJECTED)
    val stmt = TestStatement(
      Account(
        name = "test",
        path = listOf("external"),
        owners = listOf(),
        openedOn = MAR / 2021
      ),
      Month.fromString("Mar2021"),
      startBalance,
      endBalance,
    )
    stmt.addInFlow(100)
    stmt.addInFlow(-10)
    stmt.addOutFlow(-30)
    stmt.addOutFlow(10)

    stmt.addSub shouldBe 70
    stmt.change shouldBe 1000
    stmt.endBalance shouldBe endBalance
    stmt.inFlows shouldBe 110
    stmt.income shouldBe 0
    stmt.isClosed shouldBe true
    stmt.outFlows shouldBe -40
    stmt.percentChange shouldBe 100
    stmt.startBalance shouldBe startBalance
    stmt.totalPayments shouldBe 0
    stmt.totalTransfers shouldBe 0
    stmt.unaccounted shouldBe 930
  }

  it("with empty statement") {
    val stmt = TestStatement(
      Account(
        name = "test",
        path = listOf("external"),
        owners = listOf(),
        openedOn = MAR / 2021
      ),
      Month.fromString("Mar2021")
    )
    stmt.startBalance shouldBe null
    stmt.endBalance shouldBe null
    stmt.totalTransfers shouldBe 0
    stmt.income shouldBe 0
    stmt.inFlows shouldBe 0
    stmt.outFlows shouldBe 0
    stmt.totalPayments shouldBe 0
    stmt.isEmpty() shouldBe true
  }

  it("with no start-end balance") {
    val stmt = TestStatement(
      Account(
        name = "test",
        path = listOf("external"),
        owners = listOf(),
        openedOn = MAR / 2021
      ),
      Month.fromString("Mar2021")
    )
    stmt.addInFlow(100)
    stmt.addInFlow(-10)
    stmt.addOutFlow(-30)
    stmt.addOutFlow(10)
    stmt.addSub shouldBe 70
    stmt.change shouldBe null
    stmt.percentChange shouldBe null
    stmt.unaccounted shouldBe null
  }

  it("with inFlow outFlow with start balance") {
    val startBalance = Balance(1000, LocalDate.parse("2020-01-01"), Balance.Type.PROJECTED)
    val stmt = TestStatement(
      Account(
        name = "test",
        path = listOf("external"),
        owners = listOf(),
        openedOn = MAR / 2021
      ),
      Month.fromString("Mar2021"),
      startBalance
    )

    stmt.addInFlow(100)
    stmt.addInFlow(-10)
    stmt.addOutFlow(-30)
    stmt.addOutFlow(10)
    stmt.addSub shouldBe 70.0
    stmt.change shouldBe null
    stmt.percentChange shouldBe null
    stmt.unaccounted shouldBe null
  }

  it("with inFlow outFlow with end balance") {
    val endBalance = Balance(2000, LocalDate.parse("2020-02-01"), Balance.Type.PROJECTED)
    val stmt = TestStatement(
      Account(
        name = "test",
        path = listOf("external"),
        owners = listOf(),
        openedOn = MAR / 2021
      ),
      Month.fromString("Mar2021"),
      null,
      endBalance
    )

    stmt.addInFlow(100)
    stmt.addInFlow(-10)
    stmt.addOutFlow(-30)
    stmt.addOutFlow(10)
    stmt.addSub shouldBe 70
    stmt.change shouldBe null
    stmt.percentChange shouldBe null
    stmt.unaccounted shouldBe null
  }

  it("percentChange") {
    val startBalance = Balance(1000, LocalDate.parse("2020-01-01"), Balance.Type.PROJECTED)
    val endBalance = Balance(2000, LocalDate.parse("2020-02-01"), Balance.Type.PROJECTED)

    val stmt = TestStatement(
      Account(
        name = "test",
        path = listOf("external"),
        owners = listOf(),
        openedOn = MAR / 2021
      ),
      Month.fromString("Mar2021"),
      startBalance,
      endBalance
    )
    stmt.addInFlow(100)
    stmt.addInFlow(-10)
    stmt.addOutFlow(-30)
    stmt.addOutFlow(10)
    stmt.percentChange shouldBe 100.0
    stmt.annualizedPercentChange shouldBe null  // since more than 1000%
  }

  it("change") {
    val startBalance = Balance(1000, LocalDate.parse("2020-01-01"), Balance.Type.PROJECTED)
    val endBalance = Balance(1020, LocalDate.parse("2020-02-01"), Balance.Type.PROJECTED)
    val stmt = TestStatement(
      Account(
        name = "test",
        path = listOf("external"),
        owners = listOf(),
        openedOn = MAR / 2021
      ),
      Month.fromString("Mar2021"),
      startBalance,
      endBalance
    )
    stmt.addInFlow(100)
    stmt.addInFlow(-10)
    stmt.addOutFlow(-30)
    stmt.addOutFlow(10)
    stmt.change shouldBe 20
    stmt.percentChange shouldBe 2.0
    stmt.annualizedPercentChange shouldBe (26.8242 plusOrMinus 0.0001)
  }
})
