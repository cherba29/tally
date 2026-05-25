package com.cherba29.tally.data

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.MonthName.FEB
import com.cherba29.tally.core.MonthName.JAN
import com.cherba29.tally.core.MonthName.MAR
import com.cherba29.tally.core.MonthName.NOV
import com.cherba29.tally.core.NodeId
import com.cherba29.tally.data.builder.BudgetBuilder
import com.cherba29.tally.data.builder.budget
import com.cherba29.tally.statement.Transaction
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import java.nio.file.Paths
import kotlinx.datetime.LocalDate

class LoadYamlTest : DescribeSpec({
  describe("loadYaml") {
    it("empty - requires account name and month") {
      val relativeFilePath = Paths.get("path/file.yaml")
      val error = shouldThrow<IllegalArgumentException> {
        budget {
          loadYamlFile(this, parseYamlContent("number: 123\nopened_on: Dec2019", relativeFilePath), relativeFilePath)
        }
      }
      error.message shouldBe "Budget must have at least one month."
    }

    it("fails when account has no owners") {
      val relativeFilePath = Paths.get("path/file.yaml")
      val exception = shouldThrow<IllegalArgumentException> {
        budget {
          loadYamlFile(
            this,
            parseYamlContent("name: test\ntype: external\nowner: []", relativeFilePath),
            relativeFilePath
          )
        }
      }
      exception.message shouldBe "Account 'test' has no owners while processing path/file.yaml"
    }

    it("empty account") {
      val relativeFilePath = Paths.get("path/file.yaml")
      val content = """
      name: test-account
      desc: "Testing account"
      number: "1223344"
      owner: [ arthur ]
      opened_on: Nov2019
      closed_on: Mar2020
      path: [ external ]
      url: "example.com"
      phone: "111-222-3344"
      address: "55 Road"
      username: "john"
      pswd: "xxxyyy"
      transfers_to:
        external:
      """.trimIndent()
      val parsedContent = parseYamlContent(content, relativeFilePath)
      parsedContent.openedOn shouldNotBe null
      parsedContent.closedOn shouldNotBe null
      val budget = budget {
        loadYamlFile(this, parsedContent, relativeFilePath)
      }
      budget.accounts.size shouldBe 1

      val account = budget.accounts[NodeId("test-account", isSummary = false, owners = setOf("arthur"), path=listOf("external"))]!!
      account.nodeId.name shouldBe "test-account"
      account.description shouldBe "Testing account"
      account.number shouldBe "1223344"
      account.nodeId.path shouldBe listOf("external")
      account.url shouldBe "example.com"
      account.phone shouldBe "111-222-3344"
      account.address shouldBe "55 Road"
      account.userName shouldBe "john"
      account.password shouldBe "xxxyyy"
      account.nodeId.owners shouldBe listOf("arthur")
      account.openedOn shouldBe NOV / 2019
      account.closedOn shouldBe MAR / 2020

      budget.statements.size shouldBe 1
      val monthlyStatements = budget.statements[account.nodeId]!!
      monthlyStatements.size shouldBe 5
      monthlyStatements.values.count { it.startBalance != null } shouldBe 0
      budget.months.size shouldBe 5
      budget.summaries.size shouldBe 0
    }

    it("account with balances") {
      val relativeFilePath = Paths.get("path/test.yaml")
      val content = """
      name: test-account
      owner: [ someone ]
      path: [ external ]
      opened_on: Jan2020
      balances:
      - { grp: Feb2020, date: 2020-02-01, pamt: 10.00 }
      - { grp: Jan2020, date: 2020-01-01, camt:  0.00 }
      """.trimIndent()
      val parsedContent = parseYamlContent(content, relativeFilePath)
      parsedContent shouldNotBe null
      val budget = budget {
        loadYamlFile(this, parsedContent, relativeFilePath)
      }
      budget.accounts.size shouldBe 1
      budget.statements.size shouldBe 1
      budget.months.size shouldBe 2
      budget.statements.values.sumOf { it.values.sumOf { s -> s.transactions.size } } shouldBe 0

      val monthlyStatements = budget.statements[NodeId("test-account", isSummary = false, setOf("someone"), listOf("external"))]!!
      monthlyStatements.size shouldBe 2
      monthlyStatements[JAN / 2020]?.startBalance shouldBe Balance(0, LocalDate.parse("2020-01-01"), Balance.Type.CONFIRMED)
      monthlyStatements[FEB / 2020]?.startBalance shouldBe Balance(1000, LocalDate.parse("2020-02-01"), Balance.Type.PROJECTED)
    }

    it("fails without balance month") {
      val budgetBuilder = BudgetBuilder()
      val relativeFilePath = Paths.get("path/file.yaml")
      val content = """
      name: test-account
      owner: [ someone ]
      path: [ external ]
      opened_on: Jan2020
      balances:
      - { date: 2020-01-01, camt:  0.00 }
      """.trimIndent()
      val parsedContent = parseYamlContent(content, relativeFilePath)
      parsedContent shouldNotBe null
      val exception = shouldThrow<IllegalArgumentException> {
        loadYamlFile(budgetBuilder, parsedContent, relativeFilePath)
      }
      // TODO: do not test for BalanceData toString representation.
      exception.message shouldBe "Balance entry BalanceYamlData(" +
          "grp=null, date=2020-01-01, camt=0.0, pamt=null, desc=null) " +
          "has no grp setting. while processing path/file.yaml"
    }

    it("fails with bad balance month") {
      val relativeFilePath = Paths.get("path/file.yaml")
      val content = """
      name: test-account
      owner: [ someone ]
      path: [ external ]
      opened_on: Jan2020
      balances:
      - { grp: Xxx2020, date: 2020-01-01, camt:  0.00 }
      """.trimIndent()
      val exception = shouldThrow<IllegalArgumentException> {
        parseYamlContent(content, relativeFilePath)
      }
      exception.message shouldContain "Bad month name 'Xxx' for 'Xxx2020', valid names " +
          "[Jan, Feb, Mar, Apr, May, Jun, Jul, Aug, Sep, Oct, Nov, Dec]"
    }

    it("fails without balance date") {
      val budgetBuilder = BudgetBuilder()
      val relativeFilePath = Paths.get("path/file.yaml")
      val content = """
      name: test-account
      owner: [ someone ]
      path: [ external ]
      opened_on: Jan2020
      balances:
      - { grp: Jan2020, camt:  0.00 }
      """.trimIndent()
      val parsedContent = parseYamlContent(content, relativeFilePath)
      parsedContent shouldNotBe null
      val exception = shouldThrow<IllegalArgumentException> {
        loadYamlFile(budgetBuilder, parsedContent, relativeFilePath)
      }
      // TODO: do not test for BalanceData toString representation.
      exception.message shouldBe "Balance BalanceYamlData(" +
          "grp=Jan2020, date=null, camt=0.0, pamt=null, desc=null)" +
          " does not have date set. while processing path/file.yaml"
    }

    it("fails with bad balance date") {
      val relativeFilePath = Paths.get("path/file.yaml")
      val content = """
      name: test-account
      owner: [ someone ]
      path: [ external ]
      opened_on: Jan2020
      balances:
      - { grp: Jan2020, date: 20200101, camt:  0.00 }
      """.trimIndent()
      val exception = shouldThrow<IllegalArgumentException> {
        parseYamlContent(content, relativeFilePath)
      }
      exception.message shouldContain "Text '20200101' could not be parsed"
      exception.message shouldContain "while processing path/file.yaml"
    }

    it("fails without balance type") {
      val budgetBuilder = BudgetBuilder()
      val relativeFilePath = Paths.get("path/file.yaml")
      val content = """
      name: test-account
      owner: [ someone ]
      path: [ external ]
      opened_on: Jan2020
      balances:
      - { grp: Jan2020, date: 2020-01-01, xamt:  0.00 }
      """.trimIndent()
      val parsedContent = parseYamlContent(content, relativeFilePath)
      parsedContent shouldNotBe null
      val exception = shouldThrow<IllegalArgumentException> {
        loadYamlFile(budgetBuilder, parsedContent, relativeFilePath)
      }
      // TODO: do not test for BalanceData toString representation.
      exception.message shouldBe "Balance BalanceYamlData(" +
          "grp=Jan2020, date=2020-01-01, camt=null, pamt=null, desc=null) " +
          "does not have amount type set, expected camt or pamt entry. while processing path/file.yaml"
    }

    it("with projected and confirmed transfers") {
      val relativeFilePath = Paths.get("path/file.yaml")
      val testAccountData = """
      name: test-account
      owner: [ someone ]
      path: [ external ]
      opened_on: Jan2020
      balances:
      - { grp: Feb2020, date: 2020-02-01, pamt: 10.00 }
      - { grp: Jan2020, date: 2020-01-01, camt:  0.00 }
      transfers_to:
        external:
        - { grp: Jan2020, date: 2020-01-17, pamt: 37.50 }
        - { grp: Jan2020, date: 2020-01-15, camt: -22.48 }
      """.trimIndent()
      val parsedContent = parseYamlContent(testAccountData, relativeFilePath)
      parsedContent shouldNotBe null

      val externalAccountData = """
      name: external
      owner: [ someone ]
      path: [ external ]
      opened_on: Jan2020
      """.trimIndent()
      val parsedExternalContent = parseYamlContent(externalAccountData, relativeFilePath)
      parsedExternalContent shouldNotBe null

      val budget = budget {
        loadYamlFile(this, parsedContent, relativeFilePath)
        loadYamlFile(this, parsedExternalContent, relativeFilePath)
      }
      budget.months.size shouldBe 2
      budget.accounts.size shouldBe 2
      budget.statements.size shouldBe 2
      budget.summaries.size shouldBe 2
      budget.summaries.keys shouldBe setOf(listOf("someone", "external"), listOf("someone", ""))
      budget.summaries[listOf("someone", "external")]?.keys shouldBe setOf(FEB / 2020, JAN / 2020)
      budget.summaries[listOf("someone", "")]?.keys shouldBe setOf(FEB / 2020, JAN / 2020)

      val node1 = NodeId("test-account", isSummary = false, setOf("someone"), listOf("external"))
      val node2 = NodeId("external", isSummary = false, setOf("someone"), listOf("external"))
      val externalAccount = budget.accounts[node2]!!
      val testAccountMonthlyStatements = budget.statements[node1]!!

      testAccountMonthlyStatements.size shouldBe 2
      testAccountMonthlyStatements.values.count { it.startBalance != null } shouldBe 2

      val testAccountStatement = testAccountMonthlyStatements[JAN / 2020]!!
      testAccountStatement.transactions shouldBe setOf(
        Transaction(
          nodeId = node2,
          balance = Balance(-3750, LocalDate.parse("2020-01-17"), Balance.Type.PROJECTED),
          description = null,
          type = Transaction.Type.EXPENSE,
          balanceFromStart = -1502
        ),
        Transaction(
          nodeId = node2,
          balance = Balance(2248, LocalDate.parse("2020-01-15"), Balance.Type.CONFIRMED),
          description = null,
          type = Transaction.Type.INCOME,
          balanceFromStart = 2248
        ),
      )
      val externalAccountMonthlyStatements = budget.statements[externalAccount.nodeId]!!
      externalAccountMonthlyStatements.size shouldBe 2
      val externalAccountStatement = externalAccountMonthlyStatements[JAN / 2020]!!
      externalAccountStatement.transactions shouldBe setOf(
        Transaction(
          nodeId = node1,
          balance = Balance(3750, LocalDate.parse("2020-01-17"), Balance.Type.PROJECTED),
          description = null,
          type = Transaction.Type.INCOME,
          balanceFromStart = null,
        ),
        Transaction(
          nodeId = node1,
          balance = Balance(-2248, LocalDate.parse("2020-01-15"), Balance.Type.CONFIRMED),
          description = null,
          type = Transaction.Type.EXPENSE,
          balanceFromStart = null,
        ),
      )
    }

    it("fails with transfer and no grp") {
      val budgetBuilder = BudgetBuilder()
      val relativeFilePath = Paths.get("path/test.yaml")
      val testAccountData = """
      name: test-account
      owner: [ someone ]
      path: [ external ]
      opened_on: Jan2020
      transfers_to:
        external:
        - { date: 2020-01-17, pamt: 37.50 }
      """.trimIndent()
      val parsedContent = parseYamlContent(testAccountData, relativeFilePath)
      parsedContent shouldNotBe null
      val exception = shouldThrow<IllegalArgumentException> {
        loadYamlFile(budgetBuilder, parsedContent, relativeFilePath)
      }
      exception.message shouldBe "For account '/external/test-account' transfer to 'external' " +
          "does not have 'grp' field. while processing path/test.yaml"
    }

    it("fails with transfer and no date") {
      val budgetBuilder = BudgetBuilder()
      val relativeFilePath = Paths.get("path/test.yaml")
      val testAccountData = """
      name: test-account
      owner: [ someone ]
      path: [ external ]
      opened_on: Jan2020
      transfers_to:
        external:
        - { grp: Jan2020, pamt: 37.50 }
      """.trimIndent()
      val parsedContent = parseYamlContent(testAccountData, relativeFilePath)
      parsedContent shouldNotBe null
      val exception = shouldThrow<IllegalArgumentException> {
        loadYamlFile(budgetBuilder, parsedContent, relativeFilePath)
      }
      exception.message shouldBe "For account '/external/test-account' transfer to 'external' does not have a " +
          "valid 'date' field. while processing path/test.yaml"
    }

    it("fails with transfer and too far apart dates") {
      val budgetBuilder = BudgetBuilder()
      val relativeFilePath = Paths.get("path/test.yaml")
      val testAccountData = """
      name: test-account
      owner: [ someone ]
      path: [ external ]
      opened_on: Jan2020
      transfers_to:
        external:
        - { grp: Jan2020, date: 2020-04-01, pamt: 37.50 }
      """.trimIndent()
      val parsedContent = parseYamlContent(testAccountData, relativeFilePath)
      parsedContent shouldNotBe null
      val exception = shouldThrow<IllegalArgumentException> {
        loadYamlFile(budgetBuilder, parsedContent, relativeFilePath)
      }
      exception.message shouldBe "For account '/external/test-account' transfer to 'external' for Jan2020 date " +
          "2020-04-01 (Apr2020) are too far apart. while processing path/test.yaml"
    }

    it("fails with transfer and no balance") {
      val budgetBuilder = BudgetBuilder()
      val relativeFilePath = Paths.get("path/test.yaml")
      val testAccountData = """
      name: test-account
      owner: [ someone ]
      path: [ external ]
      opened_on: Jan2020
      transfers_to:
        external:
        - { grp: Jan2020, date: 2020-01-17 }
      """.trimIndent()
      val parsedContent = parseYamlContent(testAccountData, relativeFilePath)
      parsedContent shouldNotBe null
      val exception = shouldThrow<IllegalArgumentException> {
        loadYamlFile(budgetBuilder, parsedContent, relativeFilePath)
      }
      exception.message shouldBe "For account '/external/test-account' transfer to 'external' does not " +
          "have 'pamt' or 'camt' field:" +
          " TransferYamlData(grp=Jan2020, date=2020-01-17, camt=null, pamt=null, " +
          "desc=null, cat=null, tags=null). while processing path/test.yaml"
    }
  }
})
