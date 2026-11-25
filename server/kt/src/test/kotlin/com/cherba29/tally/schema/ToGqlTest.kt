package com.cherba29.tally.schema

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.MonthName.AUG
import com.cherba29.tally.core.MonthName.SEP
import com.cherba29.tally.core.Transaction
import com.cherba29.tally.recurring.RecurringCadence
import com.cherba29.tally.recurring.RecurringGroup
import com.cherba29.tally.recurring.RecurringOccurrence
import com.cherba29.tally.statement.SummaryStatement
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate

class ToGqlTest : DescribeSpec({
  describe("SummaryStatement.toGqlSummaryData") {
    it("empty") {
      val statement = SummaryStatement(
        monthRange = AUG / 2026..AUG / 2026,
        startBalance = Balance.confirmed(100, "2026-08-01")
      )
      statement.toGqlSummaryData("summary1") { treeNode, month -> false } shouldBe GqlSummaryData(
        statements = listOf(),
        total = GqlSummaryStatement(
          name = "summary1",
          month = AUG / 2026,
          addSub = 0,
          income = 0,
          change = 0,
          inFlows = 0,
          outFlows = 0,
          percentChange = 0f,
          annualizedPercentChange = 0f,
          totalPayments = 0,
          totalTransfers = 0,
          unaccounted = 0,
          endBalance = null,
          startBalance = GqlBalance(amount = 100, date = LocalDate(2026, 8, 1), type = "CONFIRMED", desc = "")
        )
      )
    }
  }

  describe("RecurringGroup.toGql") {
    it("orients transfer signs and counterparties for source, destination, and null viewer paths") {
      val transferGroup = RecurringGroup(
        id = "abc123def456",
        label = "Pay off statement",
        cadence = RecurringCadence.MONTHLY,
        typicalDayOfMonth = 10,
        averageAmount = 65000,
        minAmount = 60000,
        maxAmount = 70000,
        totalAmount = 130000,
        isFixedAmount = false,
        occurrences = listOf(
          RecurringOccurrence(
            fromAccountPath = "alex/internal/checking",
            fromAccountName = "checking",
            toAccountPath = "alex/internal/credit-card",
            toAccountName = "credit-card",
            month = SEP / 2026,
            balance = Balance.confirmed(70000, "2026-09-10"),
            description = "Pay off Aug2026 statement",
            type = Transaction.Type.TRANSFER
          ),
          RecurringOccurrence(
            fromAccountPath = "alex/internal/checking",
            fromAccountName = "checking",
            toAccountPath = "alex/internal/credit-card",
            toAccountName = "credit-card",
            month = AUG / 2026,
            balance = Balance.confirmed(60000, "2026-08-10"),
            description = "Pay off Jul2026 statement",
            type = Transaction.Type.TRANSFER
          )
        )
      )

      val fromChecking = transferGroup.toGql("alex/internal/checking")
      fromChecking.transactions[0].apply {
        balance.amount shouldBe -70000
        counterpartyAccountName shouldBe "credit-card"
        isIncome shouldBe false
        isExpense shouldBe false
        isCurrentAccount shouldBe true
      }

      val fromCard = transferGroup.toGql("alex/internal/credit-card")
      fromCard.transactions[0].apply {
        balance.amount shouldBe 70000
        counterpartyAccountName shouldBe "checking"
        isIncome shouldBe false
        isExpense shouldBe false
        isCurrentAccount shouldBe true
      }

      val noViewer = transferGroup.toGql(null)
      noViewer.transactions[0].apply {
        balance.amount shouldBe -70000
        counterpartyAccountName shouldBe "credit-card"
        isCurrentAccount shouldBe true
      }
    }

    it("orients income occurrences positively when viewer is null or unrelated") {
      val incomeGroup = RecurringGroup(
        id = "inc123456789",
        label = "Paycheck",
        cadence = RecurringCadence.BIWEEKLY,
        typicalDayOfMonth = null,
        averageAmount = 260000,
        minAmount = 260000,
        maxAmount = 260000,
        totalAmount = 260000,
        isFixedAmount = true,
        occurrences = listOf(
          RecurringOccurrence(
            fromAccountPath = "alex/external/payroll",
            fromAccountName = "payroll",
            toAccountPath = "alex/internal/checking",
            toAccountName = "checking",
            month = SEP / 2026,
            balance = Balance.confirmed(260000, "2026-09-15"),
            description = "Paycheck",
            type = Transaction.Type.INCOME
          )
        )
      )

      val fromSavings = incomeGroup.toGql("alex/internal/savings")
      fromSavings.transactions[0].apply {
        balance.amount shouldBe 260000
        counterpartyAccountName shouldBe "payroll"
        isIncome shouldBe true
        isExpense shouldBe false
        isCurrentAccount shouldBe false
      }
    }
  }
})
