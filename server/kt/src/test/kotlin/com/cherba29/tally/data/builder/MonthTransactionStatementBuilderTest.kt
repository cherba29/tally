package com.cherba29.tally.data.builder

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.MonthName.DEC
import com.cherba29.tally.core.MonthName.FEB
import com.cherba29.tally.core.MonthName.JAN
import com.cherba29.tally.core.MonthName.JUL
import com.cherba29.tally.core.MonthName.JUN
import com.cherba29.tally.core.MonthName.NOV
import com.cherba29.tally.core.TreeNode
import com.cherba29.tally.core.root
import com.cherba29.tally.core.Transaction
import com.cherba29.tally.testing.toSnapshot
import com.diffplug.selfie.coroutines.expectSelfie
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class MonthTransactionStatementBuilderTest : DescribeSpec({
  describe("Creation") {
    it("empty") {
      val testMonths = JUN / 2026..JUL / 2027
      val builder = MonthTransactionStatementBuilder()
      builder.months = testMonths
      val transactionStatements = builder.build()
      transactionStatements.keys shouldBe testMonths.toSet()
    }
  }
  describe("Build") {
    it("single account no transfers") {
      val accountPath = listOf("john", "external", "test-account")
      val account = Account(
        name = "test-account",
        path = listOf("external"),
        owners = setOf(),
        openedOn = DEC / 2019
      )
      val budget = budget {
        setAccount(accountPath, account)
      }
      val builder = MonthTransactionStatementBuilder()
      builder.months = budget.months
      val table = builder.build()
      table.size shouldBe 1
      val stmt = table[(DEC / 2019)]!!
      assertSoftly {
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
      }
    }

    it("closed internal accounts do not produce statements") {
      val months = JUN / 2020 .. JUN / 2020
      val builder = MonthTransactionStatementBuilder()
      builder.months = months
      val table = builder.build()
      table.size shouldBe 1
    }

    it("closed external accounts still produce statements") {
      val months = JUN / 2020 .. JUN / 2020
      val builder = MonthTransactionStatementBuilder()
      builder.months = months
      val table = builder.build()
      table.keys shouldBe setOf(JUN / 2020)
    }

    it("two accounts with common owner and transfers") {
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
      val node1 = tree[path1]!! as TreeNode.Leaf
      val node2 = tree[path2]!! as TreeNode.Leaf

      val months = DEC / 2019..FEB / 2020
      val builder1 = MonthTransactionStatementBuilder()
      builder1.months = months
      builder1.monthlyBalances = mapOf(
        DEC / 2019 to Balance.confirmed(10, "2019-12-01"),
        JAN / 2020 to Balance.confirmed(20, "2020-01-01"),
        FEB / 2020 to Balance.projected(30, "2020-02-01")
      )
      builder1.addTransfer(
        node1,
        node2,
        DEC/2019,
        Balance.projected(-2000, "2019-12-05"),
        "First transfer"
      )
      builder1.addTransfer(
        node1,
        node2,
        DEC/2019,
        Balance.projected(-1000, "2019-12-05"),
        "Second transfer"
      )
      val table1 = builder1.build()
      table1.size shouldBe 3

      val builder2 = MonthTransactionStatementBuilder()
      builder2.months = months
      builder2.monthlyBalances = mapOf()
      builder2.addTransfer(
        node2,
        node1,
        DEC/2019,
        Balance.projected(2000, "2019-12-05"),
        "First transfer"
      )
      builder2.addTransfer(
        node2,
        node1,
        DEC/2019,
        Balance.projected(1000, "2019-12-05"),
        "Second transfer"
      )
      val table2 = builder2.build()
      table2.size shouldBe 3

      expectSelfie(table1.toSnapshot { false }).toMatchDisk("table1")
      expectSelfie(table2.toSnapshot { false }).toMatchDisk("table2")
    }

    it("two accounts with external transfer") {
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
      val months = DEC / 2019..FEB / 2020
      val builder1 = MonthTransactionStatementBuilder()
      builder1.months = months
      builder1.monthlyBalances = mapOf(
        DEC / 2019 to Balance.confirmed(10, "2019-12-01"),
        JAN / 2020 to Balance.confirmed(20, "2020-01-01"),
        FEB / 2020 to Balance.projected(30, "2020-02-01")
      )
      builder1.addTransfer(
        node1,
        node2,
        DEC / 2019,
        Balance.projected(-2000, "2019-12-05"),
        "First transfer"
      )
      builder1.addTransfer(
        node1,
        node2,
        DEC / 2019,
        Balance.projected(-1000, "2019-12-05"),
        "Second transfer"
      )
      val table1 = builder1.build()
      table1.size shouldBe 3

      val builder2 = MonthTransactionStatementBuilder()
      builder2.months = months
      builder2.addTransfer(
        node2,
        node1,
        DEC / 2019,
        Balance.projected(2000, "2019-12-05"),
        "First transfer"
      )
      builder2.addTransfer(
        node2,
        node1,
        DEC / 2019,
        Balance.projected(1000, "2019-12-05"),
        "Second transfer"
      )

      val table2 = builder2.build()
      table1.size shouldBe 3

      expectSelfie(table1.toSnapshot { false }).toMatchDisk("table1")
      expectSelfie(table2.toSnapshot { false }).toMatchDisk("table2")
    }
    
    it("transfer to closed account") {
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

      val months = NOV / 2019..DEC / 2019
      val builder1 = MonthTransactionStatementBuilder()
      builder1.months = months
      builder1.monthlyBalances = mapOf(
        DEC / 2019 to Balance.confirmed(10, "2019-12-01")
      )
      builder1.addTransfer(
        node1,
        node2,
        DEC / 2019,
        Balance.projected(2000, "2019-12-05"),
        "First transfer"
      )

      val table1 = builder1.build()
      // Two transaction statements for the account
      table1.size shouldBe 2

      val builder2 = MonthTransactionStatementBuilder()
      builder2.months = months
      builder2.addTransfer(
        node2,
        node1,
        DEC / 2019,
        Balance.projected(-2000, "2019-12-05"),
        "First transfer"
      )

      val table2 = builder2.build()
      // Two transaction statements for the account
      table2.size shouldBe 2

      val dec1Stmt = table1[DEC / 2019]!!
      dec1Stmt.monthRange shouldBe DEC / 2019..DEC / 2019

      val nov1Stmt = table1[NOV / 2019]!!
      nov1Stmt.monthRange shouldBe NOV / 2019..NOV / 2019

      val dec2Stmt = table2[DEC / 2019]!!
      dec2Stmt.monthRange shouldBe DEC / 2019..DEC / 2019

      val nov2Stmt = table2[NOV / 2019]!!
      nov2Stmt.monthRange shouldBe NOV / 2019..NOV / 2019
    }

    it("get transaction type") {
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

      val transfer1to2 = Transaction(
        targetTreeNode = node2,
        description = "First transfer",
        balance = Balance.projected(-2000, "2019-12-05"),
        type = Transaction.Type.TRANSFER
      )
      val transfer1to3 = Transaction(
        targetTreeNode = node3,
        description = "Second transfer",
        balance = Balance.projected(-1000, "2019-12-05"),
        type = Transaction.Type.EXPENSE
      )
      val months = DEC / 2019..DEC / 2019
      val builder1 = MonthTransactionStatementBuilder()
      builder1.months = months
      builder1.monthlyBalances = mapOf(DEC / 2019 to Balance.confirmed(10, "2019-12-01"))
      builder1.addTransfer(
        node1,
        node2,
        DEC / 2019,
        Balance.projected(-2000, "2019-12-05"),
        "First transfer"
      )
      builder1.addTransfer(
        node1,
        node3,
        DEC / 2019,
        Balance.projected(-1000, "2019-12-05"),
        "Second transfer"
      )

      val table1 = builder1.build()
      table1.size shouldBe 1

      val stmt1 = table1[DEC / 2019]!!
      stmt1.transactions.size shouldBe 2 // 2 transactions for account1
      assertSoftly {
        stmt1.transactions[0] shouldBe transfer1to3
        stmt1.transactions[1] shouldBe transfer1to2
      }

      val builder2 = MonthTransactionStatementBuilder()
      builder2.months = months
      builder2.monthlyBalances = mapOf(DEC / 2019 to Balance.confirmed(10, "2019-12-01"))
      builder2.addTransfer(
        node2,
        node1,
        DEC / 2019,
        Balance.projected(-2000, "2019-12-05"),
        "First transfer"
      )
      val table2 = builder2.build()
      table2.size shouldBe 1

      val stmt2 = table2[DEC / 2019]!!
      stmt2.transactions.size shouldBe 1 // 1 transaction for account2
      stmt2.transactions[0].type shouldBe Transaction.Type.TRANSFER

      val builder3 = MonthTransactionStatementBuilder()
      builder3.months = months
      builder3.monthlyBalances = mapOf(DEC / 2019 to Balance.confirmed(10, "2019-12-01"))
      builder3.addTransfer(
        node3,
        node1,
        DEC / 2019,
        Balance.projected(-1000, "2019-12-05"),
        "Second transfer"
      )

      val table3 = builder3.build()
      table3.size shouldBe 1

      val stmt3 = table3[DEC / 2019]!!
      stmt3.transactions.size shouldBe 1 // 1 transaction for account3
      stmt3.transactions[0] shouldBe transfer1to3.copy(targetTreeNode = node1)
    }
  }
})
