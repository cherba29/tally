package com.cherba29.tally.data.builder

import com.cherba29.tally.NotFoundException
import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.MonthName.FEB
import com.cherba29.tally.core.MonthName.MAR
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class TransfersSummaryBuilderTest : DescribeSpec({
  describe("transfersSummary") {
    val account1 = Account("test-account1", owners = setOf("john"), path = listOf("internal"), openedOn = FEB / 2026)
    val account2 = Account("test-account2", owners = setOf("john"), path = listOf("external"), openedOn = MAR / 2026)
    val testBudget = budget {
      addAccount(account1, mapOf(FEB / 2026 to Balance(100, (FEB / 2026).toDate(), Balance.Type.CONFIRMED)))
      addAccount(account2, mapOf(MAR / 2026 to Balance(50, (MAR / 2026).toDate(), Balance.Type.CONFIRMED)))
      addAccountTransfer(
        account1,
        account2.name,
        MAR / 2026,
        Balance(50, (MAR / 2026).toDate(), Balance.Type.CONFIRMED)
      )
    }

    it("computes transfer summary for leaf account") {
      val summary = testBudget.transfersSummary(
        accountPath = "john/internal/test-account1",
        startMonth = MAR / 2026,
        endMonth = MAR / 2026
      )
      summary.months shouldBe listOf(MAR / 2026)
      summary.data.size shouldBe 1
      val monthData = summary.data.first()
      monthData.internalTransfers shouldBe 0
      monthData.externalTransfers shouldBe -50
      monthData.totalMonthTransfers shouldBe -50
      monthData.totalTransfers shouldBe -50
    }

    it("computes transfer summary for summary account") {
      val summary = testBudget.transfersSummary(
        accountPath = "john/internal",
        startMonth = MAR / 2026,
        endMonth = MAR / 2026
      )
      summary.months shouldBe listOf(MAR / 2026)
      summary.data.size shouldBe 1
      val monthData = summary.data.first()
      monthData.internalTransfers shouldBe 0
      monthData.externalTransfers shouldBe -50
      monthData.totalMonthTransfers shouldBe -50
      monthData.totalTransfers shouldBe -50
    }

    it("throws NotFoundException when account does not exist") {
      val ex = shouldThrow<NotFoundException> {
        testBudget.transfersSummary(
          accountPath = "john/internal/nonexistent",
          startMonth = MAR / 2026,
          endMonth = MAR / 2026
        )
      }
      ex.message shouldBe "'john/internal/nonexistent' not found."
    }
  }
})
