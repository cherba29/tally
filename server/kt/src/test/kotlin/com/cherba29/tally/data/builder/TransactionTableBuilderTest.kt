package com.cherba29.tally.data.builder

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.MonthName.DEC
import com.cherba29.tally.core.MonthName.FEB
import com.cherba29.tally.core.MonthName.JAN
import com.cherba29.tally.core.MonthName.JUL
import com.cherba29.tally.core.MonthName.JUN
import com.cherba29.tally.core.MonthName.NOV
import com.cherba29.tally.core.Transfer
import com.cherba29.tally.core.TreeNode
import com.cherba29.tally.core.root
import com.cherba29.tally.statement.Transaction
import com.cherba29.tally.testing.toSnapshot
import com.diffplug.selfie.coroutines.expectSelfie
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class TransactionTableBuilderTest : DescribeSpec({
  describe("Creation") {
    it("empty") {
      val builder = TransactionTableBuilder()
      val testMonths = JUN / 2026 .. JUL / 2027
      val transactionStatements = builder.buildTransactionStatementTable(
        months = testMonths,
        leafToAccountMap = mapOf(),
        leafToMonthlyBalancesMap = mapOf(),
        leafToMonthlyTransfersMap = mapOf()
      )
      transactionStatements shouldBe listOf()
    }
  }
  describe("Build") {
    it("no months") {
      val builder = TransactionTableBuilder()
      val exception = shouldThrow<IllegalArgumentException> {
        builder.buildTransactionStatementTable(
          months = DEC / 2019..NOV / 2019,
          leafToAccountMap = mapOf(),
          leafToMonthlyBalancesMap = mapOf(),
          leafToMonthlyTransfersMap = mapOf()
        )
      }
      exception.message shouldBe "Budget must have at least one month."
    }

    it("single account no transfers") {
      val accountPath = listOf("john", "external", "test-account")
      val account = Account(name = "test-account", path = listOf("external"), owners = setOf(), openedOn = DEC / 2019)
      val budget = budget {
        setAccount(accountPath, account)
      }
      val builder = TransactionTableBuilder()
      val table = builder.buildTransactionStatementTable(
        months = budget.months,
        budget.leafToAccount,
        leafToMonthlyBalancesMap = mapOf(),
        leafToMonthlyTransfersMap = mapOf()
      )
      table.size shouldBe 1
      val stmt = table.first()
      assertSoftly {
        stmt.treeNode.path shouldBe accountPath
        stmt.coversPrevious shouldBe false
        stmt.coversProjectedPrevious shouldBe false
        stmt.endBalance shouldBe null
        stmt.hasProjectedTransfer shouldBe false
        stmt.inFlows shouldBe 0L
        stmt.income shouldBe 0L
        stmt.isCovered shouldBe true
        stmt.isProjectedCovered shouldBe true
        stmt.monthRange shouldBe DEC / 2019..DEC / 2019
        stmt.outFlows shouldBe 0L
        stmt.startBalance shouldBe null
        stmt.totalPayments shouldBe 0L
        stmt.totalTransfers shouldBe 0L
        stmt.transactions shouldBe listOf()
        stmt.isClosed shouldBe false
      }
    }

    it("two accounts with common owner and transfers") {
      val account1 = Account(
        name = "test-account1",
        path = listOf("external"),
        owners = setOf("john"),
        openedOn = DEC / 2019
      )

      val account2 = Account(
        name = "test-account2",
        path = listOf("external"),
        owners = setOf("john"),
        openedOn = DEC / 2019
      )
      val tree = root {
        branch("john") {
          branch("external") {
            leaf("test-account1")
            leaf("test-account2")
          }
        }
      }
      val path1 = listOf("john", "external", "test-account1")
      val path2 = listOf("john", "external", "test-account2")
      val accounts = mapOf(
        tree[path1]!! as TreeNode.Leaf to account1,
        tree[path2]!! as TreeNode.Leaf to account2,
      )

      val balances = mapOf(
        tree[path1]!! as TreeNode.Leaf to mapOf(
          DEC / 2019 to Balance.confirmed(10, "2019-12-01"),
          JAN / 2020 to Balance.confirmed(20, "2020-01-01"),
          FEB / 2020 to Balance.projected(30, "2020-02-01")
        )
      )
      val firstTransfer1to2 = Transfer(
        fromAccount = tree[path1] as TreeNode.Leaf,
        toAccount = tree[path2] as TreeNode.Leaf,
        fromMonth = DEC / 2019,
        toMonth = DEC / 2019,
        description = "First transfer",
        balance = Balance.projected(2000, "2019-12-05")
      )
      val secondTransfer1to2 = Transfer(
        fromAccount = tree[path1] as TreeNode.Leaf,
        toAccount = tree[path2] as TreeNode.Leaf,
        fromMonth = DEC / 2019,
        toMonth = DEC / 2019,
        description = "Second transfer",
        balance = Balance.projected(1000, "2019-12-05")
      )
      val transfers = mapOf(
        tree[path1]!! as TreeNode.Leaf to mapOf(DEC / 2019 to listOf(firstTransfer1to2, secondTransfer1to2)),
        tree[path2]!! as TreeNode.Leaf to mapOf(DEC / 2019 to listOf(firstTransfer1to2, secondTransfer1to2)),
      )
      val builder = TransactionTableBuilder()
      val table = builder.buildTransactionStatementTable(
        DEC / 2019..FEB / 2020,
        accounts,
        balances,
        transfers
      )
      table.size shouldBe 6

      expectSelfie(table.toSnapshot()).toMatchDisk()
    }

    it("two accounts with external transfer") {
      val account1 = Account(name = "test-account1",
        path = listOf("external"), owners = setOf("john"), openedOn = DEC / 2019)
      val account2 = Account(name = "test-account2",
        path = listOf("external"),
        owners = setOf("john"), openedOn = DEC / 2019)
      val tree = root {
        branch("john") {
          branch("external") {
            leaf("test-account1")
            leaf("test-account2")
          }
        }
      }
      val node1 = tree[listOf("john", "external", "test-account1")] as TreeNode.Leaf
      val node2 = tree[listOf("john", "external", "test-account2")] as TreeNode.Leaf
      val accounts = mapOf(
        node1 to account1,
        node2 to account2
      )
      val balances = mapOf(
        node1 to mapOf(
          DEC / 2019 to Balance.confirmed(10, "2019-12-01"),
          JAN / 2020 to Balance.confirmed(20, "2020-01-01"),
          FEB / 2020 to Balance.projected(30, "2020-02-01")
        )
      )
      val firstTransfer1to2 = Transfer(
        fromAccount = node1,
        toAccount = node2,
        fromMonth = DEC / 2019,
        toMonth = DEC / 2019,
        description = "First transfer",
        balance = Balance.projected(2000, "2019-12-05")
      )
      val secondTransfer1to2 = Transfer(
        fromAccount = node1,
        toAccount = node2,
        fromMonth = DEC / 2019,
        toMonth = DEC / 2019,
        description = "Second transfer",
        balance = Balance.projected(1000, "2019-12-05")
      )

      val transfers = mapOf(
        node1 to mapOf(DEC / 2019 to listOf(firstTransfer1to2, secondTransfer1to2)),
        node2 to mapOf(DEC / 2019 to listOf(firstTransfer1to2, secondTransfer1to2)),
      )
      val builder = TransactionTableBuilder()
      val table = builder.buildTransactionStatementTable(
        DEC / 2019..FEB / 2020,
        accounts,
        balances,
        transfers
      )
      table.size shouldBe 6

      expectSelfie(table.toSnapshot()).toMatchDisk()
    }

    it("transfer to closed account") {
      val account1 = Account(
        name = "test-account1",
        path = listOf("external"),
        owners = setOf("john"),
        openedOn = NOV / 2019,
        closedOn = NOV / 2019  // closed before TransactionStatement month
      )
      val account2 = Account(
        name = "external",
        path = listOf("external"),
        owners = setOf("john"),
        openedOn = NOV / 2019,
      )
      val tree = root {
        branch("john") {
          branch("external") {
            leaf("external")
            leaf("test-account1")
          }
        }
      }
      val node1 = tree[listOf("john", "external", "test-account1")] as TreeNode.Leaf
      val node2 = tree[listOf("john", "external", "external")] as TreeNode.Leaf
      val accounts = mapOf(
        node1 to account1,
        node2 to account2
      )

      val balances = mapOf(
        node1 to mapOf(
          DEC / 2019 to Balance.confirmed(10, "2019-12-01"),
        )
      )
      val transfers = mapOf(
        node1 to mapOf(
          DEC / 2019 to listOf(
            Transfer(
              fromAccount = node1,
              toAccount = node2,
              fromMonth = DEC / 2019,
              toMonth = DEC / 2019,
              description = "First transfer",
              balance = Balance.projected(2000, "2019-12-05")
            ),
          )
        )
      )

      val builder = TransactionTableBuilder()
      val table = builder.buildTransactionStatementTable(
        NOV / 2019..DEC / 2019,
        accounts,
        balances,
        transfers
      )
      table.size shouldBe 4  // Two transaction statements for the account
      table[0].monthRange shouldBe DEC / 2019..DEC / 2019
      table[0].isClosed shouldBe true
      table[0].treeNode.path shouldBe node1.path
      table[1].monthRange shouldBe NOV / 2019..NOV / 2019
      table[1].isClosed shouldBe false
      table[1].treeNode.path shouldBe node1.path
      table[2].monthRange shouldBe DEC / 2019..DEC / 2019
      table[2].isClosed shouldBe false
      table[2].treeNode.path shouldBe node2.path
      table[3].monthRange shouldBe NOV / 2019..NOV / 2019
      table[3].isClosed shouldBe false
      table[3].treeNode.path shouldBe node2.path
    }

    it("get transaction type") {
      val account1 = Account(name = "test-account1",
        path = listOf("internal", "checking"),
        owners = setOf("john"), openedOn = DEC / 2019)
      val account2 = Account(name = "test-account2",
        path = listOf("internal", "credit"),
        owners = setOf("john"), openedOn = DEC / 2019)
      val account3 = Account(name = "test-account3",
        path = listOf("external", "expense"),
        owners = setOf("john"), openedOn = DEC / 2019)

      val tree = root {
        branch("john") {
          branch("internal") {
            branch("checking") {
              leaf("test-account1")
            }
            branch("credit") {
              leaf("test-account2")
            }
          }
          branch("external") {
            branch("expense") {
              leaf("test-account3")
            }
          }
        }
      }
      val path1 = listOf("john", "internal", "checking", "test-account1")
      val path2 = listOf("john", "internal", "credit", "test-account2")
      val path3 = listOf("john", "external", "expense", "test-account3")
      val node1 = tree[path1]!! as TreeNode.Leaf
      val node2 = tree[path2]!! as TreeNode.Leaf
      val node3 = tree[path3]!! as TreeNode.Leaf
      val accounts = mapOf(node1 to account1, node2 to account2, node3 to account3)

      val balances = mapOf(
        node1 to mapOf(DEC / 2019 to Balance.confirmed(10, "2019-12-01")),
        node2 to mapOf(DEC / 2019 to Balance.confirmed(10, "2019-12-01")),
        node3 to mapOf(DEC / 2019 to Balance.confirmed(10, "2019-12-01")),
      )
      val transfer1to2 = Transfer(
        fromAccount = node1,
        toAccount = node2,
        fromMonth = DEC / 2019,
        toMonth = DEC / 2019,
        description = "First transfer",
        balance = Balance.projected(2000, "2019-12-05")
      )
      val transfer1to3 = Transfer(
        fromAccount = node1,
        toAccount = node3,
        fromMonth = DEC / 2019,
        toMonth = DEC / 2019,
        description = "Second transfer",
        balance = Balance.projected(1000, "2019-12-05")
      )
      val transfers = mapOf(
        node1 to mapOf(DEC / 2019 to listOf(transfer1to2, transfer1to3)),
        node2 to mapOf(DEC / 2019 to listOf(transfer1to2)),
        node3 to mapOf(DEC / 2019 to listOf(transfer1to3)),
      )

      val builder = TransactionTableBuilder()
      val table = builder.buildTransactionStatementTable(
        DEC / 2019..DEC / 2019,
        accounts,
        balances,
        transfers
      )
      table.size shouldBe 3  // 3 accounts
      table[0].treeNode.path shouldBe path1
      table[0].transactions.size shouldBe 2  // 2 transactions for account1
      assertSoftly {
        table[0].transactions[0].balance.amount shouldBe -2000L
        table[0].transactions[1].balance.amount shouldBe -1000L
        table[0].transactions[0].type shouldBe Transaction.Type.TRANSFER
        table[0].transactions[1].type shouldBe Transaction.Type.EXPENSE
      }
      table[1].treeNode.path shouldBe path2
      table[1].transactions.size shouldBe 1  // 1 transaction for account2
      table[1].transactions[0].type shouldBe Transaction.Type.TRANSFER
      table[2].treeNode.path shouldBe path3
      table[2].transactions.size shouldBe 1  // 1 transaction for account3
      table[2].transactions[0].type shouldBe Transaction.Type.INCOME
    }
  }
})