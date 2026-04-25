package com.cherba29.tally.cli.cmds

import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.parse
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

    it("needs tally path") {
      val command = Report()
      val result = command.test(listOf("test-account", "--start-month=Apr2026", "--end-month=May2026"))
      result.stderr shouldContain "missing option --tally-path"
      result.stdout shouldBe ""
      result.statusCode shouldBe 1
    }

    it("uses env var for tally path") {
      val envvars = mapOf("TALLY_PATH" to "/tmp/tally")
      val command = Report().context {
        readEnvvar = { envvars[it] }
      }
      command.parse(listOf("test-account", "--start-month=Apr2026", "--end-month=May2026"))
      command.tallyPath shouldBe "/tmp/tally"
      command.account shouldBe "test-account"
      command.startMonth shouldBe "Apr2026"
      command.endMonth shouldBe "May2026"
    }

  }
  describe("runs") {
    it("successfully") {
      val command = Report()

      val result = command.test(
        listOf("test-account", "--start-month=Apr2026", "--end-month=May2026", "--tally-path=/tmp/tally")
      )
      result.stderr shouldBe ""
      result.stdout shouldBe "Executing report for test-account from Apr2026 to May2026 for /tmp/tally\n"
      result.statusCode shouldBe 0
    }
  }
})