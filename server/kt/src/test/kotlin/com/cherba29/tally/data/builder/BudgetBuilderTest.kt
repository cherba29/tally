package com.cherba29.tally.data.builder

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.MonthName.APR
import com.cherba29.tally.core.MonthName.DEC
import com.cherba29.tally.core.MonthName.FEB
import com.cherba29.tally.core.MonthName.JAN
import com.cherba29.tally.core.MonthName.MAR
import com.cherba29.tally.core.MonthName.NOV
import com.cherba29.tally.utils.TreeNode
import com.cherba29.tally.utils.root
import com.cherba29.tally.data.Profile
import com.cherba29.tally.statement.SummaryStatement
import com.cherba29.tally.statement.TransactionStatement
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate

class BudgetBuilderTest : DescribeSpec({
  it("build empty budget") {
    val error = shouldThrow<IllegalArgumentException> { budget {} }
    error.message shouldBe "Budget must have at least one month."
  }

  it("build simple") {
    val account1 = Account("test-account1", path = listOf("internal"), owners = setOf("john"), openedOn = NOV / 2019)
    val account2 = Account("test-account2", path = listOf("internal"), owners = setOf("john"), openedOn = NOV / 2019)
    val account3 = Account("test-account3", path = listOf("internal"), owners = setOf("john"), openedOn = NOV / 2019)
    val budget = budget {
      setAccount(listOf("john", "internal", "test-account1"), account1)
      setAccount(listOf("john", "internal", "test-account2"), account2)
      setAccount(listOf("john", "internal", "test-account3"), account3)
      setBalance(
        listOf("john", "internal", "test-account1"),
        NOV / 2019,
        Balance(100, LocalDate(2019, 11, 1), Balance.Type.PROJECTED)
      )
      setBalance(
        listOf("john", "internal", "test-account1"),
        DEC / 2019,
        Balance(200, LocalDate(2019, 12, 1), Balance.Type.PROJECTED)
      )
      setBalance(
        listOf("john", "internal", "test-account2"),
        NOV / 2019,
        Balance(200, LocalDate(2019, 11, 2), Balance.Type.CONFIRMED)
      )
      addTransfer(
        BudgetBuilder.TransferRecord(
          toAccountName = "test-account1",
          fromAccountPath = listOf("john", "internal", "test-account2"),
          month = NOV / 2019,
          balance = Balance(50, LocalDate(2019, 11, 2), Balance.Type.CONFIRMED),
          description = null,
          tags = listOf()
        )
      )
      addTransfer(
        BudgetBuilder.TransferRecord(
          toAccountName = "test-account3",
          fromAccountPath = listOf("john", "internal", "test-account2"),
          month = NOV / 2019,
          balance = Balance(70, LocalDate(2019, 11, 2), Balance.Type.CONFIRMED),
          description = null,
          tags = listOf()
        )
      )
    }
    budget.leafToAccount.size shouldBe 3
    budget.leafToAccount[budget.tree[listOf("john", "internal", "test-account1")]] shouldBe account1
    budget.leafToAccount[budget.tree[listOf("john", "internal", "test-account2")]] shouldBe account2
    budget.nodeToStatement.filter { it.key.children.isEmpty() }.size shouldBe 3
    val numberOfStatementsWithBalances = budget.nodeToStatement.values.sumOf {
      it.values.count { s ->
        (s as? TransactionStatement)?.startBalance != null
      }
    }
    numberOfStatementsWithBalances shouldBe 3
    budget.nodeToStatement.values.sumOf {
      it.values.sumOf { s ->
        (s as? TransactionStatement)?.transactions?.size ?: 0
      }
    } shouldBe 4
    budget.months shouldBe NOV / 2019..DEC / 2019
    budget.tree shouldBe root(Profile()) {
      branch("john", Profile()) {
        branch("internal", Profile()) {
          leaf("test-account1", Profile())
          leaf("test-account2", Profile())
          leaf("test-account3", Profile())
        }
      }
    }
    budget.nodeToStatement.size shouldBe 5
    budget.nodeToStatement.keys shouldBe setOf(
      budget.tree[listOf("john")],
      budget.tree[listOf("john", "internal")],
      budget.tree[listOf("john", "internal", "test-account1")],
      budget.tree[listOf("john", "internal", "test-account2")],
      budget.tree[listOf("john", "internal", "test-account3")]
    )
  }

  it("build ambiguous account") {
    val path1 = listOf("bob", "test-account1")
    val account1 = Account("test-account1", path = listOf(), owners = setOf("bob"), openedOn = NOV / 2019)
    val path2 = listOf("alice", "test-account1")
    val account2 = Account("test-account1", path = listOf(), owners = setOf("alice"), openedOn = NOV / 2019)
    val exception = shouldThrow<IllegalArgumentException> {
      budget {
        setAccount(path1, account1)
        setAccount(path2, account2)
        setBalance(
          path1,
          NOV / 2019,
          Balance(100, LocalDate(2019, 11, 1), Balance.Type.PROJECTED)
        )
        setBalance(
          path1,
          DEC / 2019,
          Balance(200, LocalDate(2019, 12, 1), Balance.Type.PROJECTED)
        )
        setBalance(
          path2,
          NOV / 2019,
          Balance(200, LocalDate(2019, 11, 3), Balance.Type.CONFIRMED)
        )
        addTransfer(
          BudgetBuilder.TransferRecord(
            toAccountName = "test-account1",
            fromAccountPath = path2,
            month = NOV / 2019,
            balance = Balance(50, LocalDate(2019, 11, 2), Balance.Type.CONFIRMED),
            description = null,
            tags = listOf()
          )
        )
      }
    }
    exception.message shouldBe "Ambiguous transfer from alice/test-account1 to test-account1, " +
      "found multiple candidate accounts bob/test-account1, alice/test-account1"
  }

  it("build budget - duplicate balance") {
    val builder = BudgetBuilder()
    val path1 = listOf("bob", "internal", "test-account1")
    val account1 = Account("test-account1", path = listOf("internal"), owners = setOf(), openedOn = NOV / 2019)
    builder.setAccount(path1, account1)
    builder.setBalance(
      path1,
      NOV / 2019,
      Balance(10000, LocalDate(2019, 11, 1), Balance.Type.PROJECTED)
    )
    val exception = shouldThrow<IllegalArgumentException> {
      builder.setBalance(
        path1,
        NOV / 2019,
        Balance(20000, LocalDate(2020, 3, 1), Balance.Type.PROJECTED)
      )
    }
    exception.message shouldBe "Balance for 'bob/internal/test-account1' 'Nov2019' is already set to" +
      " Balance { amount: 200.00, date: 2020-03-01, type: PROJECTED }"
  }

  it("build budget - bad to account") {
    val path2 = listOf("bob", "test-account2")
    val account2 = Account("test-account2", path = listOf(), owners = setOf(), openedOn = NOV / 2019)
    val exception = shouldThrow<IllegalArgumentException> {
      budget {
        setAccount(path2, account2)
        addTransfer(
          BudgetBuilder.TransferRecord(
            toAccountName = "test-account1",
            fromAccountPath = path2,
            month = NOV / 2019,
            balance = Balance(50, LocalDate(2019, 12, 2), Balance.Type.CONFIRMED),
            description = null,
            tags = listOf()
          )
        )
      }
    }
    exception.message shouldBe
      """
      Unknown to account test-account1 in bob/test-account2, known accounts
      └── 
          └── bob
              └── test-account2

      """.trimIndent()
  }

  it("build budget - bad from account") {
    val path1 = listOf("bob", "test-account1")
    val account1 = Account("test-account1", path = listOf(), owners = setOf(), openedOn = NOV / 2019)
    val path2 = listOf("bob", "external", "test-account2")
    val exception = shouldThrow<IllegalArgumentException> {
      budget {
        setAccount(path1, account1)
        addTransfer(
          BudgetBuilder.TransferRecord(
            toAccountName = "test-account1",
            fromAccountPath = path2,
            month = NOV / 2019,
            balance = Balance(50, LocalDate(2019, 11, 2), Balance.Type.CONFIRMED),
            description = null,
            tags = listOf()
          )
        )
      }
    }
    exception.message shouldBe "Unknown account bob/external/test-account2"
  }

  describe("findActive accounts") {
    it("open account") {
      val path1 = listOf("bob", "internal", "test-account1")
      val account1 = Account("test-account1", path = listOf("internal"), owners = setOf(), openedOn = APR / 2026)
      val budget = budget {
        setAccount(path1, account1)
      }
      budget.leafToAccount shouldBe mapOf(budget.tree[listOf("bob", "internal", "test-account1")] to account1)
    }

    it("multiple accounts") {
      val path1 = listOf("bob", "test-account1")
      val account1 = Account("test-account1", path = listOf(), owners = setOf(), openedOn = APR / 2026)
      val path2 = listOf("bob", "test-account2")
      val account2 = Account("test-account2", path = listOf(), owners = setOf(), openedOn = NOV / 2019)
      val path3 = listOf("bob", "test-account3")
      val account3 =
        Account("test-account3", path = listOf(), owners = setOf(), openedOn = JAN / 2020, closedOn = FEB / 2020)
      val budget = budget {
        setAccount(path1, account1)
        setAccount(path2, account2)
        setAccount(path3, account3)
      }
      budget.leafToAccount.size shouldBe 3
      budget.leafToAccount.values shouldBe listOf(account1, account2, account3)
    }
  }

  describe("transaction statement table") {
    describe("Build") {
      it("bad account name on transfer") {
        val path1 = listOf("john", "external", "test-account1")
        val account1 = Account(
          name = "test-account1",
          path = listOf("external"),
          owners = setOf("john"),
          openedOn = DEC / 2021
        )
        val exception =
          shouldThrow<IllegalArgumentException> {
            budget {
              setAccount(path1, account1)
              addTransfer(
                BudgetBuilder.TransferRecord(
                  fromAccountPath = path1,
                  toAccountName = "test-account2",
                  month = DEC / 2019,
                  balance = Balance.projected(2000, "2019-12-05"),
                  description = "First transfer",
                  tags = listOf()
                )
              )
            }
          }
        exception.message shouldBe
          """
          Unknown to account test-account2 in john/external/test-account1, known accounts
          └── 
              └── john
                  └── external
                      └── test-account1

          """.trimIndent()
      }

      it("transfer with date before start balance") {
        val path1 = listOf("john", "external", "test-account1")
        val account1 = Account(
          name = "test-account1",
          path = listOf("external"),
          owners = setOf("john"),
          openedOn = DEC / 2019
        )
        val exception =
          shouldThrow<IllegalStateException> {
            budget {
              setAccount(path1, account1)
              setBalance(path1, DEC / 2019, Balance.confirmed(1000, "2019-12-01"))
              addTransfer(
                BudgetBuilder.TransferRecord(
                  fromAccountPath = path1,
                  toAccountName = "test-account1",
                  month = DEC / 2019,
                  balance = Balance.projected(2000, "2019-11-25"),
                  description = "First transfer",
                  tags = listOf()
                )
              )
            }
          }
        exception.message shouldBe "Dec2019 Balance { amount: 10.00, date: 2019-12-01, type: CONFIRMED } " +
            "starts after its first transfer to john/external/test-account1 " +
            "for amount of Balance { amount: -20.00, date: 2019-11-25, type: PROJECTED } desc 'First transfer'"
      }
      it("transfer during closed month") {
        val path1 = listOf("john", "external", "test-account1")
        val account1 = Account(
          name = "test-account1",
          path = listOf("external"),
          owners = setOf("john"),
          openedOn = DEC / 2021
        )
        val path2 = listOf("john", "external", "test-account2")
        val account2 = Account(
          name = "test-account2",
          path = listOf("external"),
          owners = setOf("john"),
          openedOn = DEC / 2021
        )
        val exception =
          shouldThrow<IllegalArgumentException> {
            budget {
              setAccount(path1, account1)
              setAccount(path2, account2)
              setBalance(path1, DEC / 2019, Balance.confirmed(1000, "2019-12-01"))
              addTransfer(
                BudgetBuilder.TransferRecord(
                  fromAccountPath = path1,
                  toAccountName = "test-account2",
                  month = DEC / 2019,
                  balance = Balance.projected(2000, "2019-11-25"),
                  description = "First transfer",
                  tags = listOf()
                )
              )
            }
          }
        exception.message shouldBe "Account john/external/test-account1 has transfer during closed month Dec2019 to john/external/test-account2"
      }
    }
  }
  describe("buildSummaryStatementTable") {
    it("single closed account - produces summary without it") {
      val path1 = listOf("john", "external", "test-account1")
      val account1 = Account(
        name = "test-account1",
        path = listOf("external"),
        owners = setOf("john"),
        openedOn = MAR / 2021
      )
      val testStartBalance = Balance(
        100,
        LocalDate(2023, 12, 2),
        Balance.Type.CONFIRMED
      )
      val budget = budget {
        setAccount(path1, account1)
        setBalance(path1, MAR / 2021, testStartBalance)
      }
      val node1 = budget.tree[path1] as TreeNode.Leaf
      val tranStmt = transactionStatement {
        month = MAR / 2021
        startBalance = testStartBalance
        // There no other transactions and balance is positive.
        isCovered = true
        isProjectedCovered = true
      }
      budget.tree shouldBe root(Profile()) {
        branch("john", Profile()) {
          branch("external", Profile(isExternal = true)) {
            leaf("test-account1", Profile(isExternal = true))
          }
        }
      }
      val statements = budget.nodeToStatement
      withClue("should contain: $statements") {
        statements.isEmpty() shouldBe false
        statements.size shouldBe 3
        statements.keys shouldBe setOf(
          budget.tree[listOf("john", "external", "test-account1")],
          budget.tree[listOf("john", "external")],
          budget.tree[listOf("john")]
        )
      }
      val treeNode1 = budget.tree[listOf("john", "external")]!!
      val stmt1 = statements[treeNode1]!![MAR / 2021]!! as SummaryStatement
      withClue("statement: $stmt1") {
        stmt1.startBalance shouldBe testStartBalance
        stmt1.endBalance shouldBe null
        stmt1.inFlows shouldBe 0
        stmt1.income shouldBe 0
        stmt1.monthRange shouldBe MAR / 2021..MAR / 2021
        stmt1.outFlows shouldBe 0
        stmt1.statements.keys shouldBe setOf(node1)
        stmt1.statements[node1] shouldBe tranStmt
        stmt1.totalPayments shouldBe 0
        stmt1.totalTransfers shouldBe 0
      }

      val treeNode2 = budget.tree["john"]!!
      val stmt2 = statements[treeNode2]!![MAR / 2021]!! as SummaryStatement
      stmt2.startBalance shouldBe testStartBalance
      stmt2.endBalance shouldBe null
      stmt2.inFlows shouldBe 0
      stmt2.income shouldBe 0
      stmt2.monthRange shouldBe MAR / 2021..MAR / 2021
      stmt2.outFlows shouldBe 0
      stmt2.statements.keys shouldBe setOf(treeNode1)
      stmt2.statements[treeNode1] shouldBe stmt1
      stmt2.totalPayments shouldBe 0
      stmt2.totalTransfers shouldBe 0
    }

    it("single external account - no SUMMARY") {
      val path1 = listOf("john", "external", "test-account1")
      val account1 = Account(
        name = "test-account1",
        path = listOf("external"),
        owners = setOf("john"),
        openedOn = MAR / 2021
      )
      val balance1 = Balance(100, LocalDate(2023, 12, 2), Balance.Type.CONFIRMED)
      val budget = budget {
        setAccount(path1, account1)
        setBalance(path1, MAR / 2021, balance1)
      }

      budget.tree shouldBe root(Profile()) {
        branch("john", Profile()) {
          branch("external", Profile(isExternal = true)) {
            leaf("test-account1", Profile(isExternal = true))
          }
        }
      }
      val statements = budget.nodeToStatement
      statements.isEmpty() shouldBe false
      statements.keys shouldBe setOf(
        budget.tree[listOf("john", "external", "test-account1")],
        budget.tree[listOf("john", "external")],
        budget.tree[listOf("john")]
      )

      val treeNode = budget.tree["john"]
      val stmt = statements[treeNode]!![MAR / 2021]!! as SummaryStatement
      val externalTreeNode = budget.tree[listOf("john", "external")]
      stmt.startBalance shouldBe Balance(100, LocalDate(2023, 12, 2), Balance.Type.CONFIRMED)
      stmt.endBalance shouldBe null
      stmt.inFlows shouldBe 0
      stmt.income shouldBe 0
      stmt.monthRange shouldBe MAR / 2021..MAR / 2021
      stmt.outFlows shouldBe 0
      stmt.statements.keys shouldBe setOf(externalTreeNode)
      stmt.statements[externalTreeNode] shouldBe statements[externalTreeNode]!![MAR / 2021]!!
      stmt.totalPayments shouldBe 0
      stmt.totalTransfers shouldBe 0
    }

    it("single account - no transfers") {
      val path1 = listOf("john", "external", "test-account1")
      val account1 = Account(
        name = "test-account1",
        path = listOf("external"),
        owners = setOf("john"),
        openedOn = MAR / 2021
      )
      val balance1 = Balance(
        100,
        LocalDate(2023, 12, 2),
        Balance.Type.CONFIRMED
      )
      val budget = budget {
        setAccount(path1, account1)
        setBalance(path1, MAR / 2021, balance1)
      }
      budget.tree shouldBe root(Profile()) {
        branch("john", Profile()) {
          branch("external", Profile(isExternal = true)) {
            leaf("test-account1", Profile(isExternal = true))
          }
        }
      }
      val node1 = budget.tree[listOf("john", "external", "test-account1")] as TreeNode.Leaf
      val tranStmt = transactionStatement {
        month = MAR / 2021
        isCovered = true
        isProjectedCovered = true
        startBalance = balance1
      }

      val statements = budget.nodeToStatement
      statements.keys shouldBe setOf(
        budget.tree[listOf("john", "external", "test-account1")],
        budget.tree[listOf("john", "external")],
        budget.tree[listOf("john")]
      )

      val externalTreeNode = budget.tree[listOf("john", "external")]
      val stmt1 = statements[externalTreeNode]!![MAR / 2021]!! as SummaryStatement
      stmt1.startBalance shouldBe Balance(100, LocalDate(2023, 12, 2), Balance.Type.CONFIRMED)
      stmt1.endBalance shouldBe null
      stmt1.inFlows shouldBe 0
      stmt1.income shouldBe 0
      stmt1.monthRange shouldBe MAR / 2021..MAR / 2021
      stmt1.outFlows shouldBe 0
      stmt1.statements.keys shouldBe setOf(node1)
      stmt1.statements[node1] shouldBe tranStmt
      stmt1.totalPayments shouldBe 0
      stmt1.totalTransfers shouldBe 0

      val ownerTreeNode = budget.tree["john"]
      val stmt2 = statements[ownerTreeNode]!![MAR / 2021]!! as SummaryStatement
      stmt2.startBalance shouldBe Balance(100, LocalDate(2023, 12, 2), Balance.Type.CONFIRMED)
      stmt2.endBalance shouldBe null
      stmt2.inFlows shouldBe 0
      stmt2.income shouldBe 0
      stmt2.monthRange shouldBe MAR / 2021..MAR / 2021
      stmt2.outFlows shouldBe 0
      stmt2.statements.keys shouldBe setOf(externalTreeNode)
      stmt2.statements[externalTreeNode] shouldBe stmt1
      stmt2.totalPayments shouldBe 0
      stmt2.totalTransfers shouldBe 0
    }

    it("multiple accounts - selected owner") {
      val path1 = listOf("john", "external", "test-account1")
      val account1 = Account(
        name = "test-account1",
        path = listOf("external"),
        owners = setOf("john"),
        openedOn = MAR / 2021
      )

      val balance1 = Balance(
        100,
        LocalDate(2023, 12, 2),
        Balance.Type.CONFIRMED
      )
      // Should skip since different owner.
      val path2 = listOf("bob", "external", "test-account2")
      val account2 = Account(
        name = "test-account2",
        path = listOf("external"),
        owners = setOf("bob"),
        openedOn = MAR / 2021
      )
      val balance2 = Balance(
        300,
        LocalDate(2023, 12, 2),
        Balance.Type.CONFIRMED
      )
      // Should skip since path is empty.
      val path3 = listOf("john", "test-account3")
      val account3 = Account(
        name = "test-account3",
        path = listOf(),
        owners = setOf("john"),
        openedOn = MAR / 2021
      )
      val balance3 = Balance(
        500,
        LocalDate(2023, 12, 2),
        Balance.Type.CONFIRMED
      )
      val budget = budget {
        setAccount(path1, account1)
        setAccount(path2, account2)
        setAccount(path3, account3)
        setBalance(path1, MAR / 2021, balance1)
        setBalance(path2, MAR / 2021, balance2)
        setBalance(path3, MAR / 2021, balance3)
      }
      budget.tree shouldBe root(Profile()) {
        branch("bob", Profile()) {
          branch("external", Profile(isExternal = true)) {
            leaf("test-account2", Profile(isExternal = true))
          }
        }
        branch("john", Profile()) {
          branch("external", Profile(isExternal = true)) {
            leaf("test-account1", Profile(isExternal = true))
          }
          leaf("test-account3", Profile())
        }
      }
      val node = budget.tree[listOf("john", "external", "test-account1")] as TreeNode.Leaf
      val tranStmt1 = transactionStatement {
        month = MAR / 2021
        startBalance = balance1
        isCovered = true
        isProjectedCovered = true
      }

      val ownerTreeNode = budget.tree["john"]
      val statements = budget.nodeToStatement[ownerTreeNode]!!
      statements.keys shouldBe setOf(MAR / 2021)

      val externalTreeNode = budget.tree[listOf("john", "external")]
      val stmt1 = budget.nodeToStatement[externalTreeNode]!![MAR / 2021]!! as SummaryStatement
      stmt1.startBalance shouldBe Balance(100, LocalDate(2023, 12, 2), Balance.Type.CONFIRMED)
      stmt1.endBalance shouldBe null
      stmt1.inFlows shouldBe 0
      stmt1.income shouldBe 0
      stmt1.monthRange shouldBe MAR / 2021..MAR / 2021
      stmt1.outFlows shouldBe 0
      stmt1.statements.keys shouldBe setOf(node)
      stmt1.statements[node] shouldBe tranStmt1
      stmt1.totalPayments shouldBe 0
      stmt1.totalTransfers shouldBe 0

      val treeNode3 = budget.tree[listOf("john", "test-account3")]
      val stmt3 = budget.nodeToStatement[treeNode3]!![MAR / 2021]!! as TransactionStatement
      val stmt2 = statements[MAR / 2021]!! as SummaryStatement
      stmt2.startBalance shouldBe Balance(600, LocalDate(2023, 12, 2), Balance.Type.CONFIRMED)
      stmt2.endBalance shouldBe null
      stmt2.inFlows shouldBe 0
      stmt2.income shouldBe 0
      stmt2.monthRange shouldBe MAR / 2021..MAR / 2021
      stmt2.outFlows shouldBe 0
      stmt2.statements.keys shouldBe setOf(externalTreeNode, treeNode3)
      stmt2.statements[externalTreeNode] shouldBe stmt1
      stmt2.statements[treeNode3] shouldBe stmt3
      stmt2.totalPayments shouldBe 0
      stmt2.totalTransfers shouldBe 0
    }
  }
})
