package com.cherba29.tally.data

import com.cherba29.tally.core.MonthName.MAR
import com.cherba29.tally.core.MonthName.OCT
import com.cherba29.tally.statement.TransactionStatement
import com.cherba29.tally.utils.root
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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

  describe("recurring index caching and async reload") {
    fun checkingYaml(
      octBalance: String,
      extraNovTransfer: Boolean = false
    ): String {
      val base =
        """
        name: checking
        path: [internal]
        owner: [john]
        opened_on: Aug2026
        balances:
          - { grp: Aug2026, date: 2026-08-01, camt: 2000.00 }
          - { grp: Sep2026, date: 2026-09-01, camt: 1925.00 }
          - { grp: Oct2026, date: 2026-10-01, camt: $octBalance }
          - { grp: Nov2026, date: 2026-11-01, camt: 1775.00 }
        transfers_to:
          expenses:
            Aug2026:
              - { date: 2026-08-18, camt: 75.00, desc: "Fiber internet" }
            Sep2026:
              - { date: 2026-09-18, camt: 75.00, desc: "Fiber internet" }
            Oct2026:
              - { date: 2026-10-18, camt: 75.00, desc: "Fiber internet" }
        """.trimIndent()
      if (!extraNovTransfer) return base
      val extra =
        """
        Nov2026:
          - { date: 2026-11-18, camt: 75.00, desc: "Fiber internet" }
        """.trimIndent().prependIndent("    ")
      return "$base\n$extra"
    }

    val expensesYaml =
      """
      name: expenses
      path: [external]
      owner: [john]
      opened_on: Aug2026
      """.trimIndent()

    it("reuses recurring index on balance-only edits and recomputes when transfers change") {
      val folder = tempdir("tally-cache-", keepOnFailure = false).toPath()
      val checkingFile = (folder / "checking.yaml").createFile()
      val expensesFile = (folder / "expenses.yaml").createFile()
      checkingFile.writeText(checkingYaml(octBalance = "1850.00"))
      expensesFile.writeText(expensesYaml)

      val processedBudget = ProcessedBudget()
      processedBudget.addFile(folder, checkingFile.relativeTo(folder))
      processedBudget.addFile(folder, expensesFile.relativeTo(folder))
      processedBudget.reProcess()

      val budget1 = processedBudget.budget.shouldNotBeNull()
      budget1.transfersFingerprint.shouldNotBeNull()
      val group1 = budget1.recurringIndex.groups.single()
      group1.occurrenceCount shouldBe 3

      // Balance-only edit should reuse the exact same recurring Deferred without recomputing.
      checkingFile.writeText(checkingYaml(octBalance = "1900.00"))
      processedBudget.addFile(folder, checkingFile.relativeTo(folder))
      processedBudget.reProcess()

      val budget2 = processedBudget.budget.shouldNotBeNull()
      budget2.transfersFingerprint shouldBe budget1.transfersFingerprint
      budget2.recurringIndexDeferred shouldBeSameInstanceAs budget1.recurringIndexDeferred
      val checkingNode = budget2.tree[listOf("john", "internal", "checking")]!!
      val octStmt = budget2.nodeToStatement[checkingNode]!![OCT / 2026] as TransactionStatement
      octStmt.startBalance?.amount shouldBe 190000L

      // Adding a transfer should change the fingerprint and recompute recurring groups.
      checkingFile.writeText(checkingYaml(octBalance = "1900.00", extraNovTransfer = true))
      processedBudget.addFile(folder, checkingFile.relativeTo(folder))
      processedBudget.reProcess()

      val budget3 = processedBudget.budget.shouldNotBeNull()
      budget3.transfersFingerprint shouldNotBe budget2.transfersFingerprint
      budget3.recurringIndexDeferred shouldNotBeSameInstanceAs budget2.recurringIndexDeferred
      val group3 = budget3.recurringIndex.groups.single()
      group3.occurrenceCount shouldBe 4
    }

    it("computes recurring index asynchronously when asyncScope is provided") {
      val folder = tempdir("tally-async-", keepOnFailure = false).toPath()
      val checkingFile = (folder / "checking.yaml").createFile()
      val expensesFile = (folder / "expenses.yaml").createFile()
      checkingFile.writeText(checkingYaml(octBalance = "1850.00"))
      expensesFile.writeText(expensesYaml)

      val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
      try {
        val processedBudget = ProcessedBudget(asyncScope = scope)
        processedBudget.addFile(folder, checkingFile.relativeTo(folder))
        processedBudget.addFile(folder, expensesFile.relativeTo(folder))
        processedBudget.reProcess()

        val budget1 = processedBudget.budget.shouldNotBeNull()
        val index1 = budget1.awaitRecurringIndex()
        index1.groups.single().occurrenceCount shouldBe 3

        // Balance-only update reuses the completed async Deferred immediately.
        checkingFile.writeText(checkingYaml(octBalance = "1860.00"))
        processedBudget.addFile(folder, checkingFile.relativeTo(folder))
        processedBudget.reProcess()

        val budget2 = processedBudget.budget.shouldNotBeNull()
        budget2.recurringIndexDeferred shouldBeSameInstanceAs budget1.recurringIndexDeferred
        val index2 = budget2.awaitRecurringIndex()
        index2.groups.single().occurrenceCount shouldBe 3

        processedBudget.removeAll()
        processedBudget.budget shouldBe null
      } finally {
        scope.cancel()
      }
    }
  }
})
