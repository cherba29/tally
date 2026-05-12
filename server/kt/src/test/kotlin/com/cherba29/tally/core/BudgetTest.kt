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
      nodeId = NodeId("test-account1"),
      openedOn = NOV / 2019,
    )
    val account2 = Account(
      nodeId = NodeId("test-account2"),
      openedOn = NOV / 2019,
    )
    val account3 = Account(
      nodeId = NodeId("test-account3"),
      openedOn = NOV / 2019,
    )
    val budget = budget {
      setAccount(listOf("test-account1"), account1)
      setAccount(listOf("test-account2"), account2)
      setAccount(listOf("test-account3"), account3)
      setBalance(
        listOf("test-account1"),
        NOV / 2019,
        Balance(100, LocalDate(2019, 11, 1), Balance.Type.PROJECTED)
      )
      setBalance(
        listOf("test-account1"),
        DEC / 2019,
        Balance(200, LocalDate(2019, 12, 1), Balance.Type.PROJECTED)
      )
      setBalance(
        listOf("test-account2"),
        NOV / 2019,
        Balance(200, LocalDate(2019, 11, 3), Balance.Type.CONFIRMED)
      )
      addTransfer(
        toAccountName = "test-account1",
        toMonth = NOV / 2019,
        fromAccountPath = listOf("test-account2"),
        fromMonth = NOV / 2019,
        balance = Balance(50, LocalDate(2019, 11, 2), Balance.Type.CONFIRMED),
        description = null
      )
      addTransfer(
        toAccountName = "test-account3",
        toMonth = NOV / 2019,
        fromAccountPath = listOf("test-account2"),
        fromMonth = NOV / 2019,
        balance = Balance(70, LocalDate(2019, 11, 2), Balance.Type.CONFIRMED),
        description = null
      )
    }
    budget.accounts.size shouldBe 3
    budget.accounts[NodeId("test-account1")] shouldBe account1
    budget.accounts[NodeId("test-account2")] shouldBe account2
    budget.balances.size shouldBe 2
    budget.months shouldBe NOV / 2019 .. DEC / 2019
    budget.transfers.size shouldBe 3
  }

  it("build ambiguous account") {
    val node1 = NodeId("test-account1", owners = setOf("bob"))
    val path1 = listOf("bob", "test-account1")
    val account1 = Account(nodeId = node1, openedOn = NOV / 2019)
    val node2 = NodeId("test-account1", owners = setOf("alice"))
    val path2 = listOf("alice", "test-account1")
    val account2 = Account(nodeId = node2, openedOn = NOV / 2019)
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
    val account1 = Account(
      nodeId = NodeId("test-account1", path=listOf("internal")),
      openedOn = NOV / 2019,
    )
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
    val account2 = Account(
      nodeId = NodeId("test-account2"),
      openedOn = NOV / 2019,
    )
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
      nodeId = NodeId("test-account1"),
      openedOn = NOV / 2019,
    )
    val path2 = listOf("bob", "external", "test-account2")
    val exception = shouldThrow<IllegalArgumentException> {
      budget {
        setAccount(path1,account1)
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
    it("no accounts") {
      val budget = budget {}

      budget.accounts.size shouldBe 0
    }

    it("open account") {
      val path1 = listOf("bob", "internal", "test-account1")
      val account1 = Account(
        nodeId = NodeId("test-account1", path=listOf("internal")),
        openedOn = APR / 2026
      )
      val budget = budget {
        setAccount(path1, account1)
      }
      budget.accounts shouldBe mapOf(account1.nodeId to account1)
    }

    it("multiple accounts") {
      val path1 = listOf("bob", "test-account1")
      val account1 = Account(
        nodeId = NodeId("test-account1"),
        openedOn = APR / 2026
      )
      val path2 = listOf("bob", "test-account2")
      val account2 = Account(
        nodeId = NodeId("test-account2"),
        openedOn = NOV / 2019,
      )
      val path3 = listOf("bob", "test-account3")
      val account3 = Account(
        nodeId = NodeId("test-account3"),
        openedOn = JAN / 2020,
        closedOn = FEB / 2020,
      )
      val budget = budget {
        setAccount(path1, account1)
        setAccount(path2, account2)
        setAccount(path3, account3)
      }
      budget.accounts.size shouldBe 3
      budget.accounts.values shouldBe listOf(account1, account2, account3)
    }
  }
})
