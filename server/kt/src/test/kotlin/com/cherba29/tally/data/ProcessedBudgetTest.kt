package com.cherba29.tally.data

import com.cherba29.tally.core.MonthName.MAR
import com.cherba29.tally.utils.root
import com.cherba29.tally.statement.TransactionStatement
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.relativeTo
import kotlin.io.path.writeText

class ProcessedBudgetTest : DescribeSpec({
  describe("basic") {
    it("empty") {
      val processedBudget = ProcessedBudget()
      processedBudget.budget shouldBe null
    }

    it("reprocess empty throws") {
      val processedBudget = ProcessedBudget()
      val exception = shouldThrow<IllegalArgumentException> { processedBudget.reProcess() }
      exception.message shouldBe "Budget must have at least one month."
    }

    it("single") {
      val folder = tempdir("tally-", keepOnFailure = false).toPath()
      val filePath = (folder / "file2.yaml").createFile()
      filePath.writeText(
        """
        name: test-account
        path: [external]
        owner: [john]
        opened_on: Mar2026
        """.trimIndent()
      )

      val processedBudget = ProcessedBudget()
      processedBudget.addFile(
        folder,
        relativeFilePath = filePath.relativeTo(folder)
      )
      processedBudget.reProcess()
      val budget = processedBudget.budget!!
      budget.tree shouldBe root(Profile()) {
        branch("john", Profile()) {
          branch("external", Profile(isExternal = true)) {
            leaf("test-account", Profile(isExternal = true))
          }
        }
      }
      val accountNode = budget.tree[listOf("john", "external", "test-account")]
      budget.nodeToStatement.keys shouldBe setOf(
        accountNode,
        budget.tree[listOf("john", "external")],
        budget.tree[listOf("john")]
      )

      val transactionStatement = budget.nodeToStatement[accountNode]?.get(MAR / 2026)!! as TransactionStatement
      transactionStatement.monthRange shouldBe MAR / 2026..MAR / 2026
      transactionStatement.transactions.isEmpty() shouldBe true
    }
  }
})
