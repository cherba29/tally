package com.cherba29.tally.core

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate

class BudgetTest : DescribeSpec({
  it("build empty budget") {
    val builder = BudgetBuilder()
    val budget = builder.build()
    budget.accounts.size shouldBe 0
    budget.balances.size shouldBe 0
    budget.months.size shouldBe 0
    budget.transfers.size shouldBe 0
  }

  it("build simple") {
    val builder = BudgetBuilder()
    val account1 = Account(
      name = "test-account1",
      type = AccountType.EXTERNAL,
      owners = listOf(),
    )
    val account2 = Account(
      name = "test-account2",
      type = AccountType.BILL,
      owners = listOf(),
    )
    val account3 = Account(
      name = "test-account3",
      type = AccountType.BILL,
      owners = listOf(),
    )

    builder.setAccount(account1)
    builder.setAccount(account2)
    builder.setAccount(account3)
    builder.setBalance(
      "test-account1",
      "Nov2019",
      Balance(100, LocalDate(2019, 11, 1), BalanceType.PROJECTED)
    )
    builder.setBalance(
      "test-account1",
      "Dec2019",
      Balance(200, LocalDate(2019, 12, 1), BalanceType.PROJECTED)
    )
    builder.setBalance(
      "test-account2",
      "Nov2019",
      Balance(200, LocalDate(2019, 11, 3), BalanceType.CONFIRMED)
    )
    builder.addTransfer(
      TransferData(
        toAccount = "test-account1",
        toMonth = Month(2019, 10),
        fromAccount = "test-account2",
        fromMonth = Month(2019, 10),
        balance = Balance(50, LocalDate(2019, 11, 2), BalanceType.CONFIRMED),
        description = null
      )
    )
    builder.addTransfer(
      TransferData(
        toAccount = "test-account3",
        toMonth = Month(2019, 10),
        fromAccount = "test-account2",
        fromMonth = Month(2019, 10),
        balance = Balance(70, LocalDate(2019, 11, 2), BalanceType.CONFIRMED),
        description = null
      )
    )

    val budget = builder.build()
    budget.accounts.size shouldBe 3
    builder.accounts["test-account1"] shouldBe account1
    builder.accounts["test-account2"] shouldBe account2
    budget.balances.size shouldBe 2
    budget.months shouldBe listOf("Nov2019", "Dec2019").map { Month.fromString(it) }
    budget.transfers.size shouldBe 3
  }

  it("build budget - duplicate balance") {
    val builder = BudgetBuilder();
    val account1 = Account(
      name = "test-account1",
      type = AccountType.EXTERNAL,
      owners = listOf(),
    )
    builder.setAccount(account1)
    builder.setBalance(
      "test-account1",
      "Nov2019",
      Balance(10000, LocalDate(2019, 11, 1), BalanceType.PROJECTED)
    );
    val exception = shouldThrow<IllegalArgumentException> {
      builder.setBalance(
        "test-account1",
        "Nov2019",
        Balance(20000, LocalDate(2020, 3, 1), BalanceType.PROJECTED)
      )
    }
    exception.message shouldBe "Balance for 'test-account1' 'Nov2019' is already set to Balance { amount: 200.00, date: 2020-03-01, type: PROJECTED }"
  }

  it("build budget - bad to account") {
    val builder = BudgetBuilder();
    builder.addTransfer(
      TransferData(
        toAccount = "test-account1",
        toMonth = Month(2019, 10),
        fromAccount = "test-account2",
        fromMonth = Month(2019, 10),
        balance = Balance(50, LocalDate(2019, 12, 2), BalanceType.CONFIRMED),
        description = null,
      )
    )
    val exception = shouldThrow<IllegalArgumentException> { builder.build() }
    exception.message shouldBe "Unknown account test-account1"
  }

  it("build budget - bad from account") {
    val builder = BudgetBuilder();
    val account1 = Account(
      name = "test-account1",
      type = AccountType.EXTERNAL,
      owners = listOf(),
    )
    builder.setAccount(account1);
    builder.addTransfer(
      TransferData(
        toAccount = "test-account1",
        toMonth = Month(2019, 10),
        fromAccount = "test-account2",
        fromMonth = Month(2019, 10),
        balance = Balance(50, LocalDate(2019, 11, 2), BalanceType.CONFIRMED),
        description = null,
      )
    )
    val exception = shouldThrow<IllegalArgumentException> { builder.build() }
    exception.message shouldBe "Unknown account test-account2"
  }

  describe("findActive accounts") {
    it("no accounts") {
      val builder = BudgetBuilder();
      val budget = builder.build()

      budget.accounts.size shouldBe 0
      budget.findActiveAccounts() shouldBe listOf()
    }

    it("no months") {
      val builder = BudgetBuilder();
      val account1 = Account(
        name = "test-account1",
        type = AccountType.EXTERNAL,
        owners = listOf(),
      )
      builder.setAccount(account1);
      val budget = builder.build()

      budget.accounts.size shouldBe 1
      budget.findActiveAccounts() shouldBe listOf()
    }

    it("open account") {
      val builder = BudgetBuilder();

      val account1 = Account(
        name = "test-account1",
        type = AccountType.EXTERNAL,
        owners = listOf(),
        openedOn = Month(2026, 3)
      )
      builder.setAccount(account1);
      val budget = builder.build()

      budget.accounts.size shouldBe 1
      budget.findActiveAccounts() shouldBe listOf(account1)
    }

    it("multiple accounts") {
      val builder = BudgetBuilder();

      val account1 = Account(
        name = "test-account1",
        type = AccountType.EXTERNAL,
        owners = listOf(),
        openedOn = Month(2026, 3)
      )

      val account2 = Account(
        name = "test-account2",
        type = AccountType.EXTERNAL,
        owners = listOf(),
      )

      val account3 = Account(
        name = "test-account3",
        type = AccountType.EXTERNAL,
        owners = listOf(),
        openedOn = Month(2020, 0),
        closedOn = Month(2020, 1),
      )

      builder.setAccount(account1);
      builder.setAccount(account2);
      builder.setAccount(account3);
      val budget = builder.build()

      budget.accounts.size shouldBe 3
      budget.findActiveAccounts() shouldBe listOf(account1, account2, account3)
    }
  }

})
