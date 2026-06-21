package com.cherba29.tally.core

import com.cherba29.tally.core.MonthName.APR
import com.cherba29.tally.core.MonthName.FEB
import com.cherba29.tally.core.MonthName.JAN
import com.cherba29.tally.core.MonthName.MAR
import com.cherba29.tally.core.MonthName.MAY
import com.cherba29.tally.core.MonthName.NOV
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class AccountTest : DescribeSpec({
  describe("open-closed") {
    it("isClosed - false by default") {
      val account = Account(
        "testAccount",
        path = listOf(),
        owners = setOf(),
        isSummary = false,
        openedOn = JAN / 2021
      )
      account.isClosed(MAR / 2021) shouldBe false
    }

    it("isClosed false if closedOn not set") {
      val account = Account(
        "testAccount",
        path = listOf(),
        owners = setOf(),
        isSummary = false,
        openedOn = NOV / 2020,
      )
      account.isClosed(MAR / 2021) shouldBe false
    }

    it("isClosed true if closedOn is set") {
      val account = Account(
        "testAccount",
        path = listOf(),
        owners = setOf(),
        isSummary = false,
        openedOn = FEB / 2021,
        closedOn = APR / 2021,
      )
      account.isClosed(MAY / 2021) shouldBe true
    }

    it("isClosed true if not opened yet") {
      val account = Account(
        "testAccount",
        path = listOf(),
        owners = setOf(),
        isSummary = false,
        openedOn = FEB / 2021,
        closedOn = APR / 2021,
      )
      account.isClosed(Month(2020, 1)) shouldBe true
    }
  }

  describe("conversions") {
    it("toString") {
      val account = Account(
        name = "testAccount",
        path = listOf(),
        owners = setOf("bob"),
        isSummary = false,
        openedOn = JAN / 2021
      )
      account.toString() shouldBe "Account testAccount /"
    }

    it("toString closed") {
      val account = Account(
        name = "testAccount",
        path = listOf("internal", "tax"),
        owners = setOf("bob"),
        isSummary = false,
        openedOn = JAN / 2021,
        closedOn = MAR / 2026
      )
      account.toString() shouldBe "Account testAccount /internal/tax Closed Mar2026"
    }
  }
})
