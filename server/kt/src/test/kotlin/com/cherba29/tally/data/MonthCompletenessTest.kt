package com.cherba29.tally.data

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.MonthName.AUG
import com.cherba29.tally.core.MonthName.DEC
import com.cherba29.tally.core.MonthName.JUL
import com.cherba29.tally.core.MonthName.NOV
import com.cherba29.tally.core.MonthName.OCT
import com.cherba29.tally.core.MonthName.SEP
import com.cherba29.tally.data.builder.BudgetBuilder
import com.cherba29.tally.data.builder.budget
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.shouldBeBetween
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.shouldBe
import java.nio.file.Paths

class MonthCompletenessTest : DescribeSpec({
  val checkingPath = listOf("john", "internal", "checking")
  val expensesPath = listOf("john", "external", "expenses")

  fun twoAccountBudget(
    octStartBal: Balance?,
    novStartBal: Balance?,
    expOctStartBal: Balance? = Balance.confirmed(0, "2026-10-01"),
    expNovStartBal: Balance? = Balance.confirmed(10000, "2026-11-01"),
    transfers: List<Balance> = listOf(Balance.confirmed(10000, "2026-10-15"))
  ): Budget {
    val checking = Account("checking", path = listOf("internal"), owners = setOf("john"), openedOn = OCT / 2026)
    val expenses = Account("expenses", path = listOf("external"), owners = setOf("john"), openedOn = OCT / 2026)
    return budget {
      setAccount(checkingPath, checking)
      setAccount(expensesPath, expenses)
      if (octStartBal != null) setBalance(checkingPath, OCT / 2026, octStartBal)
      if (novStartBal != null) setBalance(checkingPath, NOV / 2026, novStartBal)
      if (expOctStartBal != null) setBalance(expensesPath, OCT / 2026, expOctStartBal)
      if (expNovStartBal != null) setBalance(expensesPath, NOV / 2026, expNovStartBal)
      for ((i, bal) in transfers.withIndex()) {
        addTransfer(
          BudgetBuilder.TransferRecord(
            toAccountName = "expenses",
            fromAccountPath = checkingPath,
            month = OCT / 2026,
            balance = bal,
            description = "tx-$i",
            tags = listOf()
          )
        )
      }
    }
  }

  describe("MonthCompleteness") {
    it("returns 100% when all open accounts have confirmed balances, confirmed transactions, and 0 unaccounted") {
      val b = twoAccountBudget(
        octStartBal = Balance.confirmed(100000, "2026-10-01"),
        novStartBal = Balance.confirmed(90000, "2026-11-01")
      )
      val pct = MonthCompleteness.monthCompletenessPercent(b, b.tree["john"]!!, OCT / 2026)
      pct shouldBe 100.0f
    }

    it("returns 100% for dormant accounts with confirmed matching balances and no transactions") {
      val savingsPath = listOf("john", "internal", "savings")
      val savings = Account("savings", path = listOf("internal"), owners = setOf("john"), openedOn = OCT / 2026)
      val b = budget {
        setAccount(savingsPath, savings)
        setBalance(savingsPath, OCT / 2026, Balance.confirmed(50000, "2026-10-01"))
        setBalance(savingsPath, NOV / 2026, Balance.confirmed(50000, "2026-11-01"))
      }
      val pct = MonthCompleteness.monthCompletenessPercent(b, b.tree["john"]!!, OCT / 2026)
      pct shouldBe 100.0f
    }

    it("scores confirmed > projected > unset for balances") {
      val confirmed = twoAccountBudget(
        octStartBal = Balance.confirmed(100000, "2026-10-01"),
        novStartBal = Balance.confirmed(90000, "2026-11-01")
      )
      val projected = twoAccountBudget(
        octStartBal = Balance.confirmed(100000, "2026-10-01"),
        novStartBal = Balance.projected(90000, "2026-11-01")
      )
      val unset = twoAccountBudget(
        octStartBal = Balance.confirmed(100000, "2026-10-01"),
        novStartBal = null
      )

      val confirmedPct = MonthCompleteness.monthCompletenessPercent(confirmed, confirmed.tree["john"]!!, OCT / 2026)
      val projectedPct = MonthCompleteness.monthCompletenessPercent(projected, projected.tree["john"]!!, OCT / 2026)
      val unsetPct = MonthCompleteness.monthCompletenessPercent(unset, unset.tree["john"]!!, OCT / 2026)

      confirmedPct shouldBe 100.0f
      projectedPct shouldBeLessThan confirmedPct
      projectedPct shouldBeGreaterThan unsetPct
      unsetPct shouldBeGreaterThan 0.0f
    }

    it("scores confirmed transactions higher than projected transactions and strictly below 100%") {
      val allConfirmed = twoAccountBudget(
        octStartBal = Balance.confirmed(100000, "2026-10-01"),
        novStartBal = Balance.confirmed(90000, "2026-11-01"),
        transfers = listOf(Balance.confirmed(6000, "2026-10-10"), Balance.confirmed(4000, "2026-10-20"))
      )
      val mixed = twoAccountBudget(
        octStartBal = Balance.confirmed(100000, "2026-10-01"),
        novStartBal = Balance.confirmed(90000, "2026-11-01"),
        transfers = listOf(Balance.confirmed(6000, "2026-10-10"), Balance.projected(4000, "2026-10-20"))
      )
      val allProjected = twoAccountBudget(
        octStartBal = Balance.confirmed(100000, "2026-10-01"),
        novStartBal = Balance.confirmed(90000, "2026-11-01"),
        transfers = listOf(Balance.projected(6000, "2026-10-10"), Balance.projected(4000, "2026-10-20"))
      )

      val confirmedPct =
        MonthCompleteness.monthCompletenessPercent(allConfirmed, allConfirmed.tree["john"]!!, OCT / 2026)
      val mixedPct = MonthCompleteness.monthCompletenessPercent(mixed, mixed.tree["john"]!!, OCT / 2026)
      val projectedPct =
        MonthCompleteness.monthCompletenessPercent(allProjected, allProjected.tree["john"]!!, OCT / 2026)

      confirmedPct shouldBe 100.0f
      mixedPct shouldBeLessThan 100.0f
      mixedPct shouldBeGreaterThan projectedPct
    }

    it("reduces completeness as relative unaccounted amount grows and never rounds non-zero unaccounted to 100%") {
      val tinyUnaccounted = twoAccountBudget(
        octStartBal = Balance.confirmed(10000000, "2026-10-01"),
        novStartBal = Balance.confirmed(9989999, "2026-11-01") // 1 cent unaccounted
      )
      val smallUnaccounted = twoAccountBudget(
        octStartBal = Balance.confirmed(100000, "2026-10-01"),
        novStartBal = Balance.confirmed(85000, "2026-11-01") // $50 unaccounted out of $1000
      )
      val largeUnaccounted = twoAccountBudget(
        octStartBal = Balance.confirmed(100000, "2026-10-01"),
        novStartBal = Balance.confirmed(40000, "2026-11-01") // $500 unaccounted out of $1000
      )

      val tinyPct =
        MonthCompleteness.monthCompletenessPercent(tinyUnaccounted, tinyUnaccounted.tree["john"]!!, OCT / 2026)
      val smallPct =
        MonthCompleteness.monthCompletenessPercent(smallUnaccounted, smallUnaccounted.tree["john"]!!, OCT / 2026)
      val largePct =
        MonthCompleteness.monthCompletenessPercent(largeUnaccounted, largeUnaccounted.tree["john"]!!, OCT / 2026)

      tinyPct shouldBeLessThan 100.0f
      tinyPct shouldBe 99.9f
      smallPct shouldBeLessThan tinyPct
      largePct shouldBeLessThan smallPct
    }

    it("returns 0% when all balances and transactions are unset") {
      val b = twoAccountBudget(
        octStartBal = null,
        novStartBal = null,
        expOctStartBal = null,
        expNovStartBal = null,
        transfers = emptyList()
      )
      val pct = MonthCompleteness.monthCompletenessPercent(b, b.tree["john"]!!, OCT / 2026)
      pct shouldBe 0.0f
    }

    it("computes expected completeness progression on sample-data") {
      val sampleBudget = Loader.loadFrom(Paths.get("../../sample-data").normalize())
      val table = sampleBudget.table("alex", JUL / 2026, DEC / 2026)
      val byMonth = table.months.zip(table.monthCompleteness).toMap()

      // Jul..Oct 2026 are fully confirmed and balanced.
      byMonth[JUL / 2026] shouldBe 100.0f
      byMonth[AUG / 2026] shouldBe 100.0f
      byMonth[SEP / 2026] shouldBe 100.0f
      byMonth[OCT / 2026] shouldBe 100.0f

      // Nov 2026 has confirmed start balances, projected end balances, and some projected transfers.
      val novPct = byMonth.getValue(NOV / 2026)
      novPct.shouldBeBetween(70.0f, 95.0f, 0.01f)

      // Dec 2026 has projected start balances, unset end balances, and minimal projected transfers.
      val decPct = byMonth.getValue(DEC / 2026)
      decPct.shouldBeBetween(10.0f, 40.0f, 0.01f)
      novPct shouldBeGreaterThan decPct
    }
  }
})
