package com.cherba29.tally.data.builder

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.Group
import com.cherba29.tally.core.MonthName.APR
import com.cherba29.tally.core.MonthName.DEC
import com.cherba29.tally.core.MonthName.FEB
import com.cherba29.tally.core.MonthName.JAN
import com.cherba29.tally.core.MonthName.MAR
import com.cherba29.tally.core.MonthName.NOV
import com.cherba29.tally.core.Transfer
import com.cherba29.tally.core.root
import com.cherba29.tally.data.yaml.toObjectNode
import com.cherba29.tally.statement.SummaryStatement
import com.cherba29.tally.statement.Transaction
import com.cherba29.tally.statement.TransactionStatement
import com.diffplug.selfie.coroutines.expectSelfie
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import io.kotest.assertions.assertSoftly
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
    val account1 = Account("test-account1", path = listOf("internal"), owners = setOf("john"), isSummary = false, openedOn = NOV / 2019)
    val account2 = Account("test-account2", path = listOf("internal"), owners = setOf("john"), isSummary = false, openedOn = NOV / 2019)
    val account3 = Account("test-account3", path = listOf("internal"), owners = setOf("john"), isSummary = false, openedOn = NOV / 2019)
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
        toAccountName = "test-account1",
        toMonth = NOV / 2019,
        fromAccountPath = listOf("john", "internal", "test-account2"),
        fromMonth = NOV / 2019,
        balance = Balance(50, LocalDate(2019, 11, 2), Balance.Type.CONFIRMED),
        description = null
      )
      addTransfer(
        toAccountName = "test-account3",
        toMonth = NOV / 2019,
        fromAccountPath = listOf("john", "internal", "test-account2"),
        fromMonth = NOV / 2019,
        balance = Balance(70, LocalDate(2019, 11, 2), Balance.Type.CONFIRMED),
        description = null
      )
    }
    budget.leafToAccount.size shouldBe 3
    budget.leafToAccount[budget.tree[listOf("john", "internal", "test-account1")]] shouldBe account1
    budget.leafToAccount[budget.tree[listOf("john", "internal", "test-account2")]] shouldBe account2
    budget.nodeToStatement.filter { it.key.children.isEmpty() }.size shouldBe 3
    budget.nodeToStatement.values.sumOf { it.values.count { s -> (s as? TransactionStatement)?.startBalance != null } } shouldBe 3
    budget.nodeToStatement.values.sumOf { it.values.sumOf { s -> (s as? TransactionStatement)?.transactions?.size ?: 0 } } shouldBe 4
    budget.months shouldBe NOV / 2019..DEC / 2019
    budget.tree shouldBe root {
      branch("john") {
        branch("internal") {
          leaf("test-account1")
          leaf("test-account2")
          leaf("test-account3")
        }
      }
    }
    budget.nodeToStatement.size shouldBe 5
    budget.nodeToStatement.keys shouldBe setOf(
      budget.tree[listOf("john")],
      budget.tree[listOf("john", "internal")],
      budget.tree[listOf("john", "internal", "test-account1")],
      budget.tree[listOf("john", "internal", "test-account2")],
      budget.tree[listOf("john", "internal", "test-account3")],
    )
  }

  it("build ambiguous account") {
    val path1 = listOf("bob", "test-account1")
    val account1 = Account("test-account1", path = listOf(), isSummary = false, owners = setOf("bob"), openedOn = NOV / 2019)
    val path2 = listOf("alice", "test-account1")
    val account2 = Account("test-account1", path = listOf(), isSummary = false, owners = setOf("alice"), openedOn = NOV / 2019)
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
          toAccountName = "test-account1",
          toMonth = NOV / 2019,
          fromAccountPath = path2,
          fromMonth = NOV / 2019,
          balance = Balance(50, LocalDate(2019, 11, 2), Balance.Type.CONFIRMED),
          description = null
        )
      }
    }
    exception.message shouldBe "Ambiguous transfer from alice/test-account1 to test-account1, " +
        "found multiple candidate accounts bob/test-account1, alice/test-account1"
  }

  it("build budget - duplicate balance") {
    val builder = BudgetBuilder()
    val path1 = listOf("bob", "internal", "test-account1")
    val account1 = Account("test-account1", path = listOf("internal"), owners = setOf(), isSummary = false, openedOn = NOV / 2019)
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
    val account2 = Account("test-account2", path = listOf(), owners = setOf(), isSummary = false, openedOn = NOV / 2019)
    val exception = shouldThrow<IllegalArgumentException> {
      budget {
        setAccount(path2, account2)
        addTransfer(
          toAccountName = "test-account1",
          toMonth = NOV / 2019,
          fromAccountPath = path2,
          fromMonth = NOV / 2019,
          balance = Balance(50, LocalDate(2019, 12, 2), Balance.Type.CONFIRMED),
          description = null,
        )
      }
    }
    exception.message shouldBe "Unknown account test-account1, known accounts [bob/test-account2]"
  }

  it("build budget - bad from account") {
    val path1 = listOf("bob", "test-account1")
    val account1 = Account(
      "test-account1", path = listOf(), owners = setOf(), isSummary = false,
      openedOn = NOV / 2019,
    )
    val path2 = listOf("bob", "external", "test-account2")
    val exception = shouldThrow<IllegalArgumentException> {
      budget {
        setAccount(path1, account1)
        addTransfer(
          toAccountName = "test-account1",
          toMonth = NOV / 2019,
          fromAccountPath = path2,
          fromMonth = NOV / 2019,
          balance = Balance(50, LocalDate(2019, 11, 2), Balance.Type.CONFIRMED),
          description = null,
        )
      }
    }
    exception.message shouldBe "Unknown account bob/external/test-account2"
  }

  describe("findActive accounts") {
    it("open account") {
      val path1 = listOf("bob", "internal", "test-account1")
      val account1 = Account(
        "test-account1", path = listOf("internal"), owners = setOf(), isSummary = false,
        openedOn = APR / 2026
      )
      val budget = budget {
        setAccount(path1, account1)
      }
      budget.leafToAccount shouldBe mapOf(budget.tree[listOf("bob", "internal", "test-account1")] to account1)
    }

    it("multiple accounts") {
      val path1 = listOf("bob", "test-account1")
      val account1 = Account(
        "test-account1", path = listOf(), owners = setOf(), isSummary = false,
        openedOn = APR / 2026
      )
      val path2 = listOf("bob", "test-account2")
      val account2 = Account(
        "test-account2", path = listOf(), owners = setOf(), isSummary = false,
        openedOn = NOV / 2019,
      )
      val path3 = listOf("bob", "test-account3")
      val account3 = Account(
        "test-account3", path = listOf(), owners = setOf(), isSummary = false,
        openedOn = JAN / 2020,
        closedOn = FEB / 2020,
      )
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
    fun List<TransactionStatement>.toSnapshot(): String {
      val mapper = YAMLMapper.builder()
        .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
        .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
        .build()
      val arrayNode = mapper.createArrayNode()
      forEach { it.toObjectNode(arrayNode.addObject()) }
      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(arrayNode)
    }

    describe("Build") {
      it("no months") {
        val exception = shouldThrow<IllegalArgumentException> {
          BudgetBuilder().buildTransactionStatementTable(
            tree = root {},
            months = DEC / 2019..NOV / 2019, owner = null,
            accounts = mapOf(),
            balances = mapOf(),
            transfers = mapOf()
          )
        }
        exception.message shouldBe "Budget must have at least one month."
      }

      it("single account no transfers") {
        val accountPath = listOf("john", "external", "test-account")
        val account = Account(name = "test-account", isSummary = false,
          path = listOf("external"), owners = setOf(), openedOn = DEC / 2019)
        val budget = budget {
          setAccount(accountPath, account)
        }
        val table = BudgetBuilder().buildTransactionStatementTable(
          tree = root {},
          months = budget.months,
          budget.leafToAccount,
          balances = mapOf(),
          transfers = mapOf(),
          owner = null
        )
        table.size shouldBe 1
        val stmt = table.first()
        assertSoftly {
          stmt.nodeId.path shouldBe accountPath
          stmt.coversPrevious shouldBe false
          stmt.coversProjectedPrevious shouldBe false
          stmt.endBalance shouldBe null
          stmt.hasProjectedTransfer shouldBe false
          stmt.inFlows shouldBe 0.0
          stmt.income shouldBe 0.0
          stmt.isCovered shouldBe true
          stmt.isProjectedCovered shouldBe true
          stmt.monthRange shouldBe DEC / 2019..DEC / 2019
          stmt.outFlows shouldBe 0.0
          stmt.startBalance shouldBe null
          stmt.totalPayments shouldBe 0.0
          stmt.totalTransfers shouldBe 0.0
          stmt.transactions shouldBe listOf()
          stmt.isClosed shouldBe false
        }
      }

      it("bad account name on transfer") {
        val path1 = listOf("john", "external", "test-account1")
        val account1 = Account(name = "test-account1", isSummary = false,
          path = listOf("external"),
          owners = setOf("john"), openedOn = DEC / 2021)
        val exception =
          shouldThrow<IllegalArgumentException> {
            budget {
              setAccount(path1, account1)
              addTransfer(
                fromAccountPath = path1,
                toAccountName = "test-account2",
                toMonth = DEC / 2019,
                fromMonth = DEC / 2019,
                balance = Balance.projected(2000, "2019-12-05"),
                description = "First transfer",
              )
            }
          }
        exception.message shouldBe "Unknown account test-account2, known accounts [john/external/test-account1]"
      }

      it("two accounts with common owner and transfers") {
        val account1 = Account(name = "test-account1", isSummary = false,
          path = listOf("external"),
          owners = setOf("john"), openedOn = DEC / 2019)

        val account2 = Account(name = "test-account2", isSummary = false,
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
        val path1 = listOf("john", "external", "test-account1")
        val path2 = listOf("john", "external", "test-account2")
        val accounts = mapOf(
          tree[path1]!! as Group.Leaf to account1,
          tree[path2]!! as Group.Leaf to account2,
        )

        val balances = mapOf(
          tree[path1]!! as Group.Leaf to mapOf(
            DEC / 2019 to Balance.confirmed(10, "2019-12-01"),
            JAN / 2020 to Balance.confirmed(20, "2020-01-01"),
            FEB / 2020 to Balance.projected(30, "2020-02-01")
          )
        )
        val firstTransfer1to2 = Transfer(
          fromAccount = path1,
          toAccount = path2,
          fromMonth = DEC / 2019,
          toMonth = DEC / 2019,
          description = "First transfer",
          balance = Balance.projected(2000, "2019-12-05")
        )
        val secondTransfer1to2 = Transfer(
          fromAccount = path1,
          toAccount = path2,
          fromMonth = DEC / 2019,
          toMonth = DEC / 2019,
          description = "Second transfer",
          balance = Balance.projected(1000, "2019-12-05")
        )
        val transfers = mapOf(
          tree[path1]!! as Group.Leaf to mapOf(DEC / 2019 to listOf(firstTransfer1to2, secondTransfer1to2)),
          tree[path2]!! as Group.Leaf to mapOf(DEC / 2019 to listOf(firstTransfer1to2, secondTransfer1to2)),
        )
        val table = BudgetBuilder().buildTransactionStatementTable(
          tree,
          DEC / 2019..FEB / 2020,
          accounts,
          balances,
          transfers,
          owner = null
        )
        table.size shouldBe 6
        
        expectSelfie(table.toSnapshot()).toMatchDisk()
      }

      it("two accounts with external transfer") {
        val account1 = Account(name = "test-account1", isSummary = false,
          path = listOf("external"), owners = setOf("john"), openedOn = DEC / 2019)
        val account2 = Account(name = "test-account2", isSummary = false,
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
        val path1 = listOf("john", "external", "test-account1")
        val path2 = listOf("john", "external", "test-account2")
        val accounts = mapOf(
          tree[path1]!! as Group.Leaf to account1,
          tree[path2]!! as Group.Leaf to account2
        )
        val balances = mapOf(
          tree[path1]!! as Group.Leaf to mapOf(
            DEC / 2019 to Balance.confirmed(10, "2019-12-01"),
            JAN / 2020 to Balance.confirmed(20, "2020-01-01"),
            FEB / 2020 to Balance.projected(30, "2020-02-01")
          )
        )
        val firstTransfer1to2 = Transfer(
          fromAccount = path1,
          toAccount = path2,
          fromMonth = DEC / 2019,
          toMonth = DEC / 2019,
          description = "First transfer",
          balance = Balance.projected(2000, "2019-12-05")
        )
        val secondTransfer1to2 = Transfer(
          fromAccount = path1,
          toAccount = path2,
          fromMonth = DEC / 2019,
          toMonth = DEC / 2019,
          description = "Second transfer",
          balance = Balance.projected(1000, "2019-12-05")
        )

        val transfers = mapOf(
          tree[path1]!! as Group.Leaf to mapOf(DEC / 2019 to listOf(firstTransfer1to2, secondTransfer1to2)),
          tree[path2]!! as Group.Leaf to mapOf(DEC / 2019 to listOf(firstTransfer1to2, secondTransfer1to2)),
        )
        val table = BudgetBuilder().buildTransactionStatementTable(
          tree,
          DEC / 2019..FEB / 2020,
          accounts,
          balances,
          transfers,
          owner = null
        )
        table.size shouldBe 6
        
        expectSelfie(table.toSnapshot()).toMatchDisk()
      }

      it("transfer with date before start balance") {
        val path1 = listOf("john", "external", "test-account1")
        val account1 = Account(name = "test-account1", isSummary = false,
          path = listOf("external"),
          owners = setOf("john"), openedOn = DEC / 2021)
        val exception =
          shouldThrow<IllegalStateException> {
            budget {
              setAccount(path1, account1)
              setBalance(path1, DEC / 2019, Balance.confirmed(1000, "2019-12-01"))
              addTransfer(
                fromAccountPath = path1,
                toAccountName = "test-account1",
                toMonth = DEC / 2019,
                fromMonth = DEC / 2019,
                balance = Balance.projected(2000, "2019-11-25"),
                description = "First transfer",
              )
            }
          }
        exception.message shouldBe "Balance Dec2019 Balance { amount: 10.00, date: 2019-12-01, type: CONFIRMED } " +
            "for account test-account1 starts after transaction test-account1 --> " +
            "test-account1/Balance { amount: 20.00, date: 2019-11-25, type: PROJECTED } " +
            "desc 'First transfer'"
      }

      it("transfer to closed account") {
        val account1 = Account(
          name = "test-account1",
          isSummary = false,
          path = listOf("external"),
          owners = setOf("john"),
          openedOn = NOV / 2019,
          closedOn = NOV / 2019  // closed before TransactionStatement month
        )
        val account2 = Account(
          name = "external", isSummary = false,
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
        val path1 = listOf("john", "external", "test-account1")
        val path2 = listOf("john", "external", "external")
        val accounts = mapOf(
          tree[path1]!! as Group.Leaf to account1,
          tree[path2]!! as Group.Leaf to account2
        )

        val balances = mapOf(
          tree[path1]!! as Group.Leaf to mapOf(
            DEC / 2019 to Balance.confirmed(10, "2019-12-01"),
          )
        )
        val transfers = mapOf(
          tree[path1]!! as Group.Leaf to mapOf(
            DEC / 2019 to listOf(
              Transfer(
                fromAccount = path1,
                toAccount = path2,
                fromMonth = DEC / 2019,
                toMonth = DEC / 2019,
                description = "First transfer",
                balance = Balance.projected(2000, "2019-12-05")
              ),
            )
          )
        )

        val table = BudgetBuilder().buildTransactionStatementTable(
          tree,
          NOV / 2019..DEC / 2019,
          accounts,
          balances,
          transfers,
          owner = null
        )
        table.size shouldBe 4  // Two transaction statements for the account
        table[0].monthRange shouldBe DEC / 2019..DEC / 2019
        table[0].isClosed shouldBe true
        table[0].nodeId.path shouldBe path1
        table[1].monthRange shouldBe NOV / 2019..NOV / 2019
        table[1].isClosed shouldBe false
        table[1].nodeId.path shouldBe path1
        table[2].monthRange shouldBe DEC / 2019..DEC / 2019
        table[2].isClosed shouldBe false
        table[2].nodeId.path shouldBe path2
        table[3].monthRange shouldBe NOV / 2019..NOV / 2019
        table[3].isClosed shouldBe false
        table[3].nodeId.path shouldBe path2
      }

      it("get transaction type") {
        val account1 = Account(name = "test-account1", isSummary = false,
          path = listOf("internal", "checking"),
          owners = setOf("john"), openedOn = DEC / 2019)
        val account2 = Account(name = "test-account2", isSummary = false,
          path = listOf("internal", "credit"),
          owners = setOf("john"), openedOn = DEC / 2019)
        val account3 = Account(name = "test-account3", isSummary = false,
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
        val node1 = tree[path1]!! as Group.Leaf
        val node2 = tree[path2]!! as Group.Leaf
        val node3 = tree[path3]!! as Group.Leaf
        val accounts = mapOf(node1 to account1, node2 to account2, node3 to account3)

        val balances = mapOf(
          node1 to mapOf(DEC / 2019 to Balance.confirmed(10, "2019-12-01")),
          node2 to mapOf(DEC / 2019 to Balance.confirmed(10, "2019-12-01")),
          node3 to mapOf(DEC / 2019 to Balance.confirmed(10, "2019-12-01")),
        )
        val transfer1to2 = Transfer(
          fromAccount = path1,
          toAccount = path2,
          fromMonth = DEC / 2019,
          toMonth = DEC / 2019,
          description = "First transfer",
          balance = Balance.projected(2000, "2019-12-05")
        )
        val transfer1to3 = Transfer(
          fromAccount = path1,
          toAccount = path3,
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

        val table = BudgetBuilder().buildTransactionStatementTable(
          tree,
          DEC / 2019..DEC / 2019,
          accounts,
          balances,
          transfers,
          owner = null
        )
        table.size shouldBe 3  // 3 accounts
        table[0].nodeId.path shouldBe path1
        table[0].transactions.size shouldBe 2  // 2 transactions for account1
        assertSoftly {
          table[0].transactions[0].balance.amount shouldBe -2000.0
          table[0].transactions[1].balance.amount shouldBe -1000.0
          table[0].transactions[0].type shouldBe Transaction.Type.TRANSFER
          table[0].transactions[1].type shouldBe Transaction.Type.EXPENSE
        }
        table[1].nodeId.path shouldBe path2
        table[1].transactions.size shouldBe 1  // 1 transaction for account2
        table[1].transactions[0].type shouldBe Transaction.Type.TRANSFER
        table[2].nodeId.path shouldBe path3
        table[2].transactions.size shouldBe 1  // 1 transaction for account3
        table[2].transactions[0].type shouldBe Transaction.Type.INCOME
      }
    }
  }
  describe("buildSummaryStatementTable") {
    it("single closed account - produces summary without it") {
      val path1 = listOf("john", "external", "test-account1")
      val account1 = Account(name = "test-account1", isSummary = false,
        path = listOf("external"),
        owners = setOf("john"), openedOn = MAR / 2021)
      val startBalance = Balance(
        100,
        LocalDate(2023, 12, 2),
        Balance.Type.CONFIRMED
      )
      val budget = budget {
        setAccount(path1, account1)
        setBalance(path1, MAR / 2021, startBalance)
      }
      val tranStmt = TransactionStatement(
        budget.tree[path1]!!,
        MAR / 2021..MAR / 2021,
        false,
        startBalance
      )
      // There no other transactions and balance is positive.
      tranStmt.isCovered = true
      tranStmt.isProjectedCovered = true
      budget.tree shouldBe root {
        branch("john") {
          branch("external") {
            leaf("test-account1")
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
          budget.tree[listOf("john")],
        )
      }
      val treeNode1 = budget.tree[listOf("john", "external")]!!
      val stmt1 = statements[treeNode1]!![MAR / 2021]!! as SummaryStatement
      stmt1.nodeId.path shouldBe listOf("john", "external")
      withClue("statement: $stmt1") {
        stmt1.startBalance shouldBe startBalance
        stmt1.endBalance shouldBe null
        stmt1.inFlows shouldBe 0
        stmt1.income shouldBe 0
        stmt1.monthRange shouldBe MAR / 2021..MAR / 2021
        stmt1.outFlows shouldBe 0
        stmt1.statements shouldBe listOf(tranStmt)
        stmt1.totalPayments shouldBe 0
        stmt1.totalTransfers shouldBe 0
      }

      val treeNode2 = budget.tree["john"]!!
      val stmt2 = statements[treeNode2]!![MAR / 2021]!! as SummaryStatement
      stmt2.nodeId.path shouldBe listOf("john")
      stmt2.startBalance shouldBe startBalance
      stmt2.endBalance shouldBe null
      stmt2.inFlows shouldBe 0
      stmt2.income shouldBe 0
      stmt2.monthRange shouldBe MAR / 2021..MAR / 2021
      stmt2.outFlows shouldBe 0
      stmt2.statements shouldBe listOf(stmt1)
      stmt2.totalPayments shouldBe 0
      stmt2.totalTransfers shouldBe 0
    }

    it("single external account - no SUMMARY") {
      val path1 = listOf("john", "external", "test-account1")
      val account1 = Account(name = "test-account1", isSummary = true,
        path = listOf("external"),
        owners = setOf("john"), openedOn = MAR / 2021)
      val balance1 = Balance(100, LocalDate(2023, 12, 2), Balance.Type.CONFIRMED)
      val budget = budget {
        setAccount(path1, account1)
        setBalance(path1, MAR / 2021, balance1)
      }

      budget.tree shouldBe root {
        branch("john") {
          branch("external") {
            leaf("test-account1")
          }
        }
      }
      val statements = budget.nodeToStatement
      statements.isEmpty() shouldBe false
      statements.keys shouldBe setOf(
        budget.tree[listOf("john", "external", "test-account1")],
        budget.tree[listOf("john", "external")],
        budget.tree[listOf("john")],
      )

      val treeNode = budget.tree["john"]
      val stmt = statements[treeNode]!![MAR / 2021]!! as SummaryStatement
      stmt.nodeId.path shouldBe listOf("john")
      val externalTreeNode = budget.tree[listOf("john", "external")]
      stmt.startBalance shouldBe Balance(100, LocalDate(2023, 12, 2), Balance.Type.CONFIRMED)
      stmt.endBalance shouldBe null
      stmt.inFlows shouldBe 0
      stmt.income shouldBe 0
      stmt.monthRange shouldBe MAR / 2021..MAR / 2021
      stmt.outFlows shouldBe 0
      stmt.statements shouldBe listOf(statements[externalTreeNode]!![MAR / 2021]!!)
      stmt.totalPayments shouldBe 0
      stmt.totalTransfers shouldBe 0
    }

    it("single account - no transfers") {
      val path1 = listOf("john", "external", "test-account1")
      val account1 = Account(name = "test-account1", isSummary = false,
        path = listOf("external"),
        owners = setOf("john"), openedOn = MAR / 2021)
      val balance1 = Balance(
        100,
        LocalDate(2023, 12, 2),
        Balance.Type.CONFIRMED
      )
      val budget = budget {
        setAccount(path1, account1)
        setBalance(path1, MAR / 2021, balance1)
      }
      budget.tree shouldBe root {
        branch("john") {
          branch("external") {
            leaf("test-account1")
          }
        }
      }
      val tranStmt = TransactionStatement(
        budget.tree[listOf("john", "external", "test-account1")]!!,
        MAR / 2021..MAR / 2021,
        false,
        startBalance = null
      )
      tranStmt.isCovered = true
      tranStmt.isProjectedCovered = true
      tranStmt.startBalance = balance1

      val statements = budget.nodeToStatement
      statements.isEmpty() shouldBe false
      statements.size shouldBe 3
      statements.keys shouldBe setOf(
        budget.tree[listOf("john", "external", "test-account1")],
        budget.tree[listOf("john", "external")],
        budget.tree[listOf("john")],
      )

      val externalTreeNode = budget.tree[listOf("john", "external")]
      val stmt1 = statements[externalTreeNode]!![MAR / 2021]!! as SummaryStatement
      stmt1.nodeId.path shouldBe listOf("john", "external")
      stmt1.startBalance shouldBe Balance(100, LocalDate(2023, 12, 2), Balance.Type.CONFIRMED)
      stmt1.endBalance shouldBe null
      stmt1.inFlows shouldBe 0
      stmt1.income shouldBe 0
      stmt1.monthRange shouldBe MAR / 2021..MAR / 2021
      stmt1.outFlows shouldBe 0
      stmt1.statements shouldBe listOf(tranStmt)
      stmt1.totalPayments shouldBe 0
      stmt1.totalTransfers shouldBe 0

      val ownerTreeNode = budget.tree["john"]
      val stmt2 = statements[ownerTreeNode]!![MAR / 2021]!! as SummaryStatement
      stmt2.nodeId.path shouldBe listOf("john")
      stmt2.startBalance shouldBe Balance(100, LocalDate(2023, 12, 2), Balance.Type.CONFIRMED)
      stmt2.endBalance shouldBe null
      stmt2.inFlows shouldBe 0
      stmt2.income shouldBe 0
      stmt2.monthRange shouldBe MAR / 2021..MAR / 2021
      stmt2.outFlows shouldBe 0
      stmt2.statements shouldBe listOf(stmt1)
      stmt2.totalPayments shouldBe 0
      stmt2.totalTransfers shouldBe 0
    }

    it("multiple accounts - selected owner") {
      val path1 = listOf("john", "external", "test-account1")
      val account1 = Account(name = "test-account1", isSummary = false,
        path = listOf("external"),
        owners = setOf("john"), openedOn = MAR / 2021)

      val balance1 = Balance(
        100,
        LocalDate(2023, 12, 2),
        Balance.Type.CONFIRMED
      )
      // Should skip since different owner.
      val path2 = listOf("bob", "external", "test-account2")
      val account2 = Account(        name = "test-account2", isSummary = false,
        path = listOf("external"),
        owners = setOf("bob"), openedOn = MAR / 2021)
      val balance2 = Balance(
        300,
        LocalDate(2023, 12, 2),
        Balance.Type.CONFIRMED
      )
      // Should skip since path is empty.
      val path3 = listOf("john", "test-account3")
      val account3 = Account(name = "test-account3", isSummary = false,
        path = listOf(),
        owners = setOf("john"), openedOn = MAR / 2021)
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
      budget.tree shouldBe root {
        branch("bob") {
          branch("external") {
            leaf("test-account2")
          }
        }
        branch("john") {
          branch("external") {
            leaf("test-account1")
          }
          leaf("test-account3")
        }
      }
      val tranStmt1 = TransactionStatement(
        budget.tree[listOf("john", "external", "test-account1")]!!,
        MAR / 2021..MAR / 2021,
        false,
        startBalance = null
      )
      tranStmt1.startBalance = balance1
      tranStmt1.isCovered = true
      tranStmt1.isProjectedCovered = true

      val ownerTreeNode = budget.tree["john"]
      val statements = budget.nodeToStatement[ownerTreeNode]!!
      statements.keys shouldBe setOf(MAR / 2021)

      val externalTreeNode = budget.tree[listOf("john", "external")]
      val stmt1 = budget.nodeToStatement[externalTreeNode]!![MAR / 2021]!! as SummaryStatement
      stmt1.nodeId.path shouldBe listOf("john", "external")
      stmt1.startBalance shouldBe Balance(100, LocalDate(2023, 12, 2), Balance.Type.CONFIRMED)
      stmt1.endBalance shouldBe null
      stmt1.inFlows shouldBe 0
      stmt1.income shouldBe 0
      stmt1.monthRange shouldBe MAR / 2021..MAR / 2021
      stmt1.outFlows shouldBe 0
      stmt1.statements shouldBe listOf(tranStmt1)
      stmt1.totalPayments shouldBe 0
      stmt1.totalTransfers shouldBe 0

      val treeNode3 = budget.tree[listOf("john", "test-account3")]
      val stmt3 = budget.nodeToStatement[treeNode3]!![MAR / 2021]!! as TransactionStatement
      val stmt2 = statements[MAR / 2021]!! as SummaryStatement
      stmt2.nodeId.path shouldBe listOf("john")
      stmt2.startBalance shouldBe Balance(600, LocalDate(2023, 12, 2), Balance.Type.CONFIRMED)
      stmt2.endBalance shouldBe null
      stmt2.inFlows shouldBe 0
      stmt2.income shouldBe 0
      stmt2.monthRange shouldBe MAR / 2021..MAR / 2021
      stmt2.outFlows shouldBe 0
      stmt2.statements shouldBe listOf(stmt3, stmt1)
      stmt2.totalPayments shouldBe 0
      stmt2.totalTransfers shouldBe 0
    }
  }
})
