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
      val balance = Balance(100, LocalDate(2020, 2, 3), BalanceType.CONFIRMED)
      val negated = Balance.negated(balance)
      negated.date shouldBe LocalDate(2020, 2, 3)
      negated.amount shouldBe -100.0
      negated.type shouldBe BalanceType.CONFIRMED
    }
  }
  describe("addition") {
    it("same type") {
      val balance1 = Balance(100, LocalDate(2020, 2, 3), BalanceType.CONFIRMED)
      val balance2 = Balance(200, LocalDate(2020, 3, 3), BalanceType.CONFIRMED)
      val sum = Balance.add(balance1, balance2)
      sum.date shouldBe LocalDate(2020, 3, 3)
      sum.amount shouldBe 300.0
      sum.type shouldBe BalanceType.CONFIRMED
    }

    it("different types") {
      val balance1 = Balance(100, LocalDate(2020, 2, 3), BalanceType.CONFIRMED)
      val balance2 = Balance(200, LocalDate(2020, 1, 3), BalanceType.PROJECTED)
      val sum = Balance.add(balance1, balance2)
      sum.date shouldBe LocalDate(2020, 2, 3)
      sum.amount shouldBe 300.0
      sum.type shouldBe BalanceType.PROJECTED
    }
  }
  describe("subtraction") {
    it("different types") {
      val balance1 = Balance(100, LocalDate(2020, 2, 3), BalanceType.CONFIRMED)
      val balance2 = Balance(200, LocalDate(2020, 1, 3), BalanceType.PROJECTED)
      val sum = Balance.subtract(balance1, balance2)
      sum.date shouldBe LocalDate(2020, 2, 3)
      sum.amount shouldBe -100.0
      sum.type shouldBe BalanceType.PROJECTED
    }
  }

  describe("comparison") {
    it("less than") {
      val balance1 = Balance(100, LocalDate(2020, 2, 3), BalanceType.CONFIRMED)
      val balance2 = Balance(200, LocalDate(2020, 1, 3), BalanceType.PROJECTED)
      balance2 shouldBeLessThan balance1
    }
    it("greater than") {
      val balance1 = Balance(100, LocalDate(2020, 2, 3), BalanceType.CONFIRMED)
      val balance2 = Balance(200, LocalDate(2020, 1, 3), BalanceType.PROJECTED)
      balance1 shouldBeGreaterThan balance2
    }

    it("equal") {
      val balance1 = Balance(100, LocalDate(2020, 2, 3), BalanceType.CONFIRMED)
      val balance2 = Balance(100, LocalDate(2020, 2, 3), BalanceType.CONFIRMED)
      balance1 shouldBeEqual balance2
    }
  }

  describe("conversions") {
    it("toString") {
      val balance = Balance(10000, LocalDate(2020, 2, 3), BalanceType.CONFIRMED)
      balance.toString() shouldBe "Balance { amount: 100.00, date: 2020-02-03, type: CONFIRMED }"
    }
  }
})