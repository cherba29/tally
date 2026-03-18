package com.cherba29.tally.core

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import com.cherba29.tally.core.MonthName.APR
import com.cherba29.tally.core.MonthName.AUG
import com.cherba29.tally.core.MonthName.DEC
import com.cherba29.tally.core.MonthName.FEB
import com.cherba29.tally.core.MonthName.JAN
import com.cherba29.tally.core.MonthName.JUL
import com.cherba29.tally.core.MonthName.JUN
import com.cherba29.tally.core.MonthName.MAR
import com.cherba29.tally.core.MonthName.MAY
import com.cherba29.tally.core.MonthName.NOV
import com.cherba29.tally.core.MonthName.OCT
import com.cherba29.tally.core.MonthName.SEP

class MonthNameTest : DescribeSpec({
  describe("Creation") {
    it("jan") {
      val month = JAN / 2020
      month.year shouldBe 2020
      month.month shouldBe 0
    }

    it("feb") {
      val month = FEB / 2020
      month.year shouldBe 2020
      month.month shouldBe 1
    }

    it("mar") {
      val month = MAR / 2020
      month.year shouldBe 2020
      month.month shouldBe 2
    }

    it("apr") {
      val month = APR / 2020
      month.year shouldBe 2020
      month.month shouldBe 3
    }

    it("may") {
      val month = MAY / 2020
      month.year shouldBe 2020
      month.month shouldBe 4
    }

    it("jun") {
      val month = JUN / 2020
      month.year shouldBe 2020
      month.month shouldBe 5
    }

    it("jul") {
      val month = JUL / 2020
      month.year shouldBe 2020
      month.month shouldBe 6
    }

    it("aug") {
      val month = AUG / 2020
      month.year shouldBe 2020
      month.month shouldBe 7
    }

    it("sep") {
      val month = SEP / 2020
      month.year shouldBe 2020
      month.month shouldBe 8
    }

    it("oct") {
      val month = OCT / 2020
      month.year shouldBe 2020
      month.month shouldBe 9
    }

    it("nov") {
      val month = NOV / 2020
      month.year shouldBe 2020
      month.month shouldBe 10
    }

    it("dec") {
      val month = DEC / 2020
      month.year shouldBe 2020
      month.month shouldBe 11
    }
  }
})