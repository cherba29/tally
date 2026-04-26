package com.cherba29.tally.cli.cmds

import com.cherba29.tally.core.MonthName.APR
import com.cherba29.tally.core.MonthName.MAY
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

class TransactionsTest: DescribeSpec({
  describe("parameter validation") {
    it("needs tally path") {
      val command = Transactions()
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
      val command = Transactions().context {
        readEnvvar = { envvars[it] }
      }
      command.parse(listOf("test-account", "--start-month=Apr2026", "--end-month=May2026"))
      command.tallyPath shouldBe tallyPath
      command.account shouldBe "test-account"
      command.startMonth shouldBe APR / 2026
      command.endMonth shouldBe MAY / 2026
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

      val command = Transactions()

      val result = command.test(
        listOf("test-account", "--start-month=Apr2026", "--end-month=May2026", "--tally-path=$tallyPath")
      )
      result.stderr shouldBe ""
      result.stdout shouldBe "Date,Amount,From,To,Description\n"
      result.statusCode shouldBe 0
    }
  }
})
