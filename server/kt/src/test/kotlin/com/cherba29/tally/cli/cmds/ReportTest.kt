package com.cherba29.tally.cli.cmds

import com.github.ajalt.clikt.testing.test
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class ReportTest: DescribeSpec({
  describe("parameter validation") {
    it("account required") {
      val command = Report()
      val result = command.test()
      result.stderr shouldContain "missing argument <account>"
      result.stdout shouldBe ""
      result.statusCode shouldBe 1
      command.commandName shouldBe "report"
    }

    it("start-month required") {
      val command = Report()
      val result = command.test("test-account")
      result.stderr shouldContain "missing option --start-month"
      result.stdout shouldBe ""
      result.statusCode shouldBe 1
    }

    it("end-month required") {
      val command = Report()
      val result = command.test("test-account --start-month=Apr2026")
      result.stderr shouldContain "missing option --end-month"
      result.stdout shouldBe ""
      result.statusCode shouldBe 1
    }
  }
  describe("runs") {
    it("successfully") {
      val command = Report()
      val result = command.test("test-account --start-month=Apr2026 --end-month=May2026")
      result.stderr shouldContain ""
      result.stdout shouldBe "Executing report for test-account from Apr2026 to May2026\n"
      result.statusCode shouldBe 0
    }
  }
})