package com.cherba29.tally.data

import com.cherba29.tally.core.MonthName.MAR
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.relativeTo
import kotlin.io.path.writeText

class ProcessedBudgetTest : DescribeSpec({
  describe("basic") {
    it("empty") {
      val processedBudget = ProcessedBudget()
      processedBudget.budget shouldBe null
      processedBudget.summaryNameMonthMap.isEmpty shouldBe true
      processedBudget.accountToMonthToTransactionStatement.isEmpty() shouldBe true
    }

    it("reprocess empty throws") {
      val processedBudget = ProcessedBudget()
      val exception = shouldThrow<IllegalArgumentException> { processedBudget.reProcess() }
      exception.message shouldBe "Budget must have at least one month."
    }

    it("single") {
      val folder = tempdir("tally-", keepOnFailure = false).toPath()
      val filePath = (folder / "file2.yaml").createFile()
      filePath.writeText("""
        name: test-account
        owner: [john]
        opened_on: Mar2026
       """.trimIndent())

      val processedBudget = ProcessedBudget()
      processedBudget.addFile(
        folder,
        relativeFilePath = filePath.relativeTo(folder)
      )
      processedBudget.reProcess()
      processedBudget.budget shouldNotBe null
      processedBudget.summaryNameMonthMap.isEmpty shouldBe true
      processedBudget.accountToMonthToTransactionStatement.size shouldBe 1

      val transactionStatement = processedBudget.accountToMonthToTransactionStatement["test-account"]?.get(MAR / 2026)!!
      transactionStatement.account.name shouldBe "test-account"
      transactionStatement.account.openedOn shouldBe MAR / 2026
      transactionStatement.account.owners shouldBe listOf("john")

      transactionStatement.transactions.isEmpty() shouldBe true
    }
  }
})
