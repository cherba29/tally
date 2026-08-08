package com.cherba29.tally.utils

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe

class NumberFormatingTest : DescribeSpec({
  describe("Double.asRounded") {
    it("rounds up double to zero decimals") {
      1.5.asRounded(0) shouldBe (2.0f plusOrMinus 0.01f)
    }

    it("rounds down double to zero decimals") {
      1.4.asRounded(0) shouldBe (1.0f plusOrMinus 0.01f)
    }

    it("round double to two decimals") {
      1.5789.asRounded(2) shouldBe (1.58f plusOrMinus 0.001f)
    }
  }
  describe("Double.asRoundedPercent") {
    it("rounds up double to zero decimals") {
      1.5.asRoundedPercent(0) shouldBe (150.0f plusOrMinus 0.01f)
    }

    it("rounds down double to zero decimals") {
      0.014.asRoundedPercent(0) shouldBe (1.0f plusOrMinus 0.01f)
    }

    it("round double to two decimals") {
      0.015789.asRoundedPercent(2) shouldBe (1.58f plusOrMinus 0.001f)
    }
  }
  describe("Long.asAmount") {
    it("formats 0") {
      0L.asAmount() shouldBe "0.00"
    }
    it("formats 1") {
      1L.asAmount() shouldBe "0.01"
    }
    it("formats 100") {
      100L.asAmount() shouldBe "1.00"
    }
    it("formats negative") {
      (-12345L).asAmount() shouldBe "-123.45"
    }
  }
})
