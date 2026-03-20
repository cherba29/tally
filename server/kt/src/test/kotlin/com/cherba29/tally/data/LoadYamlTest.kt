package com.cherba29.tally.data

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.BalanceType
import com.cherba29.tally.core.BudgetBuilder
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.MonthName.FEB
import com.cherba29.tally.core.MonthName.JAN
import com.cherba29.tally.core.MonthName.MAR
import com.cherba29.tally.core.MonthName.NOV
import com.cherba29.tally.core.Transfer
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import java.nio.file.Paths
import kotlinx.datetime.LocalDate

class LoadYamlTest : DescribeSpec({
  describe("loadYaml") {
    it("empty") {
      val budgetBuilder = BudgetBuilder()
      val relativeFilePath = Paths.get("path/file.yaml")
      loadYamlFile(budgetBuilder, parseYamlContent("number: 123", relativeFilePath), relativeFilePath)
      val budget = budgetBuilder.build()
      budget.accounts.size shouldBe 0
    }

    it("fails when unknown account type") {
      val budgetBuilder = BudgetBuilder()
      val relativeFilePath = Paths.get("path/file.yaml")
      val exception = shouldThrow<IllegalArgumentException> {
        loadYamlFile(
          budgetBuilder,
          parseYamlContent("name: test\ntype: SOMETHING", relativeFilePath),
          relativeFilePath
        )
      }
      exception.message shouldBe "Unknown type 'SOMETHING' for account 'test' while processing path/file.yaml"
    }

    it("fails when account has no owners") {
      val budgetBuilder = BudgetBuilder()
      val relativeFilePath = Paths.get("path/file.yaml")
      val exception = shouldThrow<IllegalArgumentException> {
        loadYamlFile(
          budgetBuilder,
          parseYamlContent("name: test\ntype: external\nowner: []", relativeFilePath),
          relativeFilePath
        )
      }
      exception.message shouldBe "Account 'test' has no owners while processing path/file.yaml"
    }

    it("empty account") {
      val budgetBuilder = BudgetBuilder()
      val relativeFilePath = Paths.get("path/file.yaml")
      val content = """
      name: test-account
      desc: "Testing account"
      number: "1223344"
      owner: [ arthur ]
      opened_on: Nov2019
      closed_on: Mar2020
      type: external
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
      loadYamlFile(budgetBuilder, parsedContent, relativeFilePath)
      val budget = budgetBuilder.build()
      budget.accounts.size shouldBe 1

      val account = budget.accounts["test-account"]!!
      account.name shouldBe "test-account"
      account.description shouldBe "Testing account"
      account.number shouldBe "1223344"
      account.type shouldBe Account.Type.EXTERNAL
      account.path shouldBe listOf()
      account.url shouldBe "example.com"
      account.phone shouldBe "111-222-3344"
      account.address shouldBe "55 Road"
      account.userName shouldBe "john"
      account.password shouldBe "xxxyyy"
      account.owners shouldBe listOf("arthur")
      account.openedOn shouldBe NOV / 2019
      account.closedOn shouldBe MAR / 2020

      budget.balances.size shouldBe 0
      budget.months.size shouldBe 5
      budget.transfers.size shouldBe 0
    }

    it("account with balances") {
      val budgetBuilder = BudgetBuilder()
      val relativeFilePath = Paths.get("path/test.yaml")
      val content = """
      name: test-account
      owner: [ someone ]
      type: external
      balances:
      - { grp: Feb2020, date: 2020-02-01, pamt: 10.00 }
      - { grp: Jan2020, date: 2020-01-01, camt:  0.00 }
      """.trimIndent()
      val parsedContent = parseYamlContent(content, relativeFilePath)
      parsedContent shouldNotBe null
      loadYamlFile(budgetBuilder, parsedContent, relativeFilePath)
      val budget = budgetBuilder.build()
      budget.accounts.size shouldBe 1
      budget.balances.size shouldBe 1
      budget.months.size shouldBe 2
      budget.transfers.size shouldBe 0

      val balances = budget.balances["test-account"]!!
      balances.size shouldBe 2
      balances[JAN / 2020] shouldBe Balance(0, LocalDate.parse("2020-01-01"), BalanceType.CONFIRMED)
      balances[FEB / 2020] shouldBe Balance(1000, LocalDate.parse("2020-02-01"), BalanceType.PROJECTED)
    }

    it("fails without balance month") {
      val budgetBuilder = BudgetBuilder()
      val relativeFilePath = Paths.get("path/file.yaml")
      val content = """
      name: test-account
      owner: [ someone ]
      type: external
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
      type: external
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
      type: external
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
      type: external
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
      type: external
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
      val budgetBuilder = BudgetBuilder()
      val relativeFilePath = Paths.get("path/file.yaml")
      val testAccountData = """
      name: test-account
      owner: [ someone ]
      type: external
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
      loadYamlFile(budgetBuilder, parsedContent, relativeFilePath)
      val externalAccountData = """
      name: external
      owner: [ someone ]
      type: external
      """.trimIndent()
      val parsedExternalContent = parseYamlContent(externalAccountData, relativeFilePath)
      parsedExternalContent shouldNotBe null
      loadYamlFile(budgetBuilder, parsedExternalContent, relativeFilePath)

      val budget = budgetBuilder.build()
      budget.accounts.size shouldBe 2
      val testAccount = budget.accounts["test-account"]!!
      val externalAccount = budget.accounts["external"]!!
      budget.balances.size shouldBe 1
      budget.months.size shouldBe 2
      budget.transfers.size shouldBe 2

      val testAccountTransfers = budget.transfers["test-account"]!!
      testAccountTransfers.size shouldBe 1
      val testAccountMonthTransfers = testAccountTransfers[JAN / 2020]
      testAccountMonthTransfers shouldBe setOf(
        Transfer(
          fromAccount = testAccount,
          toAccount = externalAccount,
          fromMonth = Month.fromString("Jan2020"),
          toMonth = Month.fromString("Jan2020"),
          balance = Balance(3750, LocalDate.parse("2020-01-17"), BalanceType.PROJECTED),
          description = null,
        ),
        Transfer(
          fromAccount = testAccount,
          toAccount = externalAccount,
          fromMonth = Month.fromString("Jan2020"),
          toMonth = Month.fromString("Jan2020"),
          balance = Balance(-2248, LocalDate.parse("2020-01-15"), BalanceType.CONFIRMED),
          description = null,
        ),
      )
      val externalAccountTransfers = budget.transfers["external"]!!
      externalAccountTransfers.size shouldBe 1
      val externalAccountMonthTransfers = externalAccountTransfers[JAN / 2020]
      externalAccountMonthTransfers shouldBe setOf(
        Transfer(
          fromAccount = testAccount,
          toAccount = externalAccount,
          fromMonth = Month.fromString("Jan2020"),
          toMonth = Month.fromString("Jan2020"),
          balance = Balance(3750, LocalDate.parse("2020-01-17"), BalanceType.PROJECTED),
          description = null,
        ),
        Transfer(
          fromAccount = testAccount,
          toAccount = externalAccount,
          fromMonth = Month.fromString("Jan2020"),
          toMonth = Month.fromString("Jan2020"),
          balance = Balance(-2248, LocalDate.parse("2020-01-15"), BalanceType.CONFIRMED),
          description = null,
        ),
      )
    }

    it("fails with transfer and no grp") {
      val budgetBuilder = BudgetBuilder()
      val relativeFilePath = Paths.get("path/test.yaml")
      val testAccountData = """
      name: test-account
      owner: [ someone ]
      type: external
      transfers_to:
        external:
        - { date: 2020-01-17, pamt: 37.50 }
      """.trimIndent()
      val parsedContent = parseYamlContent(testAccountData, relativeFilePath)
      parsedContent shouldNotBe null
      val exception = shouldThrow<IllegalArgumentException> {
        loadYamlFile(budgetBuilder, parsedContent, relativeFilePath)
      }
      exception.message shouldBe "For account 'test-account' transfer to 'external' does not have 'grp' field. while processing path/test.yaml"
    }

    it("fails with transfer and no date") {
      val budgetBuilder = BudgetBuilder()
      val relativeFilePath = Paths.get("path/test.yaml")
      val testAccountData = """
      name: test-account
      owner: [ someone ]
      type: external
      transfers_to:
        external:
        - { grp: Jan2020, pamt: 37.50 }
      """.trimIndent()
      val parsedContent = parseYamlContent(testAccountData, relativeFilePath)
      parsedContent shouldNotBe null
      val exception = shouldThrow<IllegalArgumentException> {
        loadYamlFile(budgetBuilder, parsedContent, relativeFilePath)
      }
      exception.message shouldBe "For account 'test-account' transfer to 'external' does not have a " +
          "valid 'date' field. while processing path/test.yaml"
    }

    it("fails with transfer and too far apart dates") {
      val budgetBuilder = BudgetBuilder()
      val relativeFilePath = Paths.get("path/test.yaml")
      val testAccountData = """
      name: test-account
      owner: [ someone ]
      type: external
      transfers_to:
        external:
        - { grp: Jan2020, date: 2020-04-01, pamt: 37.50 }
      """.trimIndent()
      val parsedContent = parseYamlContent(testAccountData, relativeFilePath)
      parsedContent shouldNotBe null
      val exception = shouldThrow<IllegalArgumentException> {
        loadYamlFile(budgetBuilder, parsedContent, relativeFilePath)
      }
      exception.message shouldBe "For account 'test-account' transfer to 'external' for Jan2020 date " +
          "2020-04-01 (Apr2020) are too far apart. while processing path/test.yaml"
    }

    it("fails with transfer and no balance") {
      val budgetBuilder = BudgetBuilder()
      val relativeFilePath = Paths.get("path/test.yaml")
      val testAccountData = """
      name: test-account
      owner: [ someone ]
      type: external
      transfers_to:
        external:
        - { grp: Jan2020, date: 2020-01-17 }
      """.trimIndent()
      val parsedContent = parseYamlContent(testAccountData, relativeFilePath)
      parsedContent shouldNotBe null
      val exception = shouldThrow<IllegalArgumentException> {
        loadYamlFile(budgetBuilder, parsedContent, relativeFilePath)
      }
      exception.message shouldBe "For account 'test-account' transfer to 'external' does not " +
          "have 'pamt' or 'camt' field:" +
          " TransferYamlData(grp=Jan2020, date=2020-01-17, camt=null, pamt=null, " +
          "desc=null, cat=null, tags=null). while processing path/test.yaml"
    }
  }
})
