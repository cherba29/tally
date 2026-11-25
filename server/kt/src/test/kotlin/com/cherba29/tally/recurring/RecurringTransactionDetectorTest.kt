package com.cherba29.tally.recurring

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.MonthName.APR
import com.cherba29.tally.core.MonthName.AUG
import com.cherba29.tally.core.MonthName.DEC
import com.cherba29.tally.core.MonthName.JAN
import com.cherba29.tally.core.MonthName.JUL
import com.cherba29.tally.core.MonthName.JUN
import com.cherba29.tally.core.MonthName.MAY
import com.cherba29.tally.core.MonthName.NOV
import com.cherba29.tally.core.MonthName.OCT
import com.cherba29.tally.core.MonthName.SEP
import com.cherba29.tally.core.Transaction
import com.cherba29.tally.data.yaml.YamlDataParser
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.readText
import kotlin.math.roundToLong

class RecurringTransactionDetectorTest : DescribeSpec({
  val detector = RecurringTransactionDetector()

  fun makeCandidate(
    index: Int,
    from: String = "alex/internal/checking",
    to: String = "alex/external/expenses",
    month: Month,
    date: String,
    amount: Long,
    desc: String?,
    type: Transaction.Type = Transaction.Type.EXPENSE,
    balanceType: Balance.Type = Balance.Type.CONFIRMED,
    owner: String = "alex"
  ): TransferCandidate =
    TransferCandidate(
      transferIndex = index,
      owner = owner,
      fromAccountPath = from,
      fromAccountName = from.substringAfterLast('/'),
      toAccountPath = to,
      toAccountName = to.substringAfterLast('/'),
      month = month,
      balance = Balance(amount, LocalDate.parse(date), balanceType),
      description = desc,
      type = type
    )

  describe("RecurringTransactionDetector") {
    it("groups noisy bank descriptions with varying IDs, dates, and confirmation codes") {
      val candidates = listOf(
        makeCandidate(0, month = AUG / 2026, date = "2026-08-12", amount = 1199, desc = "PAYPAL *SPOTIFY 4029357733"),
        makeCandidate(1, month = SEP / 2026, date = "2026-09-12", amount = 1199, desc = "PAYPAL *SPOTIFY 4029358844"),
        makeCandidate(2, month = OCT / 2026, date = "2026-10-12", amount = 1199, desc = "PAYPAL *SPOTIFY 4029359981"),
        makeCandidate(
          3,
          month = AUG / 2026,
          date = "2026-08-15",
          amount = 11250,
          desc = "ACH DEBIT CON EDISON 2026-08-15 REF#111222"
        ),
        makeCandidate(
          4,
          month = SEP / 2026,
          date = "2026-09-15",
          amount = 11840,
          desc = "ACH DEBIT CON EDISON 2026-09-15 REF#333444"
        ),
        makeCandidate(
          5,
          month = OCT / 2026,
          date = "2026-10-15",
          amount = 10910,
          desc = "ACH DEBIT CON EDISON 2026-10-15 REF#555666"
        ),
        makeCandidate(6, month = SEP / 2026, date = "2026-09-20", amount = 8500, desc = "One-off concert tickets")
      )

      val index = detector.detect(candidates)
      index.groups shouldHaveSize 2

      val spotifyGroup = index.findGroup(
        accountPath = "alex/internal/checking",
        targetAccountPath = "alex/external/expenses",
        month = OCT / 2026,
        balance = Balance.confirmed(-1199, "2026-10-12"),
        description = "PAYPAL *SPOTIFY 4029359981"
      )
      spotifyGroup.shouldNotBeNull()
      spotifyGroup.occurrenceCount shouldBe 3
      spotifyGroup.cadence shouldBe RecurringCadence.MONTHLY
      spotifyGroup.typicalDayOfMonth shouldBe 12
      spotifyGroup.isFixedAmount shouldBe true
      spotifyGroup.averageAmount shouldBe 1199

      // Mirror lookup from destination account returns the exact same group.
      val spotifyMirror = index.findGroup(
        accountPath = "alex/external/expenses",
        targetAccountPath = "alex/internal/checking",
        month = OCT / 2026,
        balance = Balance.confirmed(1199, "2026-10-12"),
        description = "PAYPAL *SPOTIFY 4029359981"
      )
      spotifyMirror shouldBe spotifyGroup

      val conEdGroup = index.findGroup(
        accountPath = "alex/internal/checking",
        targetAccountPath = "alex/external/expenses",
        month = SEP / 2026,
        balance = Balance.confirmed(-11840, "2026-09-15"),
        description = "ACH DEBIT CON EDISON 2026-09-15 REF#333444"
      )
      conEdGroup.shouldNotBeNull()
      conEdGroup.occurrenceCount shouldBe 3
      conEdGroup.cadence shouldBe RecurringCadence.MONTHLY
      conEdGroup.isFixedAmount shouldBe false
      conEdGroup.minAmount shouldBe 10910
      conEdGroup.maxAmount shouldBe 11840

      // One-off is not indexed.
      index
        .findGroup(
          accountPath = "alex/internal/checking",
          targetAccountPath = "alex/external/expenses",
          month = SEP / 2026,
          balance = Balance.confirmed(-8500, "2026-09-20"),
          description = "One-off concert tickets"
        ).shouldBeNull()
    }

    it("detects cross-account subscription migration from credit-card to checking") {
      val candidates = listOf(
        makeCandidate(
          0,
          from = "alex/internal/credit-card",
          month = AUG / 2026,
          date = "2026-08-22",
          amount = 4500,
          desc = "Streaming & cloud subscriptions"
        ),
        makeCandidate(
          1,
          from = "alex/internal/credit-card",
          month = SEP / 2026,
          date = "2026-09-22",
          amount = 4500,
          desc = "Streaming & cloud subscriptions"
        ),
        makeCandidate(
          2,
          from = "alex/internal/checking",
          month = OCT / 2026,
          date = "2026-10-22",
          amount = 4500,
          desc = "Streaming & cloud subscriptions"
        )
      )

      val index = detector.detect(candidates)
      index.groups shouldHaveSize 1
      val group = index.groups.first()
      group.occurrenceCount shouldBe 3
      group.cadence shouldBe RecurringCadence.MONTHLY
      group.occurrences.first().fromAccountName shouldBe "checking"
    }

    it("does not mark same-day duplicate transactions as recurring") {
      val candidates = listOf(
        makeCandidate(0, month = OCT / 2026, date = "2026-10-10", amount = 550, desc = "Corner cafe coffee"),
        makeCandidate(1, month = OCT / 2026, date = "2026-10-10", amount = 550, desc = "Corner cafe coffee"),
        makeCandidate(2, month = OCT / 2026, date = "2026-10-12", amount = 550, desc = "Corner cafe coffee")
      )

      val index = detector.detect(candidates)
      index.groups shouldHaveSize 0
    }

    it("prevents single-linkage chaining across dissimilar transactions") {
      val candidates = listOf(
        makeCandidate(0, month = AUG / 2026, date = "2026-08-10", amount = 5000, desc = "alpha beta gamma"),
        makeCandidate(1, month = SEP / 2026, date = "2026-09-10", amount = 5000, desc = "beta gamma delta"),
        makeCandidate(2, month = OCT / 2026, date = "2026-10-10", amount = 5000, desc = "gamma delta epsilon")
      )

      val index = detector.detect(candidates)
      // Even if adjacent pairs partially overlap, anchor check prevents all 3 unrelated items collapsing together.
      index.groups.forEach { group ->
        val descriptions = group.occurrences.map { it.description }.toSet()
        (descriptions.contains("alpha beta gamma") && descriptions.contains("gamma delta epsilon")) shouldBe false
      }
    }

    it("produces deterministic stable group IDs regardless of input order or unrelated additions") {
      val base = listOf(
        makeCandidate(0, month = AUG / 2026, date = "2026-08-18", amount = 7500, desc = "Fiber internet"),
        makeCandidate(1, month = SEP / 2026, date = "2026-09-18", amount = 7500, desc = "Fiber internet"),
        makeCandidate(2, month = OCT / 2026, date = "2026-10-18", amount = 7500, desc = "Fiber internet")
      )

      val idOriginal = detector
        .detect(base)
        .groups
        .single()
        .id
      val idReversed = detector
        .detect(base.reversed())
        .groups
        .single()
        .id
      val withUnrelated = base + makeCandidate(
        99,
        month = OCT / 2026,
        date = "2026-10-05",
        amount = 16500,
        desc = "Autumn jacket"
      )
      val idWithExtra = detector
        .detect(withUnrelated)
        .groups
        .single()
        .id

      idOriginal shouldBe idReversed
      idOriginal shouldBe idWithExtra
    }

    it("infers weekly, biweekly, monthly, quarterly, yearly, and irregular cadences") {
      RecurringTransactionDetector.inferCadence(
        listOf(LocalDate(2026, 7, 1), LocalDate(2026, 7, 8), LocalDate(2026, 7, 15))
      ) shouldBe RecurringCadence.WEEKLY

      RecurringTransactionDetector.inferCadence(
        listOf(LocalDate(2026, 7, 15), LocalDate(2026, 7, 31), LocalDate(2026, 8, 14), LocalDate(2026, 8, 31))
      ) shouldBe RecurringCadence.BIWEEKLY

      RecurringTransactionDetector.inferCadence(
        listOf(LocalDate(2026, 7, 18), LocalDate(2026, 8, 18), LocalDate(2026, 9, 18))
      ) shouldBe RecurringCadence.MONTHLY

      RecurringTransactionDetector.inferCadence(
        listOf(LocalDate(2026, 1, 15), LocalDate(2026, 4, 15), LocalDate(2026, 7, 15))
      ) shouldBe RecurringCadence.QUARTERLY

      RecurringTransactionDetector.inferCadence(
        listOf(LocalDate(2024, 5, 1), LocalDate(2025, 5, 1), LocalDate(2026, 5, 1))
      ) shouldBe RecurringCadence.YEARLY

      RecurringTransactionDetector.inferCadence(
        listOf(LocalDate(2026, 1, 1), LocalDate(2026, 2, 18), LocalDate(2026, 4, 29))
      ) shouldBe RecurringCadence.IRREGULAR
    }

    it("detects expected recurring series and excludes one-offs in sample-data") {
      val sampleRoot = Paths.get("../../sample-data").normalize()
      val parser = YamlDataParser()
      val yamlFiles = Files.walk(sampleRoot).use { stream ->
        stream.filter { it.toString().endsWith(".yaml") }.sorted().toList()
      }

      val parsedByName = yamlFiles
        .map { file -> parser.parseContent(file.readText(Charsets.UTF_8), sampleRoot.relativize(file)) }
        .filter { it.name != null }
        .associateBy { it.name!! }

      val candidates = mutableListOf<TransferCandidate>()
      var idx = 0
      for ((fromName, fromYaml) in parsedByName) {
        val owner = fromYaml.owner!!.first()
        val fromPath = (listOf(owner) + fromYaml.path!! + fromName).joinToString("/")
        val fromIsExternal = fromYaml.path.contains("external")
        for ((toName, wrapper) in fromYaml.transfersTo.orEmpty()) {
          val toYaml = parsedByName.getValue(toName)
          val toPath = (listOf(owner) + toYaml.path!! + toName).joinToString("/")
          val toIsExternal = toYaml.path.contains("external")
          for (t in wrapper?.transfers.orEmpty()) {
            val (amt, bType) = if (t.pamt != null) {
              (t.pamt * 100.0).roundToLong() to Balance.Type.PROJECTED
            } else {
              (t.camt!! * 100.0).roundToLong() to Balance.Type.CONFIRMED
            }
            val canonicalType = when {
              !fromIsExternal && !toIsExternal -> Transaction.Type.TRANSFER
              fromIsExternal && !toIsExternal -> if (amt >= 0) Transaction.Type.INCOME else Transaction.Type.EXPENSE
              else -> if (amt >= 0) Transaction.Type.EXPENSE else Transaction.Type.INCOME
            }
            candidates.add(
              TransferCandidate(
                transferIndex = idx++,
                owner = owner,
                fromAccountPath = fromPath,
                fromAccountName = fromName,
                toAccountPath = toPath,
                toAccountName = toName,
                month = t.grp!!,
                balance = Balance(amt, t.date!!, bType),
                description = t.desc,
                type = canonicalType
              )
            )
          }
        }
      }

      val index = detector.detect(candidates)
      val groupsByLabel = index.groups.associateBy { it.label }

      // Expected recurring series in sample-data
      groupsByLabel.keys shouldContainExactlyInAnyOrder setOf(
        "Monthly apartment rent",
        "Fiber internet",
        "Electric & gas utility",
        "Pay off Nov2026 statement",
        "Acme Corp bi-monthly paycheck",
        "Streaming & cloud subscriptions",
        "Weekly groceries",
        "Restaurants & coffee"
      )

      groupsByLabel.getValue("Monthly apartment rent").apply {
        occurrenceCount shouldBe 5
        cadence shouldBe RecurringCadence.MONTHLY
        typicalDayOfMonth shouldBe 2
        isFixedAmount shouldBe true
        averageAmount shouldBe 185000
      }

      groupsByLabel.getValue("Fiber internet").apply {
        occurrenceCount shouldBe 5
        cadence shouldBe RecurringCadence.MONTHLY
        typicalDayOfMonth shouldBe 18
        isFixedAmount shouldBe true
        averageAmount shouldBe 7500
      }

      groupsByLabel.getValue("Electric & gas utility").apply {
        // Includes the "(summer AC)" and "(projected)" variants
        occurrenceCount shouldBe 5
        cadence shouldBe RecurringCadence.MONTHLY
        typicalDayOfMonth shouldBe 24
        isFixedAmount shouldBe false
      }

      groupsByLabel.getValue("Pay off Nov2026 statement").apply {
        // All 6 monthly credit-card payoffs (Jul-Dec) normalized together
        occurrenceCount shouldBe 6
        cadence shouldBe RecurringCadence.MONTHLY
        typicalDayOfMonth shouldBe 10
        isFixedAmount shouldBe false
      }

      groupsByLabel.getValue("Acme Corp bi-monthly paycheck").apply {
        occurrenceCount shouldBe 9
        cadence shouldBe RecurringCadence.BIWEEKLY
        isFixedAmount shouldBe true
        averageAmount shouldBe 260000
      }

      groupsByLabel.getValue("Streaming & cloud subscriptions").apply {
        occurrenceCount shouldBe 5
        cadence shouldBe RecurringCadence.MONTHLY
        typicalDayOfMonth shouldBe 22
        isFixedAmount shouldBe true
        averageAmount shouldBe 4500
      }

      groupsByLabel.getValue("Weekly groceries").apply {
        occurrenceCount shouldBe 8
        cadence shouldBe RecurringCadence.BIWEEKLY
      }

      // Verify one-offs are NOT included in any recurring group.
      val allRecurringDescriptions = index.groups.flatMap { g -> g.occurrences.map { it.description } }.toSet()
      val expectedOneOffs = listOf(
        "Holiday gifts (projected)",
        "Autumn jacket",
        "Running shoes",
        "Weekend train tickets",
        "Transit pass & rideshare",
        "Dinner with friends"
      )
      for (oneOff in expectedOneOffs) {
        allRecurringDescriptions.contains(oneOff) shouldBe false
      }
    }

    it("supports blank-description recurring fixed transfers when accounts and cadence match") {
      val candidates = listOf(
        makeCandidate(0, month = APR / 2026, date = "2026-04-05", amount = 50000, desc = null),
        makeCandidate(1, month = MAY / 2026, date = "2026-05-05", amount = 50000, desc = ""),
        makeCandidate(2, month = JUN / 2026, date = "2026-06-05", amount = 50000, desc = null)
      )

      val index = detector.detect(candidates)
      index.groups shouldHaveSize 1
      val group = index.groups.first()
      group.label shouldBe "checking -> expenses"
      group.cadence shouldBe RecurringCadence.MONTHLY
      group.typicalDayOfMonth shouldBe 5
      group.isFixedAmount shouldBe true
    }

    it("keeps group labels stable across projected status suffixes") {
      val candidates = listOf(
        makeCandidate(
          0,
          month = OCT / 2026,
          date = "2026-10-15",
          amount = 260000,
          desc = "Acme Corp paycheck",
          balanceType = Balance.Type.CONFIRMED
        ),
        makeCandidate(
          1,
          month = NOV / 2026,
          date = "2026-11-15",
          amount = 260000,
          desc = "Acme Corp paycheck (projected)",
          balanceType = Balance.Type.PROJECTED
        ),
        makeCandidate(
          2,
          month = DEC / 2026,
          date = "2026-12-15",
          amount = 260000,
          desc = "Acme Corp paycheck (projected)",
          balanceType = Balance.Type.PROJECTED
        )
      )

      val index = detector.detect(candidates)
      index.groups.single().label shouldBe "Acme Corp paycheck"
    }
  }
})
