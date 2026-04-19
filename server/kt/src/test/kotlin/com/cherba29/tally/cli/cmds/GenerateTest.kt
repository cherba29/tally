package com.cherba29.tally.cli.cmds

import com.github.ajalt.clikt.testing.test
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class GenerateTest: DescribeSpec({
  describe("parameter validation") {
    it("account required") {
      val command = Generate()
      val result = command.test()
      result.stderr shouldContain "missing argument <account>"
      result.stdout shouldBe ""
      result.statusCode shouldBe 1
      command.commandName shouldBe "generate"
    }

    it("start-month required") {
      val command = Generate()
      val result = command.test("test-account")
      result.stderr shouldContain "missing option --start-month"
      result.stdout shouldBe ""
      result.statusCode shouldBe 1
    }
  }

  describe("runs") {
    it("successfully") {
      val command = Generate()

      val result = command.test(
        listOf("test-account", "--start-month=Apr2026")
      )
      result.stderr shouldBe ""
      result.stdout shouldBe "Generating balances for test-account starting from Apr2026\n"
      result.statusCode shouldBe 0
    }
  }
})