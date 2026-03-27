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
        name = "testAccount",
        openedOn = JAN / 2021
      )
      account.isClosed(MAR / 2021) shouldBe false
    }

    it("isClosed false if closedOn not set") {
      val account = Account(
        name = "testAccount",
        openedOn = NOV / 2020,
      )
      account.isClosed(MAR / 2021) shouldBe false
    }

    it("isClosed true if closedOn is set") {
      val account = Account(
        name = "testAccount",
        openedOn = FEB / 2021,
        closedOn = APR / 2021,
      )
      account.isClosed(MAY / 2021) shouldBe true
    }

    it("isClosed true if not opened yet") {
      val account = Account(
        name = "testAccount",
        openedOn = FEB / 2021,
        closedOn = APR / 2021,
      )
      account.isClosed(Month(2020, 1)) shouldBe true
    }
  }

  describe("type") {
    it("isSummary false if type is not summary") {
      val account = Account(
        name = "testAccount",
        openedOn = JAN / 2021
      )
      account.isSummary shouldBe false
    }

    it("isSummary when type is summary") {
      val account = Account(
        name = "/test/summary",
        openedOn = JAN / 2021
      )
      account.isSummary shouldBe true
    }

    it("isExternal false by default") {
      val account = Account(
        name = "testAccount",
        openedOn = JAN / 2021
      )
      account.isExternal shouldBe false
    }

    it("isExternal true when path starts with external") {
      val account = Account(
        name = "testAccount",
        path = listOf("external"),
        owners = listOf("bob"),
        openedOn = JAN / 2021
      )
      account.isExternal shouldBe true
    }
  }

  describe("owner") {
    it("has common owner is false if no common owners") {
      val account1 = Account(
        name = "testAccount",
        owners = listOf("bob"),
        openedOn = JAN / 2021
      )
      val account2 = Account(
        name = "testAccount",
        owners = listOf("john'"),
        openedOn = JAN / 2021
      )

      account1.hasCommonOwner(account2) shouldBe false
    }

    it("has common owner is true if common owners") {
      val account1 = Account(
        name = "testAccount",
        owners = listOf("bob"),
        openedOn = JAN / 2021
      )
      val account2 = Account(
        name = "testAccount",
        owners = listOf("john", "bob"),
        openedOn = JAN / 2021
      )

      account1.hasCommonOwner(account2) shouldBe true
    }
  }

  describe("conversions") {
    it("toString") {
      val account = Account(
        name = "testAccount",
        owners = listOf("bob"),
        openedOn = JAN / 2021
      )
      account.toString() shouldBe "Account testAccount /"
    }

    it("toString closed") {
      val account = Account(
        name = "testAccount",
        path = listOf("internal", "tax"),
        owners = listOf("bob"),
        openedOn = JAN / 2021,
        closedOn = MAR / 2026
      )
      account.toString() shouldBe "Account testAccount /internal/tax Closed Mar2026"
    }
  }
})
