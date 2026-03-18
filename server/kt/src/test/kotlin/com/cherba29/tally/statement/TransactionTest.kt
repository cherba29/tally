package com.cherba29.tally.statement

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.AccountType
import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.BudgetBuilder
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.TransferData
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.lang.IllegalStateException

class TransactionTest : DescribeSpec({
  describe("Build") {
    it("no months") {
      val builder = BudgetBuilder()
      val exception = shouldThrow<IllegalArgumentException> {
        buildTransactionStatementTable(builder.build(), owner = null)
      }
      exception.message shouldBe "Budget must have at least one month."
    }

    it("single account no transfers") {
      val builder = BudgetBuilder()
      val account = Account(
        name = "test-account",
        type = AccountType.EXTERNAL,
        owners = listOf(),
        openedOn = Month(2019, 11),
      )
      builder.setAccount(account)
      val table = buildTransactionStatementTable(builder.build(), owner = null)
      table.size shouldBe 1
      val stmt = table.first()
      assertSoftly {
        stmt.account shouldBe account
        stmt.coversPrevious shouldBe false
        stmt.coversProjectedPrevious shouldBe false
        stmt.endBalance shouldBe null
        stmt.hasProjectedTransfer shouldBe false
        stmt.inFlows shouldBe 0.0
        stmt.income shouldBe 0.0
        stmt.isCovered shouldBe true
        stmt.isProjectedCovered shouldBe true
        stmt.month shouldBe Month(2019, 11)
        stmt.outFlows shouldBe 0.0
        stmt.startBalance shouldBe null
        stmt.totalPayments shouldBe 0.0
        stmt.totalTransfers shouldBe 0.0
        stmt.transactions shouldBe listOf()
        stmt.isClosed shouldBe false
      }
    }

    it("bad account name on transfer") {
      val builder = BudgetBuilder()
      val account1 = Account(
        name = "test-account1",
        type = AccountType.CHECKING,
        owners = listOf("john"),
      )
      builder.setAccount(account1)
      builder.addTransfer(
        TransferData(
          fromAccount = "test-account1",
          toAccount = "test-account2",
          toMonth = Month.fromString("Dec2019"),
          fromMonth = Month.fromString("Dec2019"),
          balance = Balance.projected(2000, "2019-12-05"),
          description = "First transfer",
        )
      )
      val exception =
        shouldThrow<IllegalArgumentException> { buildTransactionStatementTable(builder.build(), owner = null) }
      exception.message shouldBe "Unknown account test-account2"
    }

    it("two accounts with common owner and transfers") {
      val builder = BudgetBuilder()
      val account1 = Account(
        name = "test-account1",
        type = AccountType.CHECKING,
        owners = listOf("john"),
      )
      val account2 = Account(
        name = "test-account2",
        type = AccountType.CREDIT,
        owners = listOf("john"),
      )

      builder.setAccount(account1)
      builder.setBalance("test-account1", Month(2019, Month.DEC), Balance.confirmed(10, "2019-12-01"))
      builder.setBalance("test-account1", Month(2020, Month.JAN), Balance.confirmed(20, "2020-01-01"))
      builder.setBalance("test-account1", Month(2020, Month.FEB), Balance.projected(30, "2020-02-01"))
      builder.setAccount(account2)
      builder.addTransfer(
        TransferData(
          fromAccount = "test-account1",
          toAccount = "test-account2",
          toMonth = Month.fromString("Dec2019"),
          fromMonth = Month.fromString("Dec2019"),
          balance = Balance.projected(2000, "2019-12-05"),
          description = "First transfer",
        )
      )

      builder.addTransfer(
        TransferData(
          fromAccount = "test-account1",
          toAccount = "test-account2",
          toMonth = Month.fromString("Dec2019"),
          fromMonth = Month.fromString("Dec2019"),
          balance = Balance.projected(1000, "2019-12-05"),
          description = "Second transfer",
        )
      )

      val table = buildTransactionStatementTable(builder.build(), owner = null)
      table.size shouldBe 6
      // TODO: enable snapshot tests.
      // table.toMatchSnapshot()
    }

    it("two accounts with external transfer") {
      val builder = BudgetBuilder()
      val account1 = Account(
        name = "test-account1",
        type = AccountType.EXTERNAL,
        owners = listOf(),
      )
      val account2 = Account(
        name = "test-account2",
        type = AccountType.CREDIT,
        owners = listOf("john"),
      )

      builder.setAccount(account1)
      builder.setBalance("test-account1", Month(2019, Month.DEC), Balance.confirmed(10, "2019-12-01"))
      builder.setBalance("test-account1", Month(2020, Month.JAN), Balance.confirmed(20, "2020-01-01"))
      builder.setBalance("test-account1", Month(2020, Month.FEB), Balance.projected(30, "2020-02-01"))
      builder.setAccount(account2)
      builder.addTransfer(
        TransferData(
          fromAccount = "test-account1",
          toAccount = "test-account2",
          toMonth = Month(2019, Month.DEC),
          fromMonth = Month(2019, Month.DEC),
          balance = Balance.projected(2000, "2019-12-05"),
          description = "First transfer",
        )
      )

      builder.addTransfer(
        TransferData(
          fromAccount = "test-account1",
          toAccount = "test-account2",
          toMonth = Month(2019, Month.DEC),
          fromMonth = Month(2019, Month.DEC),
          balance = Balance.projected(1000, "2019-12-05"),
          description = "Second transfer",
        )
      )

      val table = buildTransactionStatementTable(builder.build(), owner = null)
      table.size shouldBe 6
      // TODO: enable snapshot tests.
      // table.toMatchSnapshot()
    }

    it("single account with transfers") {
      val builder = BudgetBuilder()
      val account1 = Account(
        name = "test-account1",
        type = AccountType.CHECKING,
        owners = listOf("john"),
      )
      builder.setAccount(account1)
      builder.setBalance("test-account1", Month(2019, Month.DEC), Balance.confirmed(10, "2019-12-01"))
      builder.setBalance("test-account1", Month(2020, Month.JAN), Balance.confirmed(20, "2020-01-01"))
      builder.setBalance("test-account1", Month(2020, Month.FEB), Balance.projected(30, "2020-02-01"))
      builder.addTransfer(
        TransferData(
          fromAccount = "test-account1",
          toAccount = "test-account1",
          toMonth = Month(2019, Month.DEC),
          fromMonth = Month(2019, Month.DEC),
          balance = Balance.projected(2000, "2019-12-05"),
          description = "First transfer",
        )
      )

      val table = buildTransactionStatementTable(builder.build(), owner = null)
      table.size shouldBe 3
      // // TODO: enable snapshot tests.
      // table.toMatchSnapshot()
    }

    it("transfer with date before start balance") {
      val builder = BudgetBuilder()
      val account1 = Account(
        name = "test-account1",
        type = AccountType.CHECKING,
        owners = listOf("john"),
      )
      builder.setAccount(account1)
      builder.setBalance("test-account1", Month(2019, Month.DEC), Balance.confirmed(1000, "2019-12-01"))
      builder.addTransfer(
        TransferData(
          fromAccount = "test-account1",
          toAccount = "test-account1",
          toMonth = Month(2019, Month.DEC),
          fromMonth = Month(2019, Month.DEC),
          balance = Balance.projected(2000, "2019-11-25"),
          description = "First transfer",
        )
      )

      val exception =
        shouldThrow<IllegalStateException> { buildTransactionStatementTable(builder.build(), owner = null) }
      exception.message shouldBe "Balance Dec2019 Balance { amount: 10.00, date: 2019-12-01, type: CONFIRMED } " +
          "for account test-account1 starts after transaction test-account1 --> " +
          "test-account1/Balance { amount: 20.00, date: 2019-11-25, type: PROJECTED } " +
          "desc 'First transfer'"
    }

    it("transfer to closed account") {
      val builder = BudgetBuilder()
      val account1 = Account(
        name = "test-account1",
        type = AccountType.CHECKING,
        owners = listOf("john"),
        closedOn = Month.fromString("Nov2019")  // closed before TransactionStatement month
      )
      builder.setAccount(account1)
      builder.setBalance("test-account1", Month(2019, Month.DEC), Balance.confirmed(10, "2019-12-01"))
      builder.addTransfer(
        TransferData(
          fromAccount = "test-account1",
          toAccount = "test-account1",
          toMonth = Month(2019, Month.DEC),
          fromMonth = Month(2019, Month.DEC),
          balance = Balance.projected(2000, "2019-12-05"),
          description = "First transfer",
        )
      )
      val table = buildTransactionStatementTable(builder.build(), owner = null)
      table.size shouldBe 2  // Two transaction statements for the account
      table[0].month shouldBe Month(2019, Month.DEC)
      table[0].isClosed shouldBe true
      table[1].month shouldBe Month.fromString("Nov2019")
      table[1].isClosed shouldBe false
    }

    it("get transaction type") {
      val builder = BudgetBuilder()
      val account1 = Account(
        name = "test-account1",
        type = AccountType.CHECKING,
        owners = listOf("john"),
      )
      val account2 = Account(
        name = "test-account2",
        type = AccountType.CREDIT,
        owners = listOf("john"),
      )
      val account3 = Account(
        name = "test-account3",
        type = AccountType.EXTERNAL,
        owners = listOf(),
      )
      builder.setAccount(account1)
      builder.setAccount(account2)
      builder.setAccount(account3)
      builder.setBalance("test-account1", Month(2019, Month.DEC), Balance.confirmed(10, "2019-12-01"))
      builder.setBalance("test-account2", Month(2019, Month.DEC), Balance.confirmed(10, "2019-12-01"))
      builder.setBalance("test-account3", Month(2019, Month.DEC), Balance.confirmed(10, "2019-12-01"))
      builder.addTransfer(
        TransferData(
          fromAccount = "test-account1",
          toAccount = "test-account2",
          toMonth = Month(2019, Month.DEC),
          fromMonth = Month(2019, Month.DEC),
          balance = Balance.projected(2000, "2019-12-05"),
          description = "First transfer",
        )
      )

      builder.addTransfer(
        TransferData(
          fromAccount = "test-account1",
          toAccount = "test-account3",
          toMonth = Month.fromString("Dec2019"),
          fromMonth = Month.fromString("Dec2019"),
          balance = Balance.projected(1000, "2019-12-05"),
          description = "Second transfer",
        )
      )
      val table = buildTransactionStatementTable(builder.build(), owner = null)
      table.size shouldBe 3  // 3 accounts
      table[0].transactions.size shouldBe 2  // 2 transactions for account1
      assertSoftly {
        table[0].transactions[0].balance.amount shouldBe -2000.0
        table[0].transactions[1].balance.amount shouldBe -1000.0
        table[0].transactions[0].type shouldBe Transaction.Type.TRANSFER
        table[0].transactions[1].type shouldBe Transaction.Type.EXPENSE
      }
      table[1].transactions.size shouldBe 1  // 1 transaction for account2
      table[1].transactions[0].type shouldBe Transaction.Type.TRANSFER
      table[2].transactions.size shouldBe 1  // 1 transaction for account3
      table[2].transactions[0].type shouldBe Transaction.Type.INCOME
    }
  }
})