package com.cherba29.tally.cli.cmds

import com.github.ajalt.clikt.testing.test
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Paths
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.writeText

class RecurringTest : DescribeSpec({
  describe("parameter validation") {
    it("needs tally path") {
      val result = Recurring().test()
      result.statusCode shouldBe 1
      result.stderr shouldContain "missing option --tally-path"
      result.stdout shouldBe ""
    }
  }

  describe("runs") {
    it("prints empty message when no recurring groups exist") {
      val tallyPath = tempdir("tally-rec-empty-", keepOnFailure = false).toPath()
      (tallyPath / "checking.yaml").createFile().writeText(
        """
        name: checking
        owner: [alex]
        path: [internal]
        opened_on: Mar2026
        balances:
          - { grp: Mar2026, date: 2026-03-01, camt: 100.00 }
        """.trimIndent()
      )

      val result = Recurring().test(listOf("--tally-path=$tallyPath"))
      result.statusCode shouldBe 0
      result.stderr shouldBe ""
      result.stdout shouldBe "No recurring transaction groups found.\n"
    }

    it("lists recurring groups with summary and verbose occurrences on sample-data") {
      val sampleData = Paths.get("../../sample-data").normalize()

      val summaryResult = Recurring().test(listOf("--tally-path=$sampleData"))
      summaryResult.statusCode shouldBe 0
      summaryResult.stderr shouldBe ""
      summaryResult.stdout shouldContain "Monthly apartment rent"
      summaryResult.stdout shouldContain "MONTHLY (~day 2)"
      summaryResult.stdout shouldContain "Fixed 1850.00"
      summaryResult.stdout shouldContain "Electric & gas utility"
      summaryResult.stdout shouldContain "Pay off Nov2026 statement"
      summaryResult.stdout shouldContain "Acme Corp bi-monthly paycheck"
      summaryResult.stdout shouldNotContain "Autumn jacket"

      val filteredAccount = Recurring().test(listOf("--tally-path=$sampleData", "--account=credit-card", "--verbose"))
      filteredAccount.statusCode shouldBe 0
      filteredAccount.stdout shouldContain "Streaming & cloud subscriptions"
      filteredAccount.stdout shouldContain "2026-11-22"
      filteredAccount.stdout shouldNotContain "Monthly apartment rent"

      val filteredOwnerMiss = Recurring().test(listOf("--tally-path=$sampleData", "--owner=nonexistent"))
      filteredOwnerMiss.statusCode shouldBe 0
      filteredOwnerMiss.stdout shouldBe "No recurring transaction groups found.\n"
    }
  }
})
