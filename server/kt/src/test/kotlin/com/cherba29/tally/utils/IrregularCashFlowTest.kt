package com.cherba29.tally.utils

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.math.pow

class IrregularCashFlowTest : DescribeSpec({
  describe("basic") {
    it("empty") {
      val cashFlow = IrregularCashFlow()
      cashFlow.effectiveRateOfReturnOnContributions() shouldBe 0.0
      cashFlow.effectiveRateOfReturnOnGains() shouldBe 0.0
      cashFlow.effectiveRateOfReturn() shouldBe 0.0
    }
    it("single contribution") {
      val cashFlow = IrregularCashFlow()
      cashFlow.add(100, 0)
      cashFlow.effectiveRateOfReturnOnContributions() shouldBe 0.0
      cashFlow.effectiveRateOfReturnOnGains() shouldBe 0.0
      cashFlow.effectiveRateOfReturn() shouldBe 0.0
    }
    it("single gain") {
      val cashFlow = IrregularCashFlow()
      cashFlow.add(0, 100)
      cashFlow.effectiveRateOfReturnOnContributions() shouldBe 0.0
      cashFlow.effectiveRateOfReturnOnGains() shouldBe 0.0
      cashFlow.effectiveRateOfReturn() shouldBe 0.0
    }
    it("one time gain and contribution") {
      val cashFlow = IrregularCashFlow()
      cashFlow.add(100, 50)
      cashFlow.effectiveRateOfReturnOnContributions() shouldBe (0.0 plusOrMinus 0.1)
      cashFlow.effectiveRateOfReturnOnGains() shouldBe (0.0 plusOrMinus 0.1)
      cashFlow.effectiveRateOfReturn() shouldBe (0.0 plusOrMinus 0.1)
    }
  }
  describe("multi entry") {
    it("initial with contribution") {
      val cashFlow = IrregularCashFlow()
      cashFlow.add(100, 0)
      cashFlow.add(10, 0)
      // 110/110 = 1.
      cashFlow.effectiveRateOfReturnOnContributions() shouldBe 0.0
      // 110 / 100 = 1.1 over 1 periods 1.1^(12/1)-1 = 2.138.
      cashFlow.effectiveRateOfReturnOnGains() shouldBe (2.138 plusOrMinus 0.001)
      cashFlow.effectiveRateOfReturn() shouldBe (2.138 plusOrMinus 0.001)
    }
    it("initial with gain") {
      val cashFlow = IrregularCashFlow()
      cashFlow.add(100, 0)
      cashFlow.add(0, 20)
      // 120 / 100 = 1.2 over 12 periods 1.2^(12/1)-1 = 7.916.
      cashFlow.effectiveRateOfReturnOnContributions() shouldBe (7.916 plusOrMinus 0.001)
      // 120/120 = 1
      cashFlow.effectiveRateOfReturnOnGains() shouldBe 0.0
      // Same as on contributions since gains is zero.
      cashFlow.effectiveRateOfReturn() shouldBe (7.916 plusOrMinus 0.001)
    }
    it("initial with contribution and gain") {
      val cashFlow = IrregularCashFlow()
      cashFlow.add(100, 0)
      cashFlow.add(10, 20)
      // (130/110)^(12/1)-1 = 6.423
      cashFlow.effectiveRateOfReturnOnContributions() shouldBe (6.423 plusOrMinus 0.001)
      // (130/120)^(12/1)-1 = 1.613
      cashFlow.effectiveRateOfReturnOnGains() shouldBe (1.613 plusOrMinus 0.001)
      // (130/100)^(12/1)-1 = 1.613
      cashFlow.effectiveRateOfReturn() shouldBe (22.298 plusOrMinus 0.001)
    }
  }
  describe("long") {
    it("just contributions") {
      val cashFlow = IrregularCashFlow()
      cashFlow.add(100, 0)
      cashFlow.add(100, 0)
      cashFlow.add(100, 0)
      cashFlow.add(100, 0)
      cashFlow.effectiveRateOfReturnOnContributions() shouldBe (0.0 plusOrMinus 0.001)
      cashFlow.effectiveRateOfReturnOnGains() shouldBe (255.0 plusOrMinus 0.001)
      cashFlow.effectiveRateOfReturn() shouldBe (255.0 plusOrMinus 0.001)
    }

    it("contribution plus coupon") {
      val cashFlow = IrregularCashFlow()
      // 10k investment with effective annual rate of 6%
      // 1 + EAR = (1 + r_monthly)^12
      // r_monthly = (1 + EAR)^(1/12) - 1, for 6% it is 0.0048676.
      var current = 1000000.0
      cashFlow.add(1000000, 0)
      repeat(12) {
        val diff = current * 0.0048676
        cashFlow.add(0, diff.toLong())
        current += diff
      }
      current shouldBe (1060000.0 plusOrMinus 1.0)
      cashFlow.effectiveRateOfReturnOnContributions() shouldBe (0.060 plusOrMinus 0.001)
      cashFlow.effectiveRateOfReturnOnGains() shouldBe (0.0 plusOrMinus 0.001)
    }

    it("almost infinity") {
      val cashFlow = IrregularCashFlow()
      cashFlow.add(163615, 9085)
      cashFlow.add(98885, -23857)
      cashFlow.effectiveRateOfReturnOnContributions() shouldBe (-0.365 plusOrMinus 0.001)
      cashFlow.effectiveRateOfReturnOnGains() shouldBeGreaterThan 10.0.pow(18)
    }
  }
  describe("weighted total age") {
    it("empty is zero") {
      val cashFlow = IrregularCashFlow()
      cashFlow.weightedAverageAmountAge() shouldBe (0.0 plusOrMinus 0.001)
    }

    it("just made contribution is one month old") {
      val cashFlow = IrregularCashFlow()
      cashFlow.add(100, 0)
      cashFlow.weightedAverageAmountAge() shouldBe (1.0 plusOrMinus 0.001)
    }

    it("two contributions split the age") {
      val cashFlow = IrregularCashFlow()
      cashFlow.add(100, 0)
      cashFlow.add(100, 0)
      cashFlow.weightedAverageAmountAge() shouldBe (1.5 plusOrMinus 0.001)
    }

    it("gains counted the same") {
      val cashFlow = IrregularCashFlow()
      cashFlow.add(100, 0)
      cashFlow.add(0, 100)
      cashFlow.weightedAverageAmountAge() shouldBe (1.5 plusOrMinus 0.001)
    }

    it("remaining retain age") {
      val cashFlow = IrregularCashFlow()
      cashFlow.add(100, 0)
      cashFlow.add(0, -10)
      cashFlow.add(0, -20)
      cashFlow.weightedAverageAmountAge() shouldBe (3.0 plusOrMinus 0.001)
    }
  }
  describe("weighted contribution age") {
    it("empty contributions is zero") {
      val cashFlow = IrregularCashFlow()
      cashFlow.weightedContributionsAge() shouldBe (0.0 plusOrMinus 0.001)
    }

    it("contribution is one month old") {
      val cashFlow = IrregularCashFlow()
      cashFlow.add(100, 0)
      cashFlow.weightedContributionsAge() shouldBe (1.0 plusOrMinus 0.001)
    }

    it("two contributions split the age") {
      val cashFlow = IrregularCashFlow()
      cashFlow.add(100, 0)
      cashFlow.add(100, 0)
      cashFlow.weightedContributionsAge() shouldBe (1.5 plusOrMinus 0.001)
    }

    it("gains are not counted") {
      val cashFlow = IrregularCashFlow()
      cashFlow.add(100, 0)
      cashFlow.add(0, 100)
      cashFlow.weightedContributionsAge() shouldBe (2.0 plusOrMinus 0.001)
    }

    it("remaining retain age") {
      val cashFlow = IrregularCashFlow()
      cashFlow.add(100, 0)
      cashFlow.add(-10, 0)
      cashFlow.add(-20, 0)
      cashFlow.weightedContributionsAge() shouldBe (3.0 plusOrMinus 0.001)
    }
  }
  describe("weighted gains age") {
    it("empty gains is zero") {
      val cashFlow = IrregularCashFlow()
      cashFlow.weightedGainsAge() shouldBe (0.0 plusOrMinus 0.001)
    }

    it("gains is one month old") {
      val cashFlow = IrregularCashFlow()
      cashFlow.add(0, 100)
      cashFlow.weightedGainsAge() shouldBe (1.0 plusOrMinus 0.001)
    }

    it("two gains split the age") {
      val cashFlow = IrregularCashFlow()
      cashFlow.add(0, 100)
      cashFlow.add(0, 100)
      cashFlow.weightedGainsAge() shouldBe (1.5 plusOrMinus 0.001)
    }

    it("gains are not counted") {
      val cashFlow = IrregularCashFlow()
      cashFlow.add(0, 100)
      cashFlow.add(100, 0)
      cashFlow.weightedGainsAge() shouldBe (2.0 plusOrMinus 0.001)
    }

    it("remaining retain age") {
      val cashFlow = IrregularCashFlow()
      cashFlow.add(0,100)
      cashFlow.add(0, -10)
      cashFlow.add(0, -20)
      cashFlow.weightedGainsAge() shouldBe (3.0 plusOrMinus 0.001)
    }
  }
})
