package com.cherba29.tally.cli

import com.github.ajalt.clikt.testing.test
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class CliTest : DescribeSpec({
  describe("basic") {
    it("no parameters") {
      val command = Cli()
      val result = command.test()
      result.statusCode shouldBe 0
      result.stderr shouldBe ""
      result.stdout shouldBe "Verbose mode is off\n"
      command.commandName shouldBe "cli"
    }

    it("verbose mode") {
      val command = Cli()
      val result = command.test("--verbose")
      result.statusCode shouldBe 0
      result.stderr shouldBe ""
      result.stdout shouldBe "Verbose mode is on\n"
      command.commandName shouldBe "cli"
    }
  }
})