package com.cherba29.tally.core

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate


class MonthTest : DescribeSpec({
  describe("Creation") {
    it("basic") {
      val month = Month(2019, 11)
      month.year shouldBe 2019
      month.month shouldBe 11
    }

    it("bad month too big") {
      val exception = shouldThrow<IllegalArgumentException> { Month(2019, 12) }
      exception.message shouldBe "Invalid value for month 12"
    }

    it("bad month too negative") {
      val exception = shouldThrow<IllegalArgumentException> { Month(2019, -1) }
      exception.message shouldBe "Invalid value for month -1"
    }
  }

  describe("Ordering") {
    it("next month") {
      val month = Month(2020, 1)
      val nextMonth = month.next()
      nextMonth.year shouldBe 2020
      nextMonth.month shouldBe 2
    }

    it("next month becomes next year") {
      val month = Month(2019, 11)
      val nextMonth = month.next()
      nextMonth.year shouldBe 2020
      nextMonth.month shouldBe 0
    }

    it("prev month") {
      val month = Month(2020, 2)
      val prevMonth = month.previous()
      prevMonth.year shouldBe 2020
      prevMonth.month shouldBe 1
    }

    it("prev month becomes prev year") {
      val month = Month(2020, 0)
      val prevMonth = month.previous()
      prevMonth.year shouldBe 2019
      prevMonth.month shouldBe 11
    }

    it("is less - different year") {
      val monthA = Month(2020, 2)
      val monthB = Month(2019, 11)
      (monthB < monthA) shouldBe true
      (monthA < monthB) shouldBe false
    }

    it("is less - same year") {
      val monthA = Month(2019, 11)
      val monthB = Month(2019, 2)
      (monthB < monthA) shouldBe true
      (monthA < monthB) shouldBe false
    }

    it("is less - equal") {
      val monthA = Month(2019, 11)
      val monthB = Month(2019, 11)
      (monthB < monthA) shouldBe false
      (monthA < monthB) shouldBe false
    }

    it("compareTo - different year") {
      val monthA = Month(2020, 2)
      val monthB = Month(2019, 11)
      (monthB.compareTo(monthA)) shouldBeLessThan 0
      (monthA.compareTo(monthB)) shouldBeGreaterThan 0
    }

    it("compareTo - same year") {
      val monthA = Month(2019, 11)
      val monthB = Month(2019, 2)
      monthB.compareTo(monthA) shouldBeLessThan 0
      monthA.compareTo(monthB) shouldBeGreaterThan 0
    }

    it("compareTo - equal") {
      val monthA = Month(2019, 11)
      val monthB = Month(2019, 11)
      monthB.compareTo(monthA) shouldBe 0
      monthA.compareTo(monthB) shouldBe 0
    }

    it("isBetween - before") {
      val monthA = Month(2019, 10)
      val monthB = Month(2022, 10)
      (Month(2018, 11) in monthA..monthB) shouldBe false
      (Month(2019, 9) in monthA..monthB) shouldBe false
      (Month(2019, 10) in monthA..monthB) shouldBe true
    }

    it("isBetween - after") {
      val monthA = Month(2019, 10)
      val monthB = Month(2022, 10)
      (Month(2022, 10) in monthA..monthB) shouldBe true
      (Month(2022, 11) in monthA..monthB) shouldBe false
      (Month(2023, 1) in monthA..monthB) shouldBe false
    }
  }

  describe("distance") {
    it("same") {
      val monthA = Month(2019, 11)
      val monthB = Month(2019, 11)
      (monthB - monthA) shouldBe 0
      (monthA - monthB) shouldBe 0
    }

    it("one month apart") {
      val monthA = Month(2019, 11)
      val monthB = Month(2020, 0)
      (monthB - monthA) shouldBe 1
      (monthA - monthB) shouldBe -1
    }

    it("one year apart") {
      val monthA = Month(2019, 11)
      val monthB = Month(2020, 11)
      (monthB - monthA) shouldBe 12
      (monthA - monthB) shouldBe -12
    }

    it("increment") {
      var monthA = Month(2019, 11)
      val monthB = monthA
      monthA++
      (monthB - monthA) shouldBe -1
      monthA shouldBe Month(2020, 0)
    }

  }

  describe("Naming") {
    it("toString") {
      val month = Month(2019, 11)
      month.toString() shouldBe "Dec2019"
    }

    it("fromString") {
      val month = Month.fromString("Dec2019")
      month.year shouldBe 2019
      month.month shouldBe 11
    }

    it("fromString incomplete") {
      val exception = shouldThrow<java.lang.IllegalArgumentException> { Month.fromString("Dec") }
      exception.message shouldBe "Cant get month from small string 'Dec'"
    }

    it("fromString bad name") {
      val exception = shouldThrow<java.lang.IllegalArgumentException> { Month.fromString("Sec2020") }
      exception.message shouldBe "Bad month name 'Sec' for 'Sec2020', valid names [Jan, Feb, Mar, Apr, May, Jun, Jul, Aug, Sep, Oct, Nov, Dec]"
    }

    it("fromString bad year") {
      val exception = shouldThrow<java.lang.IllegalArgumentException> { Month.fromString("Sep202A") }
      exception.message shouldBe "Cant get year from 'Sep202A'"
    }
  }

  describe("toDate") {
    it("first month") {
      val date = LocalDate(2020, 1, 1)
      val month = Month.fromDate(date)
      month.toDate() shouldBe date
    }

    it("last month") {
      val date = LocalDate(2020, 12, 31)
      val month = Month.fromDate(date)
      month.toDate() shouldBe LocalDate(date.year, date.month, 1)
    }
  }

  describe("fromDate") {
    it("first month") {
      val month = Month.fromDate(LocalDate(2020, 1, 1))
      month.year shouldBe 2020
      month.month shouldBe 0
    }

    it("last month") {
      val month = Month.fromDate(LocalDate(2020, 12, 31))
      month.year shouldBe 2020
      month.month shouldBe 11
    }
  }

  describe("rangeTo") {
    it("empty") {
      val start = Month(2019, 10)
      (start..start.previous()).toList() shouldBe listOf()
    }

    it("is inclusive") {
      val start = Month(2019, 10)
      (start..start.previous()).toList() shouldBe listOf()
    }

    it("contains") {
      val start = Month(2019, 10)
      val dec = start.next()
      val jan = dec.next()
      val end = Month(2020, 1)
      (start in start..end) shouldBe true
      (dec in start..end) shouldBe true
      (jan in start..end) shouldBe true
      (end in start..end) shouldBe true
    }

    it("produces a list") {
      val start = Month(2019, 10)
      val dec = start.next()
      val jan = dec.next()
      val end = Month(2020, 1)
      (start..end).toList() shouldBe listOf(start, dec, jan, end)
    }

    it("produces a list with step 2") {
      val start = Month(2019, 10)
      val dec = start.next()
      val jan = dec.next()
      val end = Month(2020, 1)
      (start..end step 2).toList() shouldBe listOf(start, jan)
    }

    it("downTo") {
      val start = Month(2019, 10)
      val dec = start.next()
      val jan = dec.next()
      val end = Month(2020, 1)
      val months = mutableListOf<Month>()
      for (m in end downTo start) {
        months.add(m)
      }
      months shouldBe listOf(end, jan, dec, start)
    }

    it("downTo with step 2") {
      val start = Month(2019, 10)
      val dec = start.next()
      val end = Month(2020, 1)
      val months = mutableListOf<Month>()
      for (m in end downTo start step 2) {
        months.add(m)
      }
      months shouldBe listOf(end, dec)
    }

    it("fails with negative  step") {
      val start = Month(2019, 10)
      val end = Month(2020, 1)

      val exception = shouldThrow<IllegalArgumentException> { start..end step -1  }
      exception.message shouldBe "Month step should be positive, but got '-1'"
    }

    it("fails with negative reverse step") {
      val start = Month(2019, 10)
      val end = Month(2020, 1)

      val exception = shouldThrow<IllegalArgumentException> { end downTo start step -1  }
      exception.message shouldBe "Month step should be positive, but got '-1'"
    }
  }
})
