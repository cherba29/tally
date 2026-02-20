package com.cherba29.tally.statement

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.AccountType
import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.BalanceType
import com.cherba29.tally.core.Month
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate

internal class TestStatement(
  account: Account,
  month: Month,
  startBalance: Balance? = null,
  endBalance: Balance? = null,
  inFlows: Double = 0.0,
  outFlows: Double = 0.0,
  totalTransfers: Double = 0.0,
  totalPayments: Double = 0.0,
  income: Double = 0.0,
) : Statement(account, month, startBalance, endBalance, inFlows, outFlows, totalTransfers, totalPayments, income) {
  override val isClosed: Boolean = true
}

class StatementTest : DescribeSpec({
  describe("Creation") {
    it("basic") {
      val stmt = TestStatement(
        Account(name = "test", type = AccountType.BILL, owners = listOf()),
        Month.fromString("Mar2021")
      )
      stmt.account shouldBe Account(name = "test", type = AccountType.BILL, owners = listOf())
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

  it("with inFlow outFlow no start/end balance") {
    val stmt = TestStatement(
      Account(name = "test", type = AccountType.BILL, owners = listOf()),
      Month.fromString("Mar2021")
    )
    stmt.addInFlow(100.0)
    stmt.addInFlow(-10.0)
    stmt.addOutFlow(-30.0)
    stmt.addOutFlow(10.0)

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

  it("with inFlow outFlow with start/end balance") {
    val startBalance = Balance(1000.0, LocalDate.parse("2020-01-01"), BalanceType.PROJECTED)
    val endBalance = Balance(2000.0, LocalDate.parse("2020-02-01"), BalanceType.PROJECTED)
    val stmt = TestStatement(
      Account(name = "test", type = AccountType.BILL, owners = listOf()),
      Month.fromString("Mar2021"),
      startBalance,
      endBalance,
    )
    stmt.addInFlow(100.0)
    stmt.addInFlow(-10.0)
    stmt.addOutFlow(-30.0)
    stmt.addOutFlow(10.0)

    stmt.addSub shouldBe 70.0
    stmt.change shouldBe 1000.0
    stmt.endBalance shouldBe endBalance
    stmt.inFlows shouldBe 110.0
    stmt.income shouldBe 0.0
    stmt.isClosed shouldBe true
    stmt.outFlows shouldBe -40.0
    stmt.percentChange shouldBe 100.0
    stmt.startBalance shouldBe startBalance
    stmt.totalPayments shouldBe 0.0
    stmt.totalTransfers shouldBe 0.0
    stmt.unaccounted shouldBe 930.0
  }

  it("with empty statement") {
    val stmt = TestStatement(
      Account(name = "test", type = AccountType.BILL, owners = listOf()),
      Month.fromString("Mar2021")
    )
    stmt.startBalance shouldBe null
    stmt.endBalance shouldBe null
    stmt.totalTransfers shouldBe 0.0
    stmt.income shouldBe 0.0
    stmt.inFlows shouldBe 0.0
    stmt.outFlows shouldBe 0.0
    stmt.totalPayments shouldBe 0.0
    stmt.isEmpty() shouldBe true
  }

  it("with no start/end balance") {
    val stmt = TestStatement(
      Account(name = "test", type = AccountType.BILL, owners = listOf()),
      Month.fromString("Mar2021")
    )
    stmt.addInFlow(100.0)
    stmt.addInFlow(-10.0)
    stmt.addOutFlow(-30.0)
    stmt.addOutFlow(10.0)
    stmt.addSub shouldBe 70
    stmt.change shouldBe null
    stmt.percentChange shouldBe null
    stmt.unaccounted shouldBe null
  }

  it("with inFlow outFlow with start balance") {
    val startBalance = Balance(1000.0, LocalDate.parse("2020-01-01"), BalanceType.PROJECTED)
    val stmt = TestStatement(
      Account(name = "test", type = AccountType.BILL, owners = listOf()),
      Month.fromString("Mar2021"),
      startBalance
    )

    stmt.addInFlow(100.0)
    stmt.addInFlow(-10.0)
    stmt.addOutFlow(-30.0)
    stmt.addOutFlow(10.0)
    stmt.addSub shouldBe 70.0
    stmt.change shouldBe null
    stmt.percentChange shouldBe null
    stmt.unaccounted shouldBe null
  }

  it("with inFlow outFlow with end balance") {
    val endBalance = Balance(2000.0, LocalDate.parse("2020-02-01"), BalanceType.PROJECTED)
    val stmt = TestStatement(
      Account(name = "test", type = AccountType.BILL, owners = listOf()),
      Month.fromString("Mar2021"),
      null,
      endBalance
    )

    stmt.addInFlow(100.0)
    stmt.addInFlow(-10.0)
    stmt.addOutFlow(-30.0)
    stmt.addOutFlow(10.0)
    stmt.addSub shouldBe 70
    stmt.change shouldBe null
    stmt.percentChange shouldBe null
    stmt.unaccounted shouldBe null
  }

  it("percentChange") {
    val startBalance = Balance(1000.0, LocalDate.parse("2020-01-01"), BalanceType.PROJECTED)
    val endBalance = Balance(2000.0, LocalDate.parse("2020-02-01"), BalanceType.PROJECTED)

    val stmt = TestStatement(
      Account(name = "test", type = AccountType.BILL, owners = listOf()),
      Month.fromString("Mar2021"),
      startBalance,
      endBalance
    )
    stmt.addInFlow(100.0)
    stmt.addInFlow(-10.0)
    stmt.addOutFlow(-30.0)
    stmt.addOutFlow(10.0)
    stmt.percentChange shouldBe 100.0
  }

  it("change") {
    val startBalance = Balance(1000.0, LocalDate.parse("2020-01-01"), BalanceType.PROJECTED)
    val endBalance = Balance(2000.0, LocalDate.parse("2020-02-01"), BalanceType.PROJECTED)
    val stmt = TestStatement(
      Account(name = "test", type = AccountType.BILL, owners = listOf()),
      Month.fromString("Mar2021"),
      startBalance,
      endBalance
    )
    stmt.addInFlow(100.0)
    stmt.addInFlow(-10.0)
    stmt.addOutFlow(-30.0)
    stmt.addOutFlow(10.0)
    stmt.change shouldBe 1000.0
  }
})
