package com.cherba29.tally.core

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe

class DayInMonthTest : DescribeSpec({
  describe("fromString") {
    it("parses null") {
      DayInMonth.fromString(null) shouldBe DayInMonth.Unknown()
    }
    it("fails on empty") {
      val error = shouldThrow<IllegalArgumentException> {
        DayInMonth.fromString("")
      }
      error.message shouldBe "Cant parse '' as DayInMonth"
    }
    it("parses absolute") {
      DayInMonth.fromString("5") shouldBe
          DayInMonth.AbsoluteDay(5)
    }
    it("parses relative first") {
      DayInMonth.fromString("first_of_month") shouldBe
          DayInMonth.RelativeDay(DayInMonth.Relative.FIRST_OF_MONTH)
    }
    it("parses relative last") {
      DayInMonth.fromString("last_of_month") shouldBe
          DayInMonth.RelativeDay(DayInMonth.Relative.LAST_OF_MONTH)
    }
  }
  describe("compareTo") {
    it("absolute greater than relative") {
      DayInMonth.AbsoluteDay(5)
        .compareTo(DayInMonth.RelativeDay(DayInMonth.Relative.FIRST_OF_MONTH)) shouldBe 1
      DayInMonth.RelativeDay(DayInMonth.Relative.FIRST_OF_MONTH)
        .compareTo(DayInMonth.AbsoluteDay(5)) shouldBe -1
    }
    it("absolute greater than unknown") {
      DayInMonth.AbsoluteDay(5).compareTo(DayInMonth.Unknown()) shouldBeGreaterThan 0
      DayInMonth.Unknown().compareTo(DayInMonth.AbsoluteDay(5)) shouldBeLessThan 0
    }
    it("relative greater than unknown") {
      DayInMonth.RelativeDay(DayInMonth.Relative.FIRST_OF_MONTH).compareTo(DayInMonth.Unknown()) shouldBeGreaterThan 0
      DayInMonth.Unknown().compareTo(DayInMonth.RelativeDay(DayInMonth.Relative.FIRST_OF_MONTH)) shouldBeLessThan 0
    }
    it("unknowns are same") {
      DayInMonth.Unknown().compareTo(DayInMonth.Unknown()) shouldBe 0
    }
  }
  describe("equals") {
    it("any unknown is same") {
      DayInMonth.Unknown() shouldBe DayInMonth.Unknown()
    }
  }
})
