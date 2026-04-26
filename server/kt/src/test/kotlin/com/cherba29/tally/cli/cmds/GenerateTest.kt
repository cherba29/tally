package com.cherba29.tally.cli.cmds

import com.cherba29.tally.core.MonthName.MAR
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.testing.test
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.writeText

class GenerateTest : DescribeSpec({
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

    it("needs tally path") {
      val command = Generate()
      val result = command.test(listOf("test-account", "--start-month=Apr2026"))
      result.stderr shouldContain "missing option --tally-path"
      result.stdout shouldBe ""
      result.statusCode shouldBe 1
    }

    it("uses env var for tally path") {
      val tallyPath = tempdir("tally-", keepOnFailure = false).toPath()
      (tallyPath / "file2.yaml").createFile().writeText(
        """
        name: test-account
        owner: [someone]
        path: [external]
        opened_on: Mar2019
        balances:
          - { grp: Mar2019, date: 2019-03-01, camt: 100.00 }
        """.trimIndent()
      )

      val envvars = mapOf("TALLY_PATH" to tallyPath.toString())
      val command = Generate().context {
        readEnvvar = { envvars[it] }
      }
      command.parse(listOf("test-account", "--start-month=Mar2019"))
      command.tallyPath shouldBe tallyPath
      command.account shouldBe "test-account"
      command.startMonth shouldBe MAR / 2019
    }
  }

  describe("runs") {
    it("successfully") {
      val tallyPath = tempdir("tally-", keepOnFailure = false).toPath()
      (tallyPath / "file2.yaml").createFile().writeText(
        """
        name: test-account
        owner: [someone]
        path: [external]
        opened_on: Mar2019
        balances:
          - { grp: Mar2019, date: 2019-03-01, camt: 100.00 }
        """.trimIndent()
      )

      val command = Generate()

      val result = command.test(
        listOf("test-account", "--start-month=Mar2019", "--tally-path=$tallyPath")
      )
      result.stderr shouldBe ""
      result.stdout shouldBe "Generating balances for test-account starting from Mar2019 for $tallyPath\n" +
          "  - { grp: Mar2019, date: 2019-03-01, camt:  100.00 }\n"
      result.statusCode shouldBe 0
    }
  }
})