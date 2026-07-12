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
      result.stderr shouldContain "Cant find 'Date' field"
      result.stdout shouldBe "Converting csv '$csvPath' to transfers\n"
      result.statusCode shouldBe 1
    }

    it("fails on file empty") {
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
            - { grp: Jul2025, date: 2025-07-17, camt:     2.12, desc: "COSTCO WHSE#5 JOHNSVILLECA" }

      """.trimIndent()
      result.statusCode shouldBe 0
    }

  }
})
