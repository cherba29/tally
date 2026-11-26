package com.cherba29.tally.data

import com.cherba29.tally.core.MonthName.APR
import com.cherba29.tally.core.MonthName.AUG
import com.cherba29.tally.core.MonthName.DEC
import com.cherba29.tally.core.MonthName.FEB
import com.cherba29.tally.core.MonthName.JAN
import com.cherba29.tally.core.MonthName.JUL
import com.cherba29.tally.core.MonthName.JUN
import com.cherba29.tally.core.MonthName.MAR
import com.cherba29.tally.core.MonthName.MAY
import com.cherba29.tally.core.MonthName.NOV
import com.cherba29.tally.core.MonthName.OCT
import com.cherba29.tally.core.MonthName.SEP
import com.cherba29.tally.statement.TransactionStatement
import com.cherba29.tally.utils.root
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.relativeTo
import kotlin.io.path.writeText
import kotlin.time.TimeSource

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

  describe("incremental rebuilds") {
    fun loadFreshFromDisk(
      folder: Path,
      files: List<Path>
    ): Budget {
      val fresh = ProcessedBudget()
      for (file in files) {
        fresh.addFile(folder, file.relativeTo(folder))
      }
      fresh.reProcess()
      return fresh.budget.shouldNotBeNull()
    }

    fun assertEquivalentBudgets(
      actual: Budget,
      expected: Budget
    ) {
      actual.months shouldBe expected.months
      actual.tree shouldBe expected.tree
      actual.transfersFingerprint shouldBe expected.transfersFingerprint
      val actualPaths = actual.nodeToStatement.keys.mapTo(mutableSetOf()) { it.path }
      val expectedPaths = expected.nodeToStatement.keys.mapTo(mutableSetOf()) { it.path }
      actualPaths shouldBe expectedPaths
      for ((expectedNode, expectedMonthMap) in expected.nodeToStatement) {
        val actualNode = actual.tree[expectedNode.path].shouldNotBeNull()
        val actualMonthMap = actual.nodeToStatement[actualNode].shouldNotBeNull()
        actualMonthMap shouldBe expectedMonthMap
      }
      actual.recurringIndex.groups shouldBe expected.recurringIndex.groups
    }

    fun checkingYaml(
      desc: String = "Primary checking",
      octBalanceLine: String = "  - { grp: Oct2026, date: 2026-10-01, camt: 1850.00 }",
      extraBalanceLine: String = "",
      novTransfersBlock: String = ""
    ): String =
      buildString {
        appendLine("name: checking")
        appendLine("desc: \"$desc\"")
        appendLine("path: [internal, bank]")
        appendLine("owner: [john]")
        appendLine("opened_on: Aug2026")
        appendLine("balances:")
        appendLine("  - { grp: Aug2026, date: 2026-08-01, camt: 2000.00 }")
        appendLine("  - { grp: Sep2026, date: 2026-09-01, camt: 1925.00 }")
        if (octBalanceLine.isNotEmpty()) appendLine(octBalanceLine)
        appendLine("  - { grp: Nov2026, date: 2026-11-01, camt: 1775.00 }")
        if (extraBalanceLine.isNotEmpty()) appendLine(extraBalanceLine)
        appendLine("transfers_to:")
        appendLine("  expenses:")
        appendLine("    Aug2026:")
        appendLine("      - { date: 2026-08-18, camt: 75.00, desc: \"Fiber internet\" }")
        appendLine("    Sep2026:")
        appendLine("      - { date: 2026-09-18, camt: 75.00, desc: \"Fiber internet\" }")
        appendLine("    Oct2026:")
        appendLine("      - { date: 2026-10-18, camt: 75.00, desc: \"Fiber internet\" }")
        if (novTransfersBlock.isNotEmpty()) appendLine(novTransfersBlock)
      }

    val savingsYaml =
      """
      name: savings
      path: [internal, bank]
      owner: [john]
      opened_on: Aug2026
      balances:
        - { grp: Aug2026, date: 2026-08-01, camt: 5000.00 }
        - { grp: Sep2026, date: 2026-09-01, camt: 5000.00 }
      """.trimIndent()

    val expensesYaml =
      """
      name: expenses
      path: [external]
      owner: [john]
      opened_on: Aug2026
      """.trimIndent()

    val closedExternalYaml =
      """
      name: old-broker
      path: [external]
      owner: [john]
      opened_on: Aug2026
      closed_on: Sep2026
      balances:
        - { grp: Aug2026, date: 2026-08-01, camt: 100.00 }
        - { grp: Sep2026, date: 2026-09-01, camt: 120.00 }
      """.trimIndent()

    it("matches full rebuild exactly for balance edits and transfer add, edit, and delete") {
      val folder = tempdir("tally-inc-", keepOnFailure = false).toPath()
      val checkingFile = (folder / "checking.yaml").createFile()
      val savingsFile = (folder / "savings.yaml").createFile()
      val expensesFile = (folder / "expenses.yaml").createFile()
      val closedFile = (folder / "old-broker.yaml").createFile()
      val allFiles = listOf(checkingFile, savingsFile, expensesFile, closedFile)

      checkingFile.writeText(checkingYaml())
      savingsFile.writeText(savingsYaml)
      expensesFile.writeText(expensesYaml)
      closedFile.writeText(closedExternalYaml)

      val processed = ProcessedBudget()
      for (file in allFiles) {
        processed.addFile(folder, file.relativeTo(folder))
      }
      processed.reProcess()
      processed.lastRebuildWasIncremental shouldBe false
      val budget0 = processed.budget.shouldNotBeNull()
      val savingsNode = budget0.tree[listOf("john", "internal", "bank", "savings")].shouldNotBeNull()
      val savingsStmts0 = budget0.nodeToStatement[savingsNode].shouldNotBeNull()

      // 1. Balance-only edit on checking.yaml (including changing sign to negative so coverage flips on Sep2026).
      checkingFile.writeText(
        checkingYaml(octBalanceLine = "  - { grp: Oct2026, date: 2026-10-01, camt: -50.00 }")
      )
      processed.addFile(folder, checkingFile.relativeTo(folder))
      processed.reProcess()
      processed.lastRebuildWasIncremental shouldBe true
      val budget1 = processed.budget.shouldNotBeNull()
      budget1.tree shouldBeSameInstanceAs budget0.tree
      budget1.nodeToStatement[savingsNode] shouldBeSameInstanceAs savingsStmts0
      budget1.recurringIndexDeferred shouldBeSameInstanceAs budget0.recurringIndexDeferred
      assertEquivalentBudgets(budget1, loadFreshFromDisk(folder, allFiles))

      // 2. Add a transfer in Nov2026 from checking -> expenses.
      val novTransfer =
        """
        Nov2026:
          - { date: 2026-11-18, camt: 75.00, desc: "Fiber internet" }
        """.trimIndent().prependIndent("    ")
      checkingFile.writeText(
        checkingYaml(
          octBalanceLine = "  - { grp: Oct2026, date: 2026-10-01, camt: -50.00 }",
          novTransfersBlock = novTransfer
        )
      )
      processed.addFile(folder, checkingFile.relativeTo(folder))
      processed.reProcess()
      processed.lastRebuildWasIncremental shouldBe true
      val budget2 = processed.budget.shouldNotBeNull()
      budget2.tree shouldBeSameInstanceAs budget0.tree
      budget2.nodeToStatement[savingsNode] shouldBeSameInstanceAs savingsStmts0
      assertEquivalentBudgets(budget2, loadFreshFromDisk(folder, allFiles))

      // 3. Edit the Nov2026 transfer amount and description.
      val editedNovTransfer =
        """
        Nov2026:
          - { date: 2026-11-19, camt: 80.00, desc: "Fiber internet promo" }
        """.trimIndent().prependIndent("    ")
      checkingFile.writeText(
        checkingYaml(
          octBalanceLine = "  - { grp: Oct2026, date: 2026-10-01, camt: -50.00 }",
          novTransfersBlock = editedNovTransfer
        )
      )
      processed.addFile(folder, checkingFile.relativeTo(folder))
      processed.reProcess()
      processed.lastRebuildWasIncremental shouldBe true
      val budget3 = processed.budget.shouldNotBeNull()
      assertEquivalentBudgets(budget3, loadFreshFromDisk(folder, allFiles))

      // 4. Delete the Nov2026 transfer and remove the Oct2026 balance.
      checkingFile.writeText(checkingYaml(octBalanceLine = ""))
      processed.addFile(folder, checkingFile.relativeTo(folder))
      processed.reProcess()
      processed.lastRebuildWasIncremental shouldBe true
      val budget4 = processed.budget.shouldNotBeNull()
      assertEquivalentBudgets(budget4, loadFreshFromDisk(folder, allFiles))
    }

    it("falls back to full rebuild on metadata, month-range, closed-external, and file structure changes") {
      val folder = tempdir("tally-fallback-", keepOnFailure = false).toPath()
      val checkingFile = (folder / "checking.yaml").createFile()
      val expensesFile = (folder / "expenses.yaml").createFile()
      val closedFile = (folder / "old-broker.yaml").createFile()
      val allFiles = mutableListOf(checkingFile, expensesFile, closedFile)

      checkingFile.writeText(checkingYaml())
      expensesFile.writeText(expensesYaml)
      closedFile.writeText(closedExternalYaml)

      val processed = ProcessedBudget()
      for (file in allFiles) {
        processed.addFile(folder, file.relativeTo(folder))
      }
      processed.reProcess()

      // Metadata change -> full rebuild
      checkingFile.writeText(checkingYaml(desc = "Renamed checking"))
      processed.addFile(folder, checkingFile.relativeTo(folder))
      processed.reProcess()
      processed.lastRebuildWasIncremental shouldBe false
      assertEquivalentBudgets(processed.budget.shouldNotBeNull(), loadFreshFromDisk(folder, allFiles))

      // Month-range expansion (Dec2026) -> full rebuild
      checkingFile.writeText(
        checkingYaml(
          desc = "Renamed checking",
          extraBalanceLine = "  - { grp: Dec2026, date: 2026-12-01, camt: 1700.00 }"
        )
      )
      processed.addFile(folder, checkingFile.relativeTo(folder))
      processed.reProcess()
      processed.lastRebuildWasIncremental shouldBe false
      processed.budget.shouldNotBeNull().months shouldBe (AUG / 2026..DEC / 2026)
      assertEquivalentBudgets(processed.budget.shouldNotBeNull(), loadFreshFromDisk(folder, allFiles))

      // Month-range boundary shrink (removing Dec2026) -> full rebuild
      checkingFile.writeText(checkingYaml(desc = "Renamed checking"))
      processed.addFile(folder, checkingFile.relativeTo(folder))
      processed.reProcess()
      processed.lastRebuildWasIncremental shouldBe false
      processed.budget.shouldNotBeNull().months shouldBe (AUG / 2026..NOV / 2026)
      assertEquivalentBudgets(processed.budget.shouldNotBeNull(), loadFreshFromDisk(folder, allFiles))

      // Closed external account edit -> full rebuild
      closedFile.writeText(
        """
        name: old-broker
        path: [external]
        owner: [john]
        opened_on: Aug2026
        closed_on: Sep2026
        balances:
          - { grp: Aug2026, date: 2026-08-01, camt: 100.00 }
          - { grp: Sep2026, date: 2026-09-01, camt: 150.00 }
        """.trimIndent()
      )
      processed.addFile(folder, closedFile.relativeTo(folder))
      processed.reProcess()
      processed.lastRebuildWasIncremental shouldBe false
      assertEquivalentBudgets(processed.budget.shouldNotBeNull(), loadFreshFromDisk(folder, allFiles))

      // Adding and removing a file -> full rebuild
      val extraFile = (folder / "savings.yaml").createFile()
      extraFile.writeText(savingsYaml)
      allFiles.add(extraFile)
      processed.addFile(folder, extraFile.relativeTo(folder))
      processed.reProcess()
      processed.lastRebuildWasIncremental shouldBe false
      assertEquivalentBudgets(processed.budget.shouldNotBeNull(), loadFreshFromDisk(folder, allFiles))

      allFiles.remove(extraFile)
      processed.removeFile(extraFile.relativeTo(folder))
      processed.reProcess()
      processed.lastRebuildWasIncremental shouldBe false
      assertEquivalentBudgets(processed.budget.shouldNotBeNull(), loadFreshFromDisk(folder, allFiles))
    }

    it("keeps incremental state intact after validation errors and recovers cleanly") {
      val folder = tempdir("tally-err-", keepOnFailure = false).toPath()
      val checkingFile = (folder / "checking.yaml").createFile()
      val expensesFile = (folder / "expenses.yaml").createFile()
      val allFiles = listOf(checkingFile, expensesFile)

      checkingFile.writeText(checkingYaml())
      expensesFile.writeText(expensesYaml)

      val processed = ProcessedBudget()
      for (file in allFiles) {
        processed.addFile(folder, file.relativeTo(folder))
      }
      processed.reProcess()
      val originalBudget = processed.budget.shouldNotBeNull()

      // Bad balance date (>2 months apart) fails cleanly without corrupting state.
      checkingFile.writeText(
        checkingYaml(octBalanceLine = "  - { grp: Oct2026, date: 2026-05-01, camt: 1850.00 }")
      )
      processed.addFile(folder, checkingFile.relativeTo(folder))
      shouldThrow<BudgetLoadingException> { processed.reProcess() }
      processed.budget shouldBeSameInstanceAs originalBudget

      // Bad balance date after a transaction in the same month fails inside TransactionStatementBuilder cleanly.
      checkingFile.writeText(
        checkingYaml(octBalanceLine = "  - { grp: Oct2026, date: 2026-10-25, camt: 1850.00 }")
      )
      processed.addFile(folder, checkingFile.relativeTo(folder))
      shouldThrow<IllegalStateException> { processed.reProcess() }
      processed.budget shouldBeSameInstanceAs originalBudget

      // Fixing the balance recovers via incremental rebuild and matches fresh full load.
      checkingFile.writeText(
        checkingYaml(octBalanceLine = "  - { grp: Oct2026, date: 2026-10-01, camt: 1999.00 }")
      )
      processed.addFile(folder, checkingFile.relativeTo(folder))
      processed.reProcess()
      processed.lastRebuildWasIncremental shouldBe true
      assertEquivalentBudgets(processed.budget.shouldNotBeNull(), loadFreshFromDisk(folder, allFiles))
    }

    it("rebuilds 179-account 50k-transfer ledger incrementally well under 100ms") {
      val folder = tempdir("tally-perf-", keepOnFailure = false).toPath()
      val monthNames = listOf(JAN, FEB, MAR, APR, MAY, JUN, JUL, AUG, SEP, OCT, NOV, DEC)
      val months = (2022..2026).flatMap { year -> monthNames.map { it / year } } // 60 months

      val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
      try {
        val processed = ProcessedBudget(asyncScope = scope)

        // 19 external category accounts
        for (cat in 0 until 19) {
          val file = (folder / "ext_$cat.yaml").createFile()
          file.writeText(
            """
            name: ext_$cat
            path: [external, cat_${cat % 4}]
            owner: [arthur]
            opened_on: Jan2022
            """.trimIndent()
          )
          processed.addFile(folder, file.relativeTo(folder))
        }

        // 160 internal accounts (total 179 accounts), ~300 transfers each = ~48,000 canonical transfers (~96k tx)
        fun internalYaml(
          idx: Int,
          oct2026Balance: String,
          extraTransfer: Boolean = false
        ): String =
          buildString {
            appendLine("name: acct_$idx")
            appendLine("path: [internal, group_${idx % 8}, sub_${idx % 4}]")
            appendLine("owner: [arthur]")
            appendLine("opened_on: Jan2022")
            appendLine("balances:")
            for (m in months) {
              val amt = if (idx == 0 && m == OCT / 2026) oct2026Balance else "${1000 + idx}.00"
              val date = "%04d-%02d-01".format(m.year, m.month + 1)
              appendLine("  - { grp: $m, date: $date, camt: $amt }")
            }
            appendLine("transfers_to:")
            val targetCat = "ext_${idx % 19}"
            appendLine("  $targetCat:")
            for (m in months) {
              appendLine("    $m:")
              val prefix = "%04d-%02d".format(m.year, m.month + 1)
              for (t in 1..5) {
                val day = "%02d".format(t * 4)
                appendLine("      - { date: $prefix-$day, camt: ${10 + t}.00, desc: \"Merchant $idx item $t\" }")
              }
              if (extraTransfer && idx == 0 && m == OCT / 2026) {
                appendLine("      - { date: 2026-10-25, camt: 42.50, desc: \"New grocery run\" }")
              }
            }
          }

        lateinit var acct0File: Path
        for (idx in 0 until 160) {
          val file = (folder / "acct_$idx.yaml").createFile()
          if (idx == 0) acct0File = file
          file.writeText(internalYaml(idx, oct2026Balance = "1000.00"))
          processed.addFile(folder, file.relativeTo(folder))
        }

        val fullStart = TimeSource.Monotonic.markNow()
        processed.reProcess()
        val fullMs = fullStart.elapsedNow().inWholeMilliseconds
        processed.lastRebuildWasIncremental shouldBe false

        // Balance-only edit on 1 account
        acct0File.writeText(internalYaml(0, oct2026Balance = "1234.56"))
        val balStart = TimeSource.Monotonic.markNow()
        processed.addFile(folder, acct0File.relativeTo(folder))
        processed.reProcess()
        val balMs = balStart.elapsedNow().inWholeMilliseconds
        processed.lastRebuildWasIncremental shouldBe true

        // Add 1 transfer on 1 account
        acct0File.writeText(internalYaml(0, oct2026Balance = "1234.56", extraTransfer = true))
        val txStart = TimeSource.Monotonic.markNow()
        processed.addFile(folder, acct0File.relativeTo(folder))
        processed.reProcess()
        val txMs = txStart.elapsedNow().inWholeMilliseconds
        processed.lastRebuildWasIncremental shouldBe true

        println(
          "ProcessedBudget 179-account 96k-tx benchmark: full=${fullMs}ms, balanceEdit=${balMs}ms, transferEdit=${txMs}ms"
        )
        balMs shouldBeLessThan 100L
        txMs shouldBeLessThan 100L
      } finally {
        scope.cancel()
      }
    }
  }
})
