package com.cherba29.tally

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.MonthName.APR
import com.cherba29.tally.core.MonthName.MAR
import com.cherba29.tally.core.MonthName.MAY
import com.cherba29.tally.data.Loader
import com.cherba29.tally.data.builder.BudgetBuilder
import com.cherba29.tally.data.builder.budget
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk

class RecurringServiceTest : DescribeSpec({
  describe("recurringTransactions query") {
    it("throws NotFoundException for unknown groupId") {
      val account = Account("checking", owners = setOf("alex"), path = listOf("internal"), openedOn = MAR / 2026)
      val loader = mockk<Loader> {
        coEvery { budget() } returns budget {
          setAccount(listOf("alex", "internal", "checking"), account)
        }
      }

      val ex = shouldThrow<NotFoundException> {
        RecurringService(loader).recurringTransactions(groupId = "does-not-exist")
      }
      ex.message shouldBe "Recurring transaction group 'does-not-exist' not found."
    }

    it("returns group oriented for viewer account including cross-account occurrences") {
      val checking = Account("checking", owners = setOf("alex"), path = listOf("internal"), openedOn = MAR / 2026)
      val card = Account("credit-card", owners = setOf("alex"), path = listOf("internal"), openedOn = MAR / 2026)
      val expenses = Account("expenses", owners = setOf("alex"), path = listOf("external"), openedOn = MAR / 2026)

      val builtBudget = budget {
        setAccount(listOf("alex", "internal", "checking"), checking)
        setAccount(listOf("alex", "internal", "credit-card"), card)
        setAccount(listOf("alex", "external", "expenses"), expenses)
        addTransfer(
          BudgetBuilder.TransferRecord(
            toAccountName = "expenses",
            fromAccountPath = listOf("alex", "internal", "credit-card"),
            month = MAR / 2026,
            balance = Balance.confirmed(4500, "2026-03-22"),
            description = "Streaming subscription",
            tags = listOf()
          )
        )
        addTransfer(
          BudgetBuilder.TransferRecord(
            toAccountName = "expenses",
            fromAccountPath = listOf("alex", "internal", "credit-card"),
            month = APR / 2026,
            balance = Balance.confirmed(4500, "2026-04-22"),
            description = "Streaming subscription",
            tags = listOf()
          )
        )
        addTransfer(
          BudgetBuilder.TransferRecord(
            toAccountName = "expenses",
            fromAccountPath = listOf("alex", "internal", "checking"),
            month = MAY / 2026,
            balance = Balance.confirmed(4500, "2026-05-22"),
            description = "Streaming subscription",
            tags = listOf()
          )
        )
      }

      val loader = mockk<Loader> {
        coEvery { budget() } returns builtBudget
      }

      val stmt = StatementService(loader).statement("alex/internal/checking", MAY / 2026)
      val groupId = stmt.transactions.single().recurringGroupId
      groupId.shouldNotBeNull()

      val fromChecking = RecurringService(loader).recurringTransactions(
        groupId = groupId,
        accountPath = "alex/internal/checking"
      )
      fromChecking.id shouldBe groupId
      fromChecking.label shouldBe "Streaming subscription"
      fromChecking.cadence shouldBe "MONTHLY"
      fromChecking.occurrenceCount shouldBe 3
      fromChecking.isFixedAmount shouldBe true
      fromChecking.averageAmount shouldBe 4500
      fromChecking.totalAmount shouldBe 13500
      fromChecking.typicalDayOfMonth shouldBe 22
      fromChecking.transactions shouldHaveSize 3

      // Newest occurrence (May) is on checking; older ones on credit-card have isCurrentAccount = false.
      fromChecking.transactions[0].apply {
        month shouldBe MAY / 2026
        isCurrentAccount shouldBe true
        isExpense shouldBe true
        isIncome shouldBe false
        balance.amount shouldBe -4500
        counterpartyAccountName shouldBe "expenses"
      }
      fromChecking.transactions[1].apply {
        month shouldBe APR / 2026
        isCurrentAccount shouldBe false
        isExpense shouldBe true
        balance.amount shouldBe -4500
        fromAccountName shouldBe "credit-card"
        counterpartyAccountName shouldBe "expenses"
      }

      // From expenses viewpoint, signs are positive (inflows into expenses).
      val fromExpenses = RecurringService(loader).recurringTransactions(
        groupId = groupId,
        accountPath = "alex/external/expenses"
      )
      fromExpenses.transactions.all { it.isCurrentAccount } shouldBe true
      fromExpenses.transactions.all { it.balance.amount == 4500L } shouldBe true
      fromExpenses.transactions.all { it.isIncome } shouldBe true
    }
  }
})
