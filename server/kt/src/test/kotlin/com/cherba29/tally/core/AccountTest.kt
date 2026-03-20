package com.cherba29.tally.core

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class AccountTest : DescribeSpec({
  describe("open/closed") {
    it("isClosed - false by default") {
      val account = Account(
        name = "testAccount",
        type = Account.Type.CHECKING,
        owners = listOf("bob"),
      )
      account.isClosed(Month(2021, 2)) shouldBe false
    }

    it("isClosed false if closedOn not set") {
      val account = Account(
        name = "testAccount",
        type = Account.Type.CHECKING,
        owners = listOf("bob"),
        openedOn = Month(2021, 1),
      )
      account.isClosed(Month(2021, 2)) shouldBe false
    }

    it("isClosed true if closedOn is set") {
      val account = Account(
        name = "testAccount",
        type = Account.Type.CHECKING,
        owners = listOf("bob"),
        openedOn = Month(2021, 1),
        closedOn = Month(2021, 3),
      )
      account.isClosed(Month(2021, 4)) shouldBe true
    }

    it("isClosed true if not opened yet") {
      val account = Account(
        name = "testAccount",
        type = Account.Type.CHECKING,
        owners = listOf("bob"),
        openedOn = Month(2021, 1),
        closedOn = Month(2021, 3),
      )
      account.isClosed(Month(2020, 1)) shouldBe true
    }
  }

  describe("type") {
    it("isSummary false if type is not summary") {
      val account = Account(
        name = "testAccount",
        type = Account.Type.CHECKING,
        owners = listOf("bob"),
      )
      account.isSummary shouldBe false
    }

    it("isSummary when type is summary") {
      val account = Account(
        name = "testAccount",
        type = Account.Type.SUMMARY,
        owners = listOf("bob"),
      )
      account.isSummary shouldBe true
    }

    it("isExternal false by default") {
      val account = Account(
        name = "testAccount",
        type = Account.Type.CHECKING,
        owners = listOf("bob"),
      )
      account.isExternal shouldBe false
    }

    it("isExternal true when type is EXTERNAL") {
      val account = Account(
        name = "testAccount",
        type = Account.Type.EXTERNAL,
        owners = listOf("bob"),
      )
      account.isExternal shouldBe true
    }

    it("isExternal true when type is TAX") {
      val account = Account(
        name = "testAccount",
        type = Account.Type.TAX,
        owners = listOf("bob"),
      )
      account.isExternal shouldBe true
    }

    it("isExternal true when type is DEFERRED_INCOME") {
      val account = Account(
        name = "testAccount",
        type = Account.Type.DEFERRED_INCOME,
        owners = listOf("bob"),
      )
      account.isExternal shouldBe true
    }
  }
  describe("owner") {
    it("has common owner is false if no common owners") {
      val account1 = Account(
        name = "testAccount",
        type = Account.Type.CHECKING,
        owners = listOf("bob"),
      )
      val account2 = Account(
        name = "testAccount",
        type = Account.Type.CHECKING,
        owners = listOf("john'"),
      )

      account1.hasCommonOwner(account2) shouldBe false
    }

    it("has common owner is true if common owners") {
      val account1 = Account(
        name = "testAccount",
        type = Account.Type.CHECKING,
        owners = listOf("bob"),
      )
      val account2 = Account(
        name = "testAccount",
        type = Account.Type.CHECKING,
        owners = listOf("john", "bob"),
      )

      account1.hasCommonOwner(account2) shouldBe true
    }
  }

  describe("conversions") {
    it("toString") {
      val account = Account(
        name = "testAccount",
        type = Account.Type.TAX,
        owners = listOf("bob"),
      )
      account.toString() shouldBe "Account testAccount tax_"
    }

    it("toString closed") {
      val account = Account(
        name = "testAccount",
        type = Account.Type.TAX,
        owners = listOf("bob"),
        closedOn = Month(2026, 2)
      )
      account.toString() shouldBe "Account testAccount tax_ Closed Mar2026"
    }

  }
})