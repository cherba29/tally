package com.cherba29.tally.recurring

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.MonthName.AUG
import com.cherba29.tally.core.MonthName.FEB
import com.cherba29.tally.core.MonthName.JAN
import com.cherba29.tally.core.MonthName.NOV
import com.cherba29.tally.core.MonthName.OCT
import com.cherba29.tally.core.MonthName.SEP
import com.cherba29.tally.core.Transaction
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate

class TransactionSimilarityTest : DescribeSpec({
  fun candidate(
    index: Int = 0,
    owner: String = "alex",
    from: String = "alex/internal/checking",
    to: String = "alex/external/expenses",
    month: com.cherba29.tally.core.Month = OCT / 2026,
    date: String = "2026-10-18",
    amount: Long = 7500,
    desc: String? = "Fiber internet",
    type: Transaction.Type = Transaction.Type.EXPENSE
  ): TransferCandidate =
    TransferCandidate(
      transferIndex = index,
      owner = owner,
      fromAccountPath = from,
      fromAccountName = from.substringAfterLast('/'),
      toAccountPath = to,
      toAccountName = to.substringAfterLast('/'),
      month = month,
      balance = Balance.confirmed(amount, date),
      description = desc,
      type = type
    )

  describe("hard disqualifiers") {
    it("returns 0 for different owners, types, flow signs, reversed accounts, or disjoint accounts") {
      val base = candidate()
      val norm = DescriptionNormalizer.normalize(base.description)

      val diffOwner = candidate(owner = "jordan")
      TransactionSimilarity.score(base, norm, diffOwner, norm) shouldBe 0.0

      val diffType = candidate(type = Transaction.Type.TRANSFER)
      TransactionSimilarity.score(base, norm, diffType, norm) shouldBe 0.0

      val refund = candidate(amount = -7500)
      TransactionSimilarity.score(base, norm, refund, norm) shouldBe 0.0

      val reversed = candidate(from = base.toAccountPath, to = base.fromAccountPath)
      TransactionSimilarity.score(base, norm, reversed, norm) shouldBe 0.0

      val disjoint = candidate(from = "alex/internal/credit-card", to = "alex/external/other")
      TransactionSimilarity.score(base, norm, disjoint, norm) shouldBe 0.0
    }
  }

  describe("multi-signal scoring") {
    it("scores identical recurring bills at 1.0") {
      val a = candidate(month = OCT / 2026, date = "2026-10-18", amount = 7500, desc = "Fiber internet")
      val b = candidate(month = NOV / 2026, date = "2026-11-18", amount = 7500, desc = "Fiber internet")
      val score = TransactionSimilarity.score(
        a,
        DescriptionNormalizer.normalize(a.description),
        b,
        DescriptionNormalizer.normalize(b.description)
      )
      score shouldBe 1.0
    }

    it("scores variable utilities with extra parenthetical notes above threshold") {
      val a = candidate(month = OCT / 2026, date = "2026-10-24", amount = 10890, desc = "Electric & gas utility")
      val b = candidate(
        month = AUG / 2026,
        date = "2026-08-24",
        amount = 14280,
        desc = "Electric & gas utility (summer AC)"
      )
      val score = TransactionSimilarity.score(
        a,
        DescriptionNormalizer.normalize(a.description),
        b,
        DescriptionNormalizer.normalize(b.description)
      )
      score shouldBeGreaterThanOrEqual 0.72
    }

    it("rejects unrelated expenses on the same catch-all expenses account") {
      val jacket = candidate(
        from = "alex/internal/credit-card",
        month = OCT / 2026,
        date = "2026-10-13",
        amount = 16500,
        desc = "Autumn jacket"
      )
      val shoes = candidate(
        from = "alex/internal/credit-card",
        month = SEP / 2026,
        date = "2026-09-14",
        amount = 11200,
        desc = "Running shoes"
      )
      val score = TransactionSimilarity.score(
        jacket,
        DescriptionNormalizer.normalize(jacket.description),
        shoes,
        DescriptionNormalizer.normalize(shoes.description)
      )
      score shouldBe 0.0
    }

    it("scores cross-account recurring subscriptions above threshold when description, amount, and cadence align") {
      val onCard = candidate(
        from = "alex/internal/credit-card",
        to = "alex/external/expenses",
        month = OCT / 2026,
        date = "2026-10-22",
        amount = 4500,
        desc = "Streaming & cloud subscriptions"
      )
      val onChecking = candidate(
        from = "alex/internal/checking",
        to = "alex/external/expenses",
        month = NOV / 2026,
        date = "2026-11-22",
        amount = 4500,
        desc = "Streaming & cloud subscriptions"
      )
      val score = TransactionSimilarity.score(
        onCard,
        DescriptionNormalizer.normalize(onCard.description),
        onChecking,
        DescriptionNormalizer.normalize(onChecking.description)
      )
      score shouldBeGreaterThanOrEqual 0.85
    }

    it("handles blank descriptions strictly by requiring exact accounts and strong amount or timing") {
      val a = candidate(month = OCT / 2026, date = "2026-10-02", amount = 185000, desc = null)
      val b = candidate(month = NOV / 2026, date = "2026-11-02", amount = 185000, desc = "")
      val matchScore = TransactionSimilarity.score(
        a,
        NormalizedDescription.EMPTY,
        b,
        NormalizedDescription.EMPTY
      )
      matchScore shouldBe 1.0

      val irregular = candidate(month = NOV / 2026, date = "2026-11-19", amount = 4200, desc = "")
      TransactionSimilarity.score(
        a,
        NormalizedDescription.EMPTY,
        irregular,
        NormalizedDescription.EMPTY
      ) shouldBe 0.0

      val described = candidate(month = NOV / 2026, date = "2026-11-02", amount = 185000, desc = "Rent")
      TransactionSimilarity.score(
        a,
        NormalizedDescription.EMPTY,
        described,
        DescriptionNormalizer.normalize(described.description)
      ) shouldBe 0.0
    }
  }

  describe("timingSimilarity") {
    it("handles month-end circular wrap and weekend shifts") {
      TransactionSimilarity.timingSimilarity(LocalDate(2026, 1, 31), LocalDate(2026, 2, 28)) shouldBe 1.0
      TransactionSimilarity.timingSimilarity(LocalDate(2026, 10, 15), LocalDate(2026, 11, 13)) shouldBe 1.0
      TransactionSimilarity.timingSimilarity(LocalDate(2026, 10, 1), LocalDate(2026, 10, 15)) shouldBe 0.7
      TransactionSimilarity.timingSimilarity(LocalDate(2026, 10, 1), LocalDate(2026, 10, 19)) shouldBeLessThan 0.5
    }

    it("handles monthly wrap across Jan and Feb dates") {
      val a = candidate(month = JAN / 2026, date = "2026-01-31", amount = 260000, desc = "Paycheck")
      val b = candidate(month = FEB / 2026, date = "2026-02-28", amount = 260000, desc = "Paycheck")
      TransactionSimilarity.score(
        a,
        DescriptionNormalizer.normalize(a.description),
        b,
        DescriptionNormalizer.normalize(b.description)
      ) shouldBe 1.0
    }
  }
})
