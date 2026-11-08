package com.cherba29.tally.cli.cmds

import com.cherba29.tally.NotFoundException
import com.cherba29.tally.core.MonthName.MAR
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.testing.test
import com.github.ajalt.mordant.rendering.AnsiLevel
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.writeText

class TransfersSummaryTest : DescribeSpec({
  describe("parameter validation") {
    it("account required") {
      val command = TransfersSummary()
      val result = command.test()
      result.stderr shouldContain "missing argument <accountpath>"
      result.stdout shouldBe ""
      result.statusCode shouldBe 1
      command.commandName shouldBe "transfers-summary"
    }
    it("start-month required") {
      val command = TransfersSummary()
      val result = command.test("test-account")
      result.stderr shouldContain "missing option --start-month"
      result.stdout shouldBe ""
      result.statusCode shouldBe 1
    }

    it("end-month required") {
      val command = TransfersSummary()
      val result = command.test("test-account --start-month=Apr2026")
      result.stderr shouldContain "missing option --end-month"
      result.stdout shouldBe ""
      result.statusCode shouldBe 1
    }

    it("needs tally path") {
      val command = TransfersSummary()
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
      val command = TransfersSummary().context {
        readEnvvar = { envvars[it] }
      }
      val error = shouldThrow<NotFoundException> {
        command.parse(listOf("someone/external/test-account2", "--start-month=Mar2026", "--end-month=Mar2026"))
      }
      command.tallyPath shouldBe tallyPath
      command.accountPath shouldBe "someone/external/test-account2"
      command.startMonth shouldBe MAR / 2026
      command.endMonth shouldBe MAR / 2026
      error.message shouldBe "'someone/external/test-account2' not found."
    }

    it("fails on invalid start-month format") {
      val command = TransfersSummary()
      val result = command.test("test-account --start-month=InvalidMonth --end-month=Mar2026 --tally-path=/tmp")
      result.stderr shouldContain "invalid value for --start-month"
      result.statusCode shouldBe 1
    }

    it("fails on invalid end-month format") {
      val command = TransfersSummary()
      val result = command.test("test-account --start-month=Mar2026 --end-month=InvalidMonth --tally-path=/tmp")
      result.stderr shouldContain "invalid value for --end-month"
      result.statusCode shouldBe 1
    }

    it("fails when tally path does not exist") {
      val command = TransfersSummary()
      val result = command.test(listOf("test-account", "--start-month=Mar2026", "--end-month=Mar2026", "--tally-path=/path/does/not/exist"))
      result.stderr shouldContain "path \"/path/does/not/exist\" does not exist"
      result.statusCode shouldBe 1
    }

    it("fails on invalid width") {
      val command = TransfersSummary()
      val result = command.test("test-account --start-month=Mar2026 --end-month=Mar2026 --tally-path=/tmp --width=not-a-number")
      result.stderr shouldContain "invalid value for --width"
      result.statusCode shouldBe 1
    }

    it("fails when account path does not exist in budget") {
      val tallyPath = tempdir("tally-", keepOnFailure = false).toPath()
      (tallyPath / "file.yaml").createFile().writeText(
        """
        name: test-account
        owner: [someone]
        path: [external]
        opened_on: Mar2019
        balances:
          - { grp: Mar2019, date: 2019-03-01, camt: 100.00 }
        """.trimIndent()
      )
      val command = TransfersSummary()
      val error = shouldThrow<NotFoundException> {
        command.parse(listOf("someone/external/non-existent", "--start-month=Mar2019", "--end-month=Mar2019", "--tally-path=$tallyPath"))
      }
      error.message shouldBe "'someone/external/non-existent' not found."
    }

    it("handles startMonth after endMonth gracefully producing empty table") {
      val tallyPath = tempdir("tally-", keepOnFailure = false).toPath()
      (tallyPath / "file.yaml").createFile().writeText(
        """
        name: test-account
        owner: [someone]
        path: [external]
        opened_on: Mar2019
        balances:
          - { grp: Mar2019, date: 2019-03-01, camt: 100.00 }
          - { grp: Apr2019, date: 2019-04-01, camt: 100.00 }
        """.trimIndent()
      )
      val command = TransfersSummary()
      val result = command.test(
        listOf(
          "someone/external/test-account",
          "--start-month=Apr2019",
          "--end-month=Mar2019",
          "--tally-path=$tallyPath",
          "--width=150"
        ),
        width = 150,
        outputInteractive = true,
        ansiLevel = AnsiLevel.NONE
      )
      result.statusCode shouldBe 0
      result.stderr shouldBe ""
      val lines = result.stdout.lines().filter { it.isNotBlank() }
      lines.size shouldBe 1 // Only header row rendered when table has no data rows
      lines[0] shouldContain "Idx"
      lines[0] shouldContain "Unaccounted"
    }
  }
  describe("runs") {
    it("successfully for single month") {
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

      val command = TransfersSummary()

      val result = command.test(
        listOf(
          "someone/external/test-account",
          "--start-month=Mar2019",
          "--end-month=Mar2019",
          "--tally-path=$tallyPath",
          "--width=150"
        ),
        width = 150,
        outputInteractive = true,
        ansiLevel = AnsiLevel.NONE
      )
      result.stderr shouldBe ""
      result.statusCode shouldBe 0

      val lines = result.stdout.lines().filter { it.isNotBlank() }
      lines.size shouldBe 3

      // Header row
      lines[0] shouldBe " Idx │   Month │ Internal │ External │ Change │ Tot Int │ Int % │ Int A % │ Tot Ext │ Ext % │ Ext A % │ Total │ Tot A % │ Unaccounted "
      // Border divider
      lines[1] shouldBe "─────┼─────────┼──────────┼──────────┼────────┼─────────┼───────┼─────────┼─────────┼───────┼─────────┼───────┼─────────┼─────────────"
      // Data row
      lines[2] shouldBe "   1 │ Mar2019 │     0.00 │     0.00 │   0.00 │    0.00 │   0.0 │     0.0 │    0.00 │   0.0 │     0.0 │  0.00 │     0.0 │      100.00 "
    }

    it("successfully for multi-month with internal and external transfers") {
      val tallyPath = tempdir("tally-multi-", keepOnFailure = false).toPath()
      (tallyPath / "checking.yaml").createFile().writeText(
        """
        name: checking
        owner: [john]
        path: [assets]
        opened_on: Jan2020
        balances:
          - { grp: Jan2020, date: 2020-01-01, camt: 1000.00 }
          - { grp: Feb2020, date: 2020-02-01, camt: 900.00 }
          - { grp: Mar2020, date: 2020-03-01, camt: 800.00 }
          - { grp: Apr2020, date: 2020-04-01, camt: 1200.00 }
        transfers_to:
          savings:
            - { grp: Jan2020, date: 2020-01-15, camt: 100.00, desc: "Deposit to savings" }
            - { grp: Feb2020, date: 2020-02-15, camt: 150.00, desc: "Deposit to savings" }
          groceries:
            - { grp: Feb2020, date: 2020-02-20, camt: 50.00, desc: "Food shopping" }
            - { grp: Mar2020, date: 2020-03-10, camt: 75.00, desc: "Food shopping" }
        """.trimIndent()
      )
      (tallyPath / "savings.yaml").createFile().writeText(
        """
        name: savings
        owner: [john]
        path: [assets]
        opened_on: Jan2020
        balances:
          - { grp: Jan2020, date: 2020-01-01, camt: 500.00 }
          - { grp: Feb2020, date: 2020-02-01, camt: 600.00 }
          - { grp: Mar2020, date: 2020-03-01, camt: 750.00 }
          - { grp: Apr2020, date: 2020-04-01, camt: 750.00 }
        """.trimIndent()
      )
      (tallyPath / "groceries.yaml").createFile().writeText(
        """
        name: groceries
        owner: [john]
        path: [external]
        opened_on: Jan2020
        balances:
          - { grp: Jan2020, date: 2020-01-01, camt: 0.00 }
          - { grp: Feb2020, date: 2020-02-01, camt: 0.00 }
          - { grp: Mar2020, date: 2020-03-01, camt: 50.00 }
          - { grp: Apr2020, date: 2020-04-01, camt: 125.00 }
        """.trimIndent()
      )

      val command = TransfersSummary()
      val result = command.test(
        listOf(
          "john/assets/checking",
          "--start-month=Jan2020",
          "--end-month=Mar2020",
          "--tally-path=$tallyPath",
          "--width=150"
        ),
        width = 150,
        outputInteractive = true,
        ansiLevel = AnsiLevel.NONE
      )
      result.statusCode shouldBe 0
      result.stderr shouldBe ""

      val lines = result.stdout.lines().filter { it.isNotBlank() }
      lines.size shouldBe 7 // Header, divider, and 3 data rows separated by dividers

      // Header row
      lines[0] shouldBe " Idx │   Month │ Internal │ External │  Change │ Tot Int │ Int % │ Int A % │ Tot Ext │ Ext % │  Ext A % │   Total │   Tot A % │ Unaccounted "
      lines[1] shouldBe "─────┼─────────┼──────────┼──────────┼─────────┼─────────┼───────┼─────────┼─────────┼───────┼──────────┼─────────┼───────────┼─────────────"

      // Row 1 (Mar2020)
      lines[2] shouldBe "   1 │ Mar2020 │     0.00 │   -75.00 │  -75.00 │ -250.00 │  66.7 │ 1039.06 │ -125.00 │  33.3 │   6300.0 │ -375.00 │ 277991.44 │     1100.00 "
      lines[3] shouldBe lines[1]

      // Row 2 (Feb2020)
      lines[4] shouldBe "   2 │ Feb2020 │  -150.00 │   -50.00 │ -200.00 │ -250.00 │  83.3 │  791.61 │  -50.00 │  16.7 │ 409500.0 │ -300.00 │  5.3144E7 │     1000.00 "
      lines[5] shouldBe lines[1]

      // Row 3 (Jan2020)
      lines[6] shouldBe "   3 │ Jan2020 │  -100.00 │     0.00 │ -100.00 │ -100.00 │ 100.0 │     0.0 │    0.00 │  -0.0 │      0.0 │ -100.00 │       0.0 │     1000.00 "
    }

    it("successfully for summary account path") {
      val tallyPath = tempdir("tally-summary-", keepOnFailure = false).toPath()
      (tallyPath / "checking.yaml").createFile().writeText(
        """
        name: checking
        owner: [john]
        path: [assets]
        opened_on: Jan2020
        balances:
          - { grp: Jan2020, date: 2020-01-01, camt: 1000.00 }
          - { grp: Feb2020, date: 2020-02-01, camt: 900.00 }
          - { grp: Mar2020, date: 2020-03-01, camt: 800.00 }
          - { grp: Apr2020, date: 2020-04-01, camt: 1200.00 }
        transfers_to:
          savings:
            - { grp: Jan2020, date: 2020-01-15, camt: 100.00, desc: "Deposit to savings" }
            - { grp: Feb2020, date: 2020-02-15, camt: 150.00, desc: "Deposit to savings" }
          groceries:
            - { grp: Feb2020, date: 2020-02-20, camt: 50.00, desc: "Food shopping" }
            - { grp: Mar2020, date: 2020-03-10, camt: 75.00, desc: "Food shopping" }
        """.trimIndent()
      )
      (tallyPath / "savings.yaml").createFile().writeText(
        """
        name: savings
        owner: [john]
        path: [assets]
        opened_on: Jan2020
        balances:
          - { grp: Jan2020, date: 2020-01-01, camt: 500.00 }
          - { grp: Feb2020, date: 2020-02-01, camt: 600.00 }
          - { grp: Mar2020, date: 2020-03-01, camt: 750.00 }
          - { grp: Apr2020, date: 2020-04-01, camt: 750.00 }
        """.trimIndent()
      )
      (tallyPath / "groceries.yaml").createFile().writeText(
        """
        name: groceries
        owner: [john]
        path: [external]
        opened_on: Jan2020
        balances:
          - { grp: Jan2020, date: 2020-01-01, camt: 0.00 }
          - { grp: Feb2020, date: 2020-02-01, camt: 0.00 }
          - { grp: Mar2020, date: 2020-03-01, camt: 50.00 }
          - { grp: Apr2020, date: 2020-04-01, camt: 125.00 }
        """.trimIndent()
      )

      val summaryCommand = TransfersSummary()
      val summaryResult = summaryCommand.test(
        listOf(
          "john/assets",
          "--start-month=Jan2020",
          "--end-month=Mar2020",
          "--tally-path=$tallyPath",
          "--width=150"
        ),
        width = 150,
        outputInteractive = true,
        ansiLevel = AnsiLevel.NONE
      )
      summaryResult.statusCode shouldBe 0
      summaryResult.stderr shouldBe ""

      val lines = summaryResult.stdout.lines().filter { it.isNotBlank() }
      lines.size shouldBe 7

      // Header row
      lines[0] shouldBe " Idx │   Month │ Internal │ External │ Change │ Tot Int │ Int % │  Int A % │ Tot Ext │ Ext % │ Ext A % │   Total │  Tot A % │ Unaccounted "
      lines[1] shouldBe "─────┼─────────┼──────────┼──────────┼────────┼─────────┼───────┼──────────┼─────────┼───────┼─────────┼─────────┼──────────┼─────────────"

      // Row 1 (Mar2020) - internal transfers net to 0.00
      lines[2] shouldBe "   1 │ Mar2020 │     0.00 │   -75.00 │ -75.00 │    0.00 │  -0.0 │ 24314.06 │ -125.00 │ 100.0 │     0.0 │ -125.00 │ 24314.06 │     1600.00 "
      lines[3] shouldBe lines[1]

      // Row 2 (Feb2020)
      lines[4] shouldBe "   2 │ Feb2020 │     0.00 │   -50.00 │ -50.00 │    0.00 │  -0.0 │      0.0 │  -50.00 │ 100.0 │     0.0 │  -50.00 │      0.0 │     1500.00 "
      lines[5] shouldBe lines[1]

      // Row 3 (Jan2020)
      lines[6] shouldBe "   3 │ Jan2020 │     0.00 │     0.00 │   0.00 │    0.00 │   0.0 │      0.0 │    0.00 │   0.0 │     0.0 │    0.00 │      0.0 │     1500.00 "
    }

    it("successfully filters sub-range of months") {
      val tallyPath = tempdir("tally-subrange-", keepOnFailure = false).toPath()
      (tallyPath / "checking.yaml").createFile().writeText(
        """
        name: checking
        owner: [john]
        path: [assets]
        opened_on: Jan2020
        balances:
          - { grp: Jan2020, date: 2020-01-01, camt: 1000.00 }
          - { grp: Feb2020, date: 2020-02-01, camt: 900.00 }
          - { grp: Mar2020, date: 2020-03-01, camt: 800.00 }
          - { grp: Apr2020, date: 2020-04-01, camt: 1200.00 }
        transfers_to:
          savings:
            - { grp: Jan2020, date: 2020-01-15, camt: 100.00, desc: "Deposit to savings" }
            - { grp: Feb2020, date: 2020-02-15, camt: 150.00, desc: "Deposit to savings" }
          groceries:
            - { grp: Feb2020, date: 2020-02-20, camt: 50.00, desc: "Food shopping" }
            - { grp: Mar2020, date: 2020-03-10, camt: 75.00, desc: "Food shopping" }
        """.trimIndent()
      )
      (tallyPath / "savings.yaml").createFile().writeText(
        """
        name: savings
        owner: [john]
        path: [assets]
        opened_on: Jan2020
        balances:
          - { grp: Jan2020, date: 2020-01-01, camt: 500.00 }
          - { grp: Feb2020, date: 2020-02-01, camt: 600.00 }
          - { grp: Mar2020, date: 2020-03-01, camt: 750.00 }
          - { grp: Apr2020, date: 2020-04-01, camt: 750.00 }
        """.trimIndent()
      )
      (tallyPath / "groceries.yaml").createFile().writeText(
        """
        name: groceries
        owner: [john]
        path: [external]
        opened_on: Jan2020
        balances:
          - { grp: Jan2020, date: 2020-01-01, camt: 0.00 }
          - { grp: Feb2020, date: 2020-02-01, camt: 0.00 }
          - { grp: Mar2020, date: 2020-03-01, camt: 50.00 }
          - { grp: Apr2020, date: 2020-04-01, camt: 125.00 }
        """.trimIndent()
      )

      val command = TransfersSummary()
      val result = command.test(
        listOf(
          "john/assets/checking",
          "--start-month=Feb2020",
          "--end-month=Mar2020",
          "--tally-path=$tallyPath",
          "--width=150"
        ),
        width = 150,
        outputInteractive = true,
        ansiLevel = AnsiLevel.NONE
      )
      result.statusCode shouldBe 0
      result.stderr shouldBe ""

      val lines = result.stdout.lines().filter { it.isNotBlank() }
      lines.size shouldBe 5 // Header, divider, and 2 data rows with 1 divider

      // Row 1 is Mar2020 (Idx 1)
      lines[2] shouldContain "1 │ Mar2020"
      lines[2] shouldContain "-75.00"

      // Row 2 is Feb2020 (Idx 2)
      lines[4] shouldContain "2 │ Feb2020"
      lines[4] shouldContain "-150.00"
      lines[4] shouldContain "-50.00"
      lines[4] shouldContain "-200.00"

      // Jan2020 is excluded
      result.stdout shouldNotContain "Jan2020"
    }

    it("verifies borderless outer layout and column formatting") {
      val tallyPath = tempdir("tally-layout-", keepOnFailure = false).toPath()
      (tallyPath / "file.yaml").createFile().writeText(
        """
        name: test-account
        owner: [someone]
        path: [external]
        opened_on: Mar2019
        balances:
          - { grp: Mar2019, date: 2019-03-01, camt: 100.00 }
        """.trimIndent()
      )

      val command = TransfersSummary()
      val result = command.test(
        listOf(
          "someone/external/test-account",
          "--start-month=Mar2019",
          "--end-month=Mar2019",
          "--tally-path=$tallyPath",
          "--width=150"
        ),
        width = 150,
        outputInteractive = true,
        ansiLevel = AnsiLevel.NONE
      )
      result.statusCode shouldBe 0

      val lines = result.stdout.lines().filter { it.isNotBlank() }

      // Verify no outer borders: no line starts or ends with a vertical border character
      for (line in lines) {
        line.startsWith("│") shouldBe false
        line.startsWith("|") shouldBe false
        line.endsWith("│") shouldBe false
        line.endsWith("|") shouldBe false
      }

      // Verify 14 column headers in exact expected order
      val expectedHeaders = listOf(
        "Idx", "Month", "Internal", "External", "Change",
        "Tot Int", "Int %", "Int A %", "Tot Ext", "Ext %",
        "Ext A %", "Total", "Tot A %", "Unaccounted"
      )
      val actualHeaders = lines[0].split("│").map { it.trim() }
      actualHeaders shouldBe expectedHeaders

      // Verify data row column count and alignment
      val dataCells = lines[2].split("│").map { it.trim() }
      dataCells.size shouldBe 14
      dataCells[0] shouldBe "1"
      dataCells[1] shouldBe "Mar2019"
      dataCells[2] shouldBe "0.00"
      dataCells[13] shouldBe "100.00"
    }

    it("defaults to 120 width when not specified") {
      val tallyPath = tempdir("tally-width-def-", keepOnFailure = false).toPath()
      (tallyPath / "file.yaml").createFile().writeText(
        """
        name: test-account
        owner: [someone]
        path: [external]
        opened_on: Mar2019
        balances:
          - { grp: Mar2019, date: 2019-03-01, camt: 100.00 }
        """.trimIndent()
      )

      val command = TransfersSummary()
      val result = command.test(
        listOf("someone/external/test-account", "--start-month=Mar2019", "--end-month=Mar2019", "--tally-path=$tallyPath"),
        width = 150,
        outputInteractive = true,
        ansiLevel = AnsiLevel.NONE
      )
      result.statusCode shouldBe 0
      command.width shouldBe 120

      val lines = result.stdout.lines().filter { it.isNotBlank() }
      lines.size shouldBe 3
      for (line in lines) {
        (line.length <= 120) shouldBe true
      }
    }

    it("supports custom width via --width and -w") {
      val tallyPath = tempdir("tally-width-custom-", keepOnFailure = false).toPath()
      (tallyPath / "file.yaml").createFile().writeText(
        """
        name: test-account
        owner: [someone]
        path: [external]
        opened_on: Mar2019
        balances:
          - { grp: Mar2019, date: 2019-03-01, camt: 100.00 }
        """.trimIndent()
      )

      val command1 = TransfersSummary()
      val result1 = command1.test(
        listOf(
          "someone/external/test-account",
          "--start-month=Mar2019",
          "--end-month=Mar2019",
          "--tally-path=$tallyPath",
          "--width=90"
        ),
        width = 150,
        outputInteractive = true,
        ansiLevel = AnsiLevel.NONE
      )
      result1.statusCode shouldBe 0
      command1.width shouldBe 90
      for (line in result1.stdout.lines().filter { it.isNotBlank() }) {
        (line.length <= 90) shouldBe true
      }

      val command2 = TransfersSummary()
      val result2 = command2.test(
        listOf(
          "someone/external/test-account",
          "--start-month=Mar2019",
          "--end-month=Mar2019",
          "--tally-path=$tallyPath",
          "-w",
          "80"
        ),
        width = 150,
        outputInteractive = true,
        ansiLevel = AnsiLevel.NONE
      )
      result2.statusCode shouldBe 0
      command2.width shouldBe 80
      for (line in result2.stdout.lines().filter { it.isNotBlank() }) {
        (line.length <= 80) shouldBe true
      }
    }
  }
})
