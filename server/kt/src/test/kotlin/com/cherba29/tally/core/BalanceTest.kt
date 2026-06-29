package com.cherba29.tally.core

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate

class BalanceTest : DescribeSpec({
  describe("negation") {
    it("flips sign and preserves type") {
      val balance = Balance(100, LocalDate(2020, 2, 3), Balance.Type.CONFIRMED)
      val negated = -balance
      negated.date shouldBe LocalDate(2020, 2, 3)
      negated.amount shouldBe -100L
      negated.type shouldBe Balance.Type.CONFIRMED
    }
  }
  describe("addition") {
    it("same type") {
      val balance1 = Balance(100, LocalDate(2020, 2, 3), Balance.Type.CONFIRMED)
      val balance2 = Balance(200, LocalDate(2020, 3, 3), Balance.Type.CONFIRMED)
      val sum = balance1 + balance2
      sum.date shouldBe LocalDate(2020, 3, 3)
      sum.amount shouldBe 300L
      sum.type shouldBe Balance.Type.CONFIRMED
    }

    it("different types") {
      val balance1 = Balance(100, LocalDate(2020, 2, 3), Balance.Type.CONFIRMED)
      val balance2 = Balance(200, LocalDate(2020, 1, 3), Balance.Type.PROJECTED)
      val sum = balance1 + balance2
      sum.date shouldBe LocalDate(2020, 2, 3)
      sum.amount shouldBe 300L
      sum.type shouldBe Balance.Type.PROJECTED
    }

    it("first null") {
      val balance1: Balance? = null
      val balance2 = Balance(200, LocalDate(2020, 1, 3), Balance.Type.PROJECTED)
      val sum = balance1 + balance2
      sum?.date shouldBe LocalDate(2020, 1, 3)
      sum?.amount shouldBe 200L
      sum?.type shouldBe Balance.Type.PROJECTED
    }

    it("second null") {
      val balance1 = Balance(100, LocalDate(2020, 2, 3), Balance.Type.CONFIRMED)
      val balance2: Balance? = null
      val sum = balance1 + balance2
      sum?.date shouldBe LocalDate(2020, 2, 3)
      sum?.amount shouldBe 100L
      sum?.type shouldBe Balance.Type.CONFIRMED
    }
    it("both null") {
      val balance1: Balance? = null
      val balance2: Balance? = null
      val sum = balance1 + balance2
      sum shouldBe null
    }
  }

  describe("subtraction") {
    it("different types") {
      val balance1 = Balance(100, LocalDate(2020, 2, 3), Balance.Type.CONFIRMED)
      val balance2 = Balance(200, LocalDate(2020, 1, 3), Balance.Type.PROJECTED)
      val sum = balance1 - balance2
      sum.date shouldBe LocalDate(2020, 2, 3)
      sum.amount shouldBe -100L
      sum.type shouldBe Balance.Type.PROJECTED
    }
    it("first null") {
      val balance1: Balance? = null
      val balance2 = Balance(200, LocalDate(2020, 1, 3), Balance.Type.PROJECTED)
      val sum = balance1 - balance2
      sum?.date shouldBe LocalDate(2020, 1, 3)
      sum?.amount shouldBe -200L
      sum?.type shouldBe Balance.Type.PROJECTED
    }

    it("second null") {
      val balance2: Balance? = null
      val balance1 = Balance(100, LocalDate(2020, 2, 3), Balance.Type.CONFIRMED)
      val sum = balance1 - balance2
      sum?.date shouldBe LocalDate(2020, 2, 3)
      sum?.amount shouldBe 100L
      sum?.type shouldBe Balance.Type.CONFIRMED
    }

    it("both null") {
      val balance1: Balance? = null
      val balance2: Balance? = null
      val sum = balance1 - balance2
      sum shouldBe null
    }
  }

  describe("comparison") {
    it("less than") {
      val balance1 = Balance(100, LocalDate(2020, 2, 3), Balance.Type.CONFIRMED)
      val balance2 = Balance(200, LocalDate(2020, 1, 3), Balance.Type.PROJECTED)
      balance2 shouldBeLessThan balance1
    }
    it("greater than") {
      val balance1 = Balance(100, LocalDate(2020, 2, 3), Balance.Type.CONFIRMED)
      val balance2 = Balance(200, LocalDate(2020, 1, 3), Balance.Type.PROJECTED)
      balance1 shouldBeGreaterThan balance2
    }

    it("equal") {
      val balance1 = Balance(100, LocalDate(2020, 2, 3), Balance.Type.CONFIRMED)
      val balance2 = Balance(100, LocalDate(2020, 2, 3), Balance.Type.CONFIRMED)
      balance1 shouldBeEqual balance2
    }
  }

  describe("conversions") {
    it("toString") {
      val balance = Balance(10000, LocalDate(2020, 2, 3), Balance.Type.CONFIRMED)
      balance.toString() shouldBe "Balance { amount: 100.00, date: 2020-02-03, type: CONFIRMED }"
    }
  }

  describe("constructors") {
    it("confirmed") {
      val balance = Balance.confirmed(10000, "2020-02-03")
      balance.toString() shouldBe "Balance { amount: 100.00, date: 2020-02-03, type: CONFIRMED }"
    }
    it("projected") {
      val balance = Balance.projected(10000, "2020-02-03")
      balance.toString() shouldBe "Balance { amount: 100.00, date: 2020-02-03, type: PROJECTED }"
    }
  }
})