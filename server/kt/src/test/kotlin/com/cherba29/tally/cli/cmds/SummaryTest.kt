package com.cherba29.tally.cli.cmds

import com.cherba29.tally.NotFoundException
import com.cherba29.tally.core.MonthName.MAR
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.testing.test
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.writeText

class SummaryTest : DescribeSpec({
  describe("parameter validation") {
    it("account required") {
      val command = Summary()
      val result = command.test()
      result.stderr shouldContain "missing argument <account>"
      result.stdout shouldBe ""
      result.statusCode shouldBe 1
      command.commandName shouldBe "summary"
    }
    it("start-month required") {
      val command = Summary()
      val result = command.test("test-account")
      result.stderr shouldContain "missing option --start-month"
      result.stdout shouldBe ""
      result.statusCode shouldBe 1
    }

    it("end-month required") {
      val command = Summary()
      val result = command.test("test-account --start-month=Apr2026")
      result.stderr shouldContain "missing option --end-month"
      result.stdout shouldBe ""
      result.statusCode shouldBe 1
    }

    it("needs tally path") {
      val command = Summary()
      val result = command.test(listOf("test-account", "--start-month=Apr2026", "--end-month=May2026"))
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
      val command = Summary().context {
        readEnvvar = { envvars[it] }
      }
      val error = shouldThrow<NotFoundException> {
        command.parse(listOf("someone/external", "--start-month=Mar2026", "--end-month=Mar2026"))
      }
      command.tallyPath shouldBe tallyPath
      command.account shouldBe "someone/external"
      command.startMonth shouldBe MAR / 2026
      command.endMonth shouldBe MAR / 2026
      error.message shouldBe "Summary 'someone/external' for months [Mar2026, Mar2026] not found."
    }

    it("computes multi-month summary excluding closed accounts") {
      val tallyPath = tempdir("tally-", keepOnFailure = false).toPath()
      (tallyPath / "active.yaml").createFile().writeText(
        """
        name: active-account
        owner: [someone]
        path: [internal]
        opened_on: Mar2019
        balances:
          - { grp: Mar2019, date: 2019-03-01, camt: 100.00 }
          - { grp: Apr2019, date: 2019-04-01, camt: 150.00 }
          - { grp: May2019, date: 2019-05-01, camt: 200.00 }
        """.trimIndent()
      )
      (tallyPath / "closed.yaml").createFile().writeText(
        """
        name: closed-account
        owner: [someone]
        path: [internal]
        opened_on: Mar2019
        closed_on: Mar2019
        balances:
          - { grp: Mar2019, date: 2019-03-01, camt: 500.00 }
        """.trimIndent()
      )

      val result = Summary().test(
        listOf("someone/internal", "--start-month=Apr2019", "--end-month=Apr2019"),
        envvars = mapOf("TALLY_FILES" to tallyPath.toString())
      )
      result.statusCode shouldBe 0
      result.stdout shouldContain "name: someone/internal"
      result.stdout shouldContain "month: Apr2019..Apr2019"
      result.stdout shouldContain "change: 50.00"
    }
  }
})
