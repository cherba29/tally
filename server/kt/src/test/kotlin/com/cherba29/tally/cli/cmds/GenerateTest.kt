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
          "  - { grp: Apr2019, date: 2019-04-01, pamt:  100.00 }\n" +
          "  - { grp: Mar2019, date: 2019-03-01, camt:  100.00 }\n"
      result.statusCode shouldBe 0
    }

    it("account not found") {
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
        listOf("test-account1", "--start-month=Mar2019", "--tally-path=$tallyPath")
      )
      result.stderr shouldContain  "The account test-account1 has no statements."
      result.stdout shouldBe "Generating balances for test-account1 starting from Mar2019 for $tallyPath\n"
      result.statusCode shouldBe 1
    }

    it("start month too large") {
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
        listOf("test-account", "--start-month=Mar2020", "--tally-path=$tallyPath")
      )
      result.stderr shouldContain "Error: The account test-account has no balances or transactions after Apr2019."
      result.stdout shouldBe "Generating balances for test-account starting from Mar2020 for $tallyPath\n"
      result.statusCode shouldBe 1
    }

    it("start month too small") {
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
        listOf("test-account", "--start-month=Mar2018", "--tally-path=$tallyPath")
      )
      result.stderr shouldContain ""
      result.stdout shouldBe "Generating balances for test-account starting from Mar2018 for $tallyPath\n" +
          "  - { grp: Apr2019, date: 2019-04-01, pamt:  100.00 }\n" +
          "  - { grp: Mar2019, date: 2019-03-01, camt:  100.00 }\n" +
          "  - { grp: Feb2019 } # has no balance and had 0 transfers.\n"
      result.statusCode shouldBe 0
    }

    it("no account balances") {
      val tallyPath = tempdir("tally-", keepOnFailure = false).toPath()
      (tallyPath / "file2.yaml").createFile().writeText(
        """
        name: test-account
        owner: [someone]
        path: [external]
        opened_on: Mar2019
        balances:
        """.trimIndent()
      )

      val command = Generate()

      val result = command.test(
        listOf("test-account", "--start-month=Mar2019", "--tally-path=$tallyPath")
      )
      result.stdout shouldBe "Generating balances for test-account starting from Mar2019 for $tallyPath\n"
      result.stderr shouldContain  "Error: Account 'test-account' has no records for any month."
      result.statusCode shouldBe 1
    }

    it("use transfers") {
      val tallyPath = tempdir("tally-", keepOnFailure = false).toPath()
      (tallyPath / "file1.yaml").createFile().writeText(
        """
        name: test-account1
        owner: [someone]
        path: [external]
        opened_on: Mar2019
        balances:
          - { grp: Mar2019, date: 2019-03-01, camt: 100.00 }
        transfers_to:
          test-account2:
            - { grp: Mar2019, date: 2019-03-25, camt: 345.00, desc: "spent" }
        """.trimIndent()
      )
      (tallyPath / "file2.yaml").createFile().writeText(
        """
        name: test-account2
        owner: [someone]
        path: [external]
        opened_on: Mar2019
        balances:
          - { grp: Mar2019, date: 2019-03-01, camt: 100.00 }
        """.trimIndent()
      )

      val command = Generate()

      val result = command.test(
        listOf("test-account1", "--start-month=Mar2019", "--use-transfers", "--tally-path=$tallyPath")
      )
      result.stderr shouldBe ""
      result.stdout shouldBe "Generating balances for test-account1 starting from Mar2019 for $tallyPath\n" +
          "  - { grp: Apr2019, date: 2019-04-01, camt: -245.00 }\n" +
          "  - { grp: Mar2019, date: 2019-03-01, camt:  100.00 }\n"
      result.statusCode shouldBe 0
    }

    it("show transfers") {
      val tallyPath = tempdir("tally-", keepOnFailure = false).toPath()
      (tallyPath / "file1.yaml").createFile().writeText(
        """
        name: test-account1
        owner: [someone]
        path: [external]
        opened_on: Mar2019
        balances:
          - { grp: Mar2019, date: 2019-03-01, camt: 100.00 }
        transfers_to:
          test-account2:
            - { grp: Mar2019, date: 2019-03-25, camt: 345.00, desc: "spent" }
        """.trimIndent()
      )
      (tallyPath / "file2.yaml").createFile().writeText(
        """
        name: test-account2
        owner: [someone]
        path: [external]
        opened_on: Mar2019
        balances:
          - { grp: Mar2019, date: 2019-03-01, camt: 100.00 }
        """.trimIndent()
      )

      val command = Generate()

      val result = command.test(
        listOf("test-account1", "--start-month=Mar2019", "--show-transfers", "--tally-path=$tallyPath")
      )
      result.stderr shouldBe ""
      result.stdout shouldBe "Generating balances for test-account1 starting from Mar2019 for $tallyPath\n" +
          "  - { grp: Apr2019, date: 2019-04-01, pamt: -245.00 }\n" +
          "    Mar2019 test-account1 --> test-account2 Balance { amount: 345.00, date: 2019-03-25, type: CONFIRMED }\n" +
          "  - { grp: Mar2019, date: 2019-03-01, camt:  100.00 }\n"
      result.statusCode shouldBe 0
    }

    it("balance disagreement") {
      val tallyPath = tempdir("tally-", keepOnFailure = false).toPath()
      (tallyPath / "file1.yaml").createFile().writeText(
        """
        name: test-account1
        owner: [someone]
        path: [external]
        opened_on: Mar2019
        balances:
          - { grp: Apr2019, date: 2019-04-01, pamt: 200.00 }
          - { grp: Mar2019, date: 2019-03-01, camt: 100.00 }
        transfers_to:
          test-account2:
            - { grp: Mar2019, date: 2019-03-25, camt: 345.00, desc: "spent" }
        """.trimIndent()
      )
      (tallyPath / "file2.yaml").createFile().writeText(
        """
        name: test-account2
        owner: [someone]
        path: [external]
        opened_on: Mar2019
        balances:
          - { grp: Mar2019, date: 2019-03-01, camt: 100.00 }
        """.trimIndent()
      )

      val command = Generate()

      val result = command.test(
        listOf("test-account1", "--start-month=Mar2019", "--tally-path=$tallyPath")
      )
      result.stderr shouldBe ""
      result.stdout shouldBe "Generating balances for test-account1 starting from Mar2019 for $tallyPath\n" +
          "  - { grp: May2019, date: 2019-05-01, pamt:  200.00 }\n" +
          "  - { grp: Apr2019, date: 2019-04-01, pamt:  200.00 } # predicted -245.00 unaccounted  445.00\n" +
          "  - { grp: Mar2019, date: 2019-03-01, camt:  100.00 }\n"
      result.statusCode shouldBe 0
    }

    it("next month balance date based on transaction") {
      val tallyPath = tempdir("tally-", keepOnFailure = false).toPath()
      (tallyPath / "file1.yaml").createFile().writeText(
        """
        name: test-account1
        owner: [someone]
        path: [external]
        opened_on: Mar2019
        balances:
          - { grp: Mar2019, date: 2019-03-01, camt: 100.00 }
        """.trimIndent()
      )
      (tallyPath / "file2.yaml").createFile().writeText(
        """
        name: test-account2
        owner: [someone]
        path: [external]
        opened_on: Mar2019
        balances:
          - { grp: Mar2019, date: 2019-03-01, camt: 100.00 }
        transfers_to:
          test-account1:
            - { grp: Apr2019, date: 2019-04-03, camt: 100.00, desc: "spent" }
            - { grp: Mar2019, date: 2019-04-02, camt: 345.00, desc: "spent" }
        """.trimIndent()
      )

      val command = Generate()

      val result = command.test(
        listOf("test-account1", "--start-month=Mar2019", "--tally-path=$tallyPath")
      )
      result.stderr shouldBe ""
      result.stdout shouldBe "Generating balances for test-account1 starting from Mar2019 for $tallyPath\n" +
          "  - { grp: May2019, date: 2019-05-02, pamt:  545.00 }\n" +
          "  - { grp: Apr2019, date: 2019-04-02, pamt:  445.00 }\n" +
          "  - { grp: Mar2019, date: 2019-03-01, camt:  100.00 }\n"
      result.statusCode shouldBe 0
    }

    it("with annual flush") {
      val tallyPath = tempdir("tally-", keepOnFailure = false).toPath()
      (tallyPath / "file1.yaml").createFile().writeText(
        """
        name: test-account1
        owner: [someone]
        path: [external]
        opened_on: Dec2019
        balances:
          - { grp: Jan2020, date: 2020-01-01, pamt: 200.00 }
          - { grp: Dec2019, date: 2019-12-01, camt: 100.00 }
        transfers_to:
          test-account2:
            - { grp: Dec2019, date: 2019-12-25, camt: 345.00, desc: "spent" }
        """.trimIndent()
      )
      (tallyPath / "file2.yaml").createFile().writeText(
        """
        name: test-account2
        owner: [someone]
        path: [external]
        opened_on: Dec2019
        balances:
          - { grp: Dec2019, date: 2019-12-01, camt: 100.00 }
        transfers_to:
          test-account1:
            - { grp: Dec2019, date: 2019-12-26, camt: 50.00, desc: "refund" }
        """.trimIndent()
      )

      val command = Generate()

      val result = command.test(
        listOf("test-account1", "--start-month=Dec2019", "--with-annual-flush", "--tally-path=$tallyPath")
      )
      result.stderr shouldBe ""
      result.stdout shouldBe "Generating balances for test-account1 starting from Dec2019 for $tallyPath\n" +
          "  - { grp: Feb2020, date: 2020-02-01, pamt:    0.00 }\n" +
          "  - { grp: Jan2020, date: 2020-01-01, pamt:  200.00 } # predicted -195.00 unaccounted  395.00\n" +
          "  - { grp: Dec2019, date: 2019-12-01, camt:  100.00 }\n" +
          "    - { grp: Jan2020, date: 2020-01-02, camt:  200.00, desc: \"Total for 2019\" }\n"
      result.statusCode shouldBe 0
    }
  }
})