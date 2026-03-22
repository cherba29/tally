package com.cherba29.tally.core

import com.cherba29.tally.core.MonthName.APR
import com.cherba29.tally.core.MonthName.DEC
import com.cherba29.tally.core.MonthName.FEB
import com.cherba29.tally.core.MonthName.JAN
import com.cherba29.tally.core.MonthName.NOV
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate

class BudgetTest : DescribeSpec({
  it("build empty budget") {
    val budget = budget {}
    budget.accounts.size shouldBe 0
    budget.balances.size shouldBe 0
    budget.months.size shouldBe 0
    budget.transfers.size shouldBe 0
  }

  it("build simple") {
    val account1 = Account(
      name = "test-account1",
      type = Account.Type.EXTERNAL,
      owners = listOf(),
    )
    val account2 = Account(
      name = "test-account2",
      type = Account.Type.BILL,
      owners = listOf(),
    )
    val account3 = Account(
      name = "test-account3",
      type = Account.Type.BILL,
      owners = listOf(),
    )
    val budget = budget {
      setAccount(account1)
      setAccount(account2)
      setAccount(account3)
      setBalance(
        "test-account1",
        NOV / 2019,
        Balance(100, LocalDate(2019, 11, 1), Balance.Type.PROJECTED)
      )
      setBalance(
        "test-account1",
        DEC / 2019,
        Balance(200, LocalDate(2019, 12, 1), Balance.Type.PROJECTED)
      )
      setBalance(
        "test-account2",
        NOV / 2019,
        Balance(200, LocalDate(2019, 11, 3), Balance.Type.CONFIRMED)
      )
      addTransfer(
        TransferData(
          toAccount = "test-account1",
          toMonth = NOV / 2019,
          fromAccount = "test-account2",
          fromMonth = NOV / 2019,
          balance = Balance(50, LocalDate(2019, 11, 2), Balance.Type.CONFIRMED),
          description = null
        )
      )
      addTransfer(
        TransferData(
          toAccount = "test-account3",
          toMonth = NOV / 2019,
          fromAccount = "test-account2",
          fromMonth = NOV / 2019,
          balance = Balance(70, LocalDate(2019, 11, 2), Balance.Type.CONFIRMED),
          description = null
        )
      )
    }
    budget.accounts.size shouldBe 3
    budget.accounts["test-account1"] shouldBe account1
    budget.accounts["test-account2"] shouldBe account2
    budget.balances.size shouldBe 2
    budget.months shouldBe listOf("Nov2019", "Dec2019").map { Month.fromString(it) }
    budget.transfers.size shouldBe 3
  }

  it("build budget - duplicate balance") {
    val builder = BudgetBuilder()
    val account1 = Account(
      name = "test-account1",
      type = Account.Type.EXTERNAL,
      owners = listOf(),
    )
    builder.setAccount(account1)
    builder.setBalance(
      "test-account1",
      NOV / 2019,
      Balance(10000, LocalDate(2019, 11, 1), Balance.Type.PROJECTED)
    )
    val exception = shouldThrow<IllegalArgumentException> {
      builder.setBalance(
        "test-account1",
        NOV / 2019,
        Balance(20000, LocalDate(2020, 3, 1), Balance.Type.PROJECTED)
      )
    }
    exception.message shouldBe "Balance for 'test-account1' 'Nov2019' is already set to Balance { amount: 200.00, date: 2020-03-01, type: PROJECTED }"
  }

  it("build budget - bad to account") {
    val exception = shouldThrow<IllegalArgumentException> {
      budget {
        addTransfer(
          TransferData(
            toAccount = "test-account1",
            toMonth = NOV / 2019,
            fromAccount = "test-account2",
            fromMonth = NOV / 2019,
            balance = Balance(50, LocalDate(2019, 12, 2), Balance.Type.CONFIRMED),
            description = null,
          )
        )
      }
    }
    exception.message shouldBe "Unknown account test-account1"
  }

  it("build budget - bad from account") {
    val account1 = Account(
      name = "test-account1",
      type = Account.Type.EXTERNAL,
      owners = listOf(),
    )
    val exception = shouldThrow<IllegalArgumentException> {
      budget {
        setAccount(account1)
        addTransfer(
          TransferData(
            toAccount = "test-account1",
            toMonth = NOV / 2019,
            fromAccount = "test-account2",
            fromMonth = NOV / 2019,
            balance = Balance(50, LocalDate(2019, 11, 2), Balance.Type.CONFIRMED),
            description = null,
          )
        )
      }
    }
    exception.message shouldBe "Unknown account test-account2"
  }

  describe("findActive accounts") {
    it("no accounts") {
      val budget = budget {}

      budget.accounts.size shouldBe 0
      budget.findActiveAccounts() shouldBe listOf()
    }

    it("no months") {
      val account1 = Account(
        name = "test-account1",
        type = Account.Type.EXTERNAL,
        owners = listOf(),
      )
      val budget = budget {
        setAccount(account1)
      }

      budget.accounts.size shouldBe 1
      budget.findActiveAccounts() shouldBe listOf()
    }

    it("open account") {
      val account1 = Account(
        name = "test-account1",
        type = Account.Type.EXTERNAL,
        owners = listOf(),
        openedOn = APR / 2026
      )
      val budget = budget {
        setAccount(account1)
      }
      budget.accounts.size shouldBe 1
      budget.findActiveAccounts() shouldBe listOf(account1)
    }

    it("multiple accounts") {
      val account1 = Account(
        name = "test-account1",
        type = Account.Type.EXTERNAL,
        owners = listOf(),
        openedOn = APR / 2026
      )

      val account2 = Account(
        name = "test-account2",
        type = Account.Type.EXTERNAL,
        owners = listOf(),
      )

      val account3 = Account(
        name = "test-account3",
        type = Account.Type.EXTERNAL,
        owners = listOf(),
        openedOn = JAN / 2020,
        closedOn = FEB / 2020,
      )
      val budget = budget {
        setAccount(account1)
        setAccount(account2)
        setAccount(account3)
      }
      budget.accounts.size shouldBe 3
      budget.findActiveAccounts() shouldBe listOf(account1, account2, account3)
    }
  }

})
