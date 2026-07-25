package com.cherba29.tally.cli.cmds

import com.github.ajalt.clikt.testing.test
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.writeText

class CsvToTransfersTest : DescribeSpec({
  describe("parameter validation") {
    it("csf file required") {
      val command = CsvToTransfers()
      val result = command.test()
      result.stderr shouldContain "missing option --csv-file"
      result.stdout shouldBe ""
      result.statusCode shouldBe 1
      command.commandName shouldBe "csv-to-transfers"
    }
  }
  describe("runs") {
    it("fails on file empty") {
      val csvPath = (tempdir("tally-", keepOnFailure = false).toPath()
       / "transactions.csv").createFile()

      csvPath.writeText(
        """
        """.trimIndent()
      )

      val command = CsvToTransfers()

      val result = command.test(
        listOf("--csv-file=$csvPath")
      )
      result.stderr shouldContain "Unknown type of csv with fields []"
      result.stdout shouldBe "Converting csv '$csvPath' to transfers\n"
      result.statusCode shouldBe 1
    }

    it("converts citi costco") {
      val csvPath = (tempdir("tally-", keepOnFailure = false).toPath()
          / "transactions.csv").createFile()

      csvPath.writeText(
        """
          Status,Date,Description,Debit,Credit,Member Name
          Cleared,07/17/2025,"COSTCO WHSE#5 JOHNSVILLECA",2.12,,JOHN
        """.trimIndent()
      )

      val command = CsvToTransfers()

      val result = command.test(
        listOf("--csv-file=$csvPath")
      )
      result.stderr shouldBe ""
      result.stdout shouldBe """
        Converting csv '$csvPath' to transfers
        {Status=Cleared, Date=07/17/2025, Description=COSTCO WHSE#5 JOHNSVILLECA, Debit=2.12, Credit=, Member Name=JOHN}
        Detected source: costco
        Detected month: Jul2025
            - { grp: Jul2025, date: 2025-07-17, camt:    2.12, desc: "COSTCO WHSE#5 JOHNSVILLECA" }

      """.trimIndent()
      result.statusCode shouldBe 0
    }

    it("converts citi double") {
      val csvPath = (tempdir("tally-", keepOnFailure = false).toPath()
          / "transactions.csv").createFile()

      csvPath.writeText(
        """
          Status,Date,Description,Debit,Credit
          Cleared,07/17/2025,"WALMART#4 JOHNSVILLECA",2.12,
        """.trimIndent()
      )

      val command = CsvToTransfers()

      val result = command.test(
        listOf("--csv-file=$csvPath")
      )
      result.stderr shouldBe ""
      result.stdout shouldBe """
        Converting csv '$csvPath' to transfers
        {Status=Cleared, Date=07/17/2025, Description=WALMART#4 JOHNSVILLECA, Debit=2.12, Credit=}
        Detected source: citi-double
        Detected month: Jul2025
            - { grp: Jul2025, date: 2025-07-17, camt:    2.12, desc: "WALMART#4 JOHNSVILLECA" }

      """.trimIndent()
      result.statusCode shouldBe 0
    }

    it("converts chace amazon") {
      val csvPath = (tempdir("tally-", keepOnFailure = false).toPath()
          / "transactions.csv").createFile()

      csvPath.writeText(
        """
          Transaction Date,Post Date,Description,Category,Type,Amount,Memo
          07/12/2026,07/12/2026,Payment Thank You - Web,,Payment,181.32,
          07/09/2026,07/10/2026,Amazon.com*BM16E52V3,Shopping,Sale,-48.99,
        """.trimIndent()
      )

      val command = CsvToTransfers()

      val result = command.test(
        listOf("--csv-file=$csvPath")
      )
      result.stderr shouldBe ""
      result.stdout shouldBe """
        Converting csv '$csvPath' to transfers
        {Transaction Date=07/12/2026, Post Date=07/12/2026, Description=Payment Thank You - Web, Category=, Type=Payment, Amount=181.32, Memo=}
        {Transaction Date=07/09/2026, Post Date=07/10/2026, Description=Amazon.com*BM16E52V3, Category=Shopping, Type=Sale, Amount=-48.99, Memo=}
        Detected source: chase_amazon
        Detected month: Jul2026
            - { grp: Jul2026, date: 2026-07-09, camt:   48.99, desc: "Amazon.com*BM16E52V3" }

      """.trimIndent()
      result.statusCode shouldBe 0
    }

  }
})
