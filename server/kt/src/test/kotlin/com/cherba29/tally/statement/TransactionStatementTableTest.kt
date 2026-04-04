package com.cherba29.tally.statement

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.MonthName.DEC
import com.cherba29.tally.core.MonthName.FEB
import com.cherba29.tally.core.MonthName.JAN
import com.cherba29.tally.core.MonthName.NOV
import com.cherba29.tally.core.NodeId
import com.cherba29.tally.core.budget
import com.cherba29.tally.data.yaml.toObjectNode
import com.diffplug.selfie.coroutines.expectSelfie
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.lang.IllegalStateException

class TransactionStatementTableTest : DescribeSpec({
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
        buildTransactionStatementTable(budget {}, owner = null)
      }
      exception.message shouldBe "Budget must have at least one month."
    }

    it("single account no transfers") {
      val nodeId = NodeId(
        name = "test-account",
        path = listOf("external"),
      )
      val account = Account(nodeId, openedOn = DEC / 2019)
      val budget = budget {
        setAccount(account)
      }
      val table = buildTransactionStatementTable(budget, owner = null)
      table.size shouldBe 1
      val stmt = table.first()
      assertSoftly {
        stmt.nodeId shouldBe nodeId
        stmt.coversPrevious shouldBe false
        stmt.coversProjectedPrevious shouldBe false
        stmt.endBalance shouldBe null
        stmt.hasProjectedTransfer shouldBe false
        stmt.inFlows shouldBe 0.0
        stmt.income shouldBe 0.0
        stmt.isCovered shouldBe true
        stmt.isProjectedCovered shouldBe true
        stmt.monthRange shouldBe DEC / 2019 .. DEC / 2019
        stmt.outFlows shouldBe 0.0
        stmt.startBalance shouldBe null
        stmt.totalPayments shouldBe 0.0
        stmt.totalTransfers shouldBe 0.0
        stmt.transactions shouldBe listOf()
        stmt.isClosed shouldBe false
      }
    }

    it("bad account name on transfer") {
      val node1 = NodeId(
        name = "test-account1",
        path = listOf("external"),
        owners = setOf("john")
      )
      val account1 = Account(node1, openedOn = DEC / 2021)
      val exception =
        shouldThrow<IllegalArgumentException> {
          budget {
            setAccount(account1)
            addTransfer(
              fromAccount = node1,
              toAccount = "test-account2",
              toMonth = DEC / 2019,
              fromMonth = DEC / 2019,
              balance = Balance.projected(2000, "2019-12-05"),
              description = "First transfer",
            )
          }
        }
      exception.message shouldBe "Unknown account test-account2"
    }

    it("two accounts with common owner and transfers") {
      val node1 = NodeId(
        name = "test-account1",
        path = listOf("external"),
        owners = setOf("john")
      )
      val account1 = Account(node1, openedOn = DEC / 2019)
      val node2 = NodeId(
        name = "test-account2",
        path = listOf("external"),
        owners = setOf("john")
      )

      val account2 = Account(node2, openedOn = DEC / 2019)
      val budget = budget {
        setAccount(account1)
        setBalance(node1, DEC / 2019, Balance.confirmed(10, "2019-12-01"))
        setBalance(node1, JAN / 2020, Balance.confirmed(20, "2020-01-01"))
        setBalance(node1, FEB / 2020, Balance.projected(30, "2020-02-01"))
        setAccount(account2)
        addTransfer(
          fromAccount = node1,
          toAccount = node2.name,
          toMonth = DEC / 2019,
          fromMonth = DEC / 2019,
          balance = Balance.projected(2000, "2019-12-05"),
          description = "First transfer",
        )

        addTransfer(
          fromAccount = node1,
          toAccount = node2.name,
          toMonth = DEC / 2019,
          fromMonth = DEC / 2019,
          balance = Balance.projected(1000, "2019-12-05"),
          description = "Second transfer",
        )
      }
      val table = buildTransactionStatementTable(budget, owner = null)
      table.size shouldBe 6
      expectSelfie(table.toSnapshot()).toMatchDisk()
    }

    it("two accounts with external transfer") {
      val node1 = NodeId(
        name = "test-account1",
        path = listOf("external"),
      )
      val account1 = Account(node1, openedOn = DEC / 2019)
      val node2 = NodeId(
        name = "test-account2",
        path = listOf("external"),
        owners = setOf("john"),
      )
      val account2 = Account(node2, openedOn = DEC / 2019)
      val budget = budget {
        setAccount(account1)
        setBalance(node1, DEC / 2019, Balance.confirmed(10, "2019-12-01"))
        setBalance(node1, JAN / 2020, Balance.confirmed(20, "2020-01-01"))
        setBalance(node1, FEB / 2020, Balance.projected(30, "2020-02-01"))
        setAccount(account2)
        addTransfer(
          fromAccount = node1,
          toAccount = node2.name,
          toMonth = DEC / 2019,
          fromMonth = DEC / 2019,
          balance = Balance.projected(2000, "2019-12-05"),
          description = "First transfer",
        )

        addTransfer(
          fromAccount = node1,
          toAccount = node2.name,
          toMonth = DEC / 2019,
          fromMonth = DEC / 2019,
          balance = Balance.projected(1000, "2019-12-05"),
          description = "Second transfer",
        )
      }
      val table = buildTransactionStatementTable(budget, owner = null)
      table.size shouldBe 6
      expectSelfie(table.toSnapshot()).toMatchDisk()
    }

    it("single account with transfers") {
      val node1 = NodeId(
        name = "test-account1",
        path = listOf("external"),
        owners = setOf("john"),
      )
      val account1 = Account(node1, openedOn = DEC / 2019)
      val budget = budget {
        setAccount(account1)
        setBalance(node1, DEC / 2019, Balance.confirmed(10, "2019-12-01"))
        setBalance(node1, JAN / 2020, Balance.confirmed(20, "2020-01-01"))
        setBalance(node1, FEB / 2020, Balance.projected(30, "2020-02-01"))
        addTransfer(
          fromAccount = node1,
          toAccount = node1.name,
          toMonth = DEC / 2019,
          fromMonth = DEC / 2019,
          balance = Balance.projected(2000, "2019-12-05"),
          description = "First transfer",
        )
      }
      val table = buildTransactionStatementTable(budget, owner = null)
      table.size shouldBe 3
      expectSelfie(table.toSnapshot()).toMatchDisk()
    }

    it("transfer with date before start balance") {
      val node1 = NodeId(
        name = "test-account1",
        path = listOf("external"),
        owners = setOf("john"),
      )
      val account1 = Account(node1, openedOn = DEC / 2021)
      val budget = budget {
        setAccount(account1)
        setBalance(node1, DEC / 2019, Balance.confirmed(1000, "2019-12-01"))
        addTransfer(
          fromAccount = node1,
          toAccount = node1.name,
          toMonth = DEC / 2019,
          fromMonth = DEC / 2019,
          balance = Balance.projected(2000, "2019-11-25"),
          description = "First transfer",
        )
      }
      val exception =
        shouldThrow<IllegalStateException> { buildTransactionStatementTable(budget, owner = null) }
      exception.message shouldBe "Balance Dec2019 Balance { amount: 10.00, date: 2019-12-01, type: CONFIRMED } " +
          "for account /external/test-account1 starts after transaction test-account1 --> " +
          "test-account1/Balance { amount: 20.00, date: 2019-11-25, type: PROJECTED } " +
          "desc 'First transfer'"
    }

    it("transfer to closed account") {
      val node1 = NodeId(
        name = "test-account1",
        path = listOf("external"),
        owners = setOf("john"),
      )

      val account1 = Account(
        node1,
        openedOn = NOV / 2019,
        closedOn = NOV / 2019  // closed before TransactionStatement month
      )
      val budget = budget {
        setAccount(account1)
        setBalance(node1, DEC / 2019, Balance.confirmed(10, "2019-12-01"))
        addTransfer(
          fromAccount = node1,
          toAccount = node1.name,
          toMonth = DEC / 2019,
          fromMonth = DEC / 2019,
          balance = Balance.projected(2000, "2019-12-05"),
          description = "First transfer",
        )
      }
      val table = buildTransactionStatementTable(budget, owner = null)
      table.size shouldBe 2  // Two transaction statements for the account
      table[0].monthRange shouldBe DEC / 2019 .. DEC / 2019
      table[0].isClosed shouldBe true
      table[1].monthRange shouldBe NOV / 2019 .. NOV / 2019
      table[1].isClosed shouldBe false
    }

    it("get transaction type") {
      val node1 = NodeId(
        name = "test-account1",
        path = listOf("internal", "checking"),
        owners = setOf("john"),
      )
      val account1 = Account(node1, openedOn = DEC / 2019)
      val node2 = NodeId(
        name = "test-account2",
        path = listOf("internal", "credit"),
        owners = setOf("john"),
      )
      val account2 = Account(node2, openedOn = DEC / 2019)
      val node3 = NodeId(
        name = "test-account3",
        path = listOf("external", "expense"),
        owners = setOf(),
      )

      val account3 = Account(node3, openedOn = DEC / 2019)
      val budget = budget {
        setAccount(account1)
        setAccount(account2)
        setAccount(account3)
        setBalance(node1, DEC / 2019, Balance.confirmed(10, "2019-12-01"))
        setBalance(node2, DEC / 2019, Balance.confirmed(10, "2019-12-01"))
        setBalance(node3, DEC / 2019, Balance.confirmed(10, "2019-12-01"))
        addTransfer(
          fromAccount = node1,
          toAccount = node2.name,
          toMonth = DEC / 2019,
          fromMonth = DEC / 2019,
          balance = Balance.projected(2000, "2019-12-05"),
          description = "First transfer",
        )

        addTransfer(
          fromAccount = node1,
          toAccount = node3.name,
          toMonth = DEC / 2019,
          fromMonth = DEC / 2019,
          balance = Balance.projected(1000, "2019-12-05"),
          description = "Second transfer",
        )
      }
      val table = buildTransactionStatementTable(budget, owner = null)
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