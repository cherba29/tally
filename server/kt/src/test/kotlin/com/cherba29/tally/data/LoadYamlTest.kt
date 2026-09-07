package com.cherba29.tally.data

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.MonthName.APR
import com.cherba29.tally.core.MonthName.DEC
import com.cherba29.tally.core.MonthName.FEB
import com.cherba29.tally.core.MonthName.JAN
import com.cherba29.tally.core.MonthName.MAR
import com.cherba29.tally.core.MonthName.NOV
import com.cherba29.tally.utils.root
import com.cherba29.tally.data.builder.BudgetBuilder
import com.cherba29.tally.data.builder.budget
import com.cherba29.tally.data.yaml.BalanceYamlData
import com.cherba29.tally.data.yaml.TransferYamlData
import com.cherba29.tally.data.yaml.YamlData
import com.cherba29.tally.core.Transaction
import com.cherba29.tally.statement.TransactionStatement
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import java.nio.file.Paths

class LoadYamlTest : DescribeSpec({
  describe("loadYaml") {
    it("empty - requires account name and month") {
      val relativeFilePath = Paths.get("path/file.yaml")
      val accountData = YamlData(
        number = "123",
        openedOn = DEC / 2019
      )
      val error = shouldThrow<IllegalArgumentException> {
        budget {
          loadYamlFile(this, accountData, relativeFilePath)
        }
      }
      error.message shouldBe "Budget must have at least one month."
    }

    it("fails when account has no owners") {
      val relativeFilePath = Paths.get("path/file.yaml")
      val accountData = YamlData(
        name = "test",
        number = "123",
        openedOn = DEC / 2019,
        owner = listOf()
      )
      val exception = shouldThrow<IllegalArgumentException> {
        budget {
          loadYamlFile(
            this,
            accountData,
            relativeFilePath
          )
        }
      }
      exception.message shouldBe "Account 'test' has no owners, while processing path/file.yaml"
    }

    it("empty account") {
      val relativeFilePath = Paths.get("path/file.yaml")
      val accountData = YamlData(
        name = "test-account",
        desc = "Testing account",
        number = "1223344",
        path = listOf("external"),
        openedOn = NOV / 2019,
        closedOn = MAR / 2020,
        owner = listOf("arthur"),
        url = "example.com",
        phone = "111-222-3344",
        address = "55 Road",
        username = "john",
        pswd = "xxxyyy",
        transfersTo = mapOf("external" to listOf())
      )

      val budget = budget {
        loadYamlFile(this, accountData, relativeFilePath)
      }
      budget.tree shouldBe root(Profile()) {
        branch("arthur", Profile()) {
          branch("external", Profile(isExternal = true)) {
            branch("inactive", Profile(isExternal = true, isInactive = true)) {
              leaf("_test-account", Profile(isExternal = true, isInactive = true))
            }
            leaf("test-account", Profile(isExternal = true))
          }
        }
      }
      budget.tree.nLeaves shouldBe 2

      val account = budget.tree[listOf("arthur", "external", "test-account")]?.data?.account!!
      account.name shouldBe "test-account"
      account.description shouldBe "Testing account"
      account.number shouldBe "1223344"
      account.path shouldBe listOf("external")
      account.url shouldBe "example.com"
      account.phone shouldBe "111-222-3344"
      account.address shouldBe "55 Road"
      account.userName shouldBe "john"
      account.password shouldBe "xxxyyy"
      account.owners shouldBe listOf("arthur")
      account.openedOn shouldBe NOV / 2019
      account.closedOn shouldBe MAR / 2020

      budget.nodeToStatement.size shouldBe 5
      val monthlyStatements = budget.nodeToStatement[budget.tree[listOf("arthur", "external", "test-account")]]!!
      monthlyStatements.keys shouldBe (NOV / 2019..APR / 2020).toSet()
      monthlyStatements.values.count { it.startBalance != null } shouldBe 1
      budget.months.size shouldBe 6
      budget.nodeToStatement.keys shouldBe setOf(
        budget.tree[listOf("arthur", "external", "inactive", "_test-account")],
        budget.tree[listOf("arthur", "external", "test-account")],
        budget.tree[listOf("arthur", "external", "inactive")],
        budget.tree[listOf("arthur", "external")],
        budget.tree[listOf("arthur")]
      )
    }

    it("account with balances") {
      val relativeFilePath = Paths.get("path/test.yaml")
      val accountData = YamlData(
        name = "test-account",
        path = listOf("external"),
        openedOn = JAN / 2020,
        owner = listOf("someone"),
        balances = listOf(
          BalanceYamlData(grp = FEB / 2020, date = LocalDate(2020, 2, 1), pamt = 10.0),
          BalanceYamlData(grp = JAN / 2020, date = LocalDate(2020, 1, 1), camt = 0.0)
        )
      )
      val budget = budget {
        loadYamlFile(this, accountData, relativeFilePath)
      }
      budget.tree shouldBe root(Profile()) {
        branch("someone", Profile()) {
          branch("external", Profile(isExternal = true)) {
            leaf("test-account", Profile(isExternal = true))
          }
        }
      }
      budget.nodeToStatement.size shouldBe 3
      budget.months.size shouldBe 2
      budget.nodeToStatement.values.sumOf {
        it.values.sumOf { s ->
          (s as? TransactionStatement)?.transactions?.size ?: 0
        }
      } shouldBe
        0

      val monthlyStatements = budget.nodeToStatement[budget.tree[listOf("someone", "external", "test-account")]]!!
      monthlyStatements.size shouldBe 2
      monthlyStatements[JAN / 2020]?.startBalance shouldBe
        Balance(0, LocalDate.parse("2020-01-01"), Balance.Type.CONFIRMED)
      monthlyStatements[FEB / 2020]?.startBalance shouldBe
        Balance(1000, LocalDate.parse("2020-02-01"), Balance.Type.PROJECTED)
    }

    it("fails without balance month") {
      val budgetBuilder = BudgetBuilder()
      val relativeFilePath = Paths.get("path/file.yaml")
      val accountData = YamlData(
        name = "test-account",
        path = listOf("external"),
        openedOn = JAN / 2020,
        owner = listOf("someone"),
        balances = listOf(
          BalanceYamlData(date = LocalDate(2020, 1, 1), camt = 0.0)
        )
      )
      val exception = shouldThrow<IllegalArgumentException> {
        loadYamlFile(budgetBuilder, accountData, relativeFilePath)
      }
      exception.message shouldBe "For test-account account balance entry with date '2020-01-01' " +
        "and desc 'null' has no grp setting, while processing path/file.yaml"
    }

    it("fails without balance date") {
      val budgetBuilder = BudgetBuilder()
      val relativeFilePath = Paths.get("path/file.yaml")
      val accountData = YamlData(
        name = "test-account",
        path = listOf("external"),
        openedOn = JAN / 2020,
        owner = listOf("someone"),
        balances = listOf(BalanceYamlData(grp = JAN / 2020, camt = 0.0))
      )

      val exception = shouldThrow<IllegalArgumentException> {
        loadYamlFile(budgetBuilder, accountData, relativeFilePath)
      }
      exception.message shouldBe "For test-account account balance for Jan2020 " +
        "does not have date set, while processing path/file.yaml"
    }

    it("fails without balance type") {
      val budgetBuilder = BudgetBuilder()
      val relativeFilePath = Paths.get("path/file.yaml")
      val accountData = YamlData(
        name = "test-account",
        path = listOf("external"),
        openedOn = JAN / 2020,
        owner = listOf("someone"),
        balances = listOf(
          BalanceYamlData(grp = JAN / 2020, date = LocalDate(2020, 1, 1))
        )
      )

      val exception = shouldThrow<IllegalArgumentException> {
        loadYamlFile(budgetBuilder, accountData, relativeFilePath)
      }
      exception.message shouldBe "For test-account account balance for Jan2020 " +
        "does not have amount type set, expected camt or pamt entry, while processing path/file.yaml"
    }

    it("with projected and confirmed transfers") {
      val relativeFilePath = Paths.get("path/file.yaml")
      val testAccountData = YamlData(
        name = "test-account",
        path = listOf("external"),
        openedOn = JAN / 2020,
        owner = listOf("someone"),
        balances = listOf(
          BalanceYamlData(grp = FEB / 2020, date = LocalDate(2020, 2, 1), pamt = 10.0),
          BalanceYamlData(grp = JAN / 2020, date = LocalDate(2020, 1, 1), camt = 0.0)
        ),
        transfersTo = mapOf(
          "external" to listOf(
            TransferYamlData(
              grp = JAN / 2020,
              date = LocalDate(2020, 1, 17),
              pamt = 37.5
            ),
            TransferYamlData(
              grp = JAN / 2020,
              date = LocalDate(2020, 1, 15),
              camt = -22.48
            )
          )
        )
      )

      val externalAccountData = YamlData(
        name = "external",
        path = listOf("external"),
        openedOn = JAN / 2020,
        owner = listOf("someone")
      )

      val budget = budget {
        loadYamlFile(this, testAccountData, relativeFilePath)
        loadYamlFile(this, externalAccountData, relativeFilePath)
      }
      budget.months.size shouldBe 2
      budget.tree shouldBe root(Profile()) {
        branch("someone", Profile()) {
          branch("external", Profile(isExternal = true)) {
            leaf("external", Profile(isExternal = true))
            leaf("test-account", Profile(isExternal = true))
          }
        }
      }
      budget.nodeToStatement.size shouldBe 4
      budget.nodeToStatement.keys shouldBe setOf(
        budget.tree[listOf("someone", "external")],
        budget.tree[listOf("someone")],
        budget.tree[listOf("someone", "external", "test-account")],
        budget.tree[listOf("someone", "external", "external")]
      )
      budget.nodeToStatement[budget.tree[listOf("someone", "external")]]?.keys shouldBe setOf(FEB / 2020, JAN / 2020)
      budget.nodeToStatement[budget.tree[listOf("someone")]]?.keys shouldBe setOf(FEB / 2020, JAN / 2020)

      val testAccountNode = budget.tree[listOf("someone", "external", "test-account")]
      val testAccountMonthlyStatements = budget.nodeToStatement[testAccountNode]!!

      testAccountMonthlyStatements.size shouldBe 2
      testAccountMonthlyStatements.values.count { it.startBalance != null } shouldBe 2

      val testAccountStatement = testAccountMonthlyStatements[JAN / 2020]!! as TransactionStatement
      testAccountStatement.transactions shouldBe setOf(
        Transaction(
          targetTreeNode = budget.tree[listOf("someone", "external", "external")]!!,
          balance = Balance(-3750, LocalDate.parse("2020-01-17"), Balance.Type.PROJECTED),
          description = null,
          type = Transaction.Type.EXPENSE
        ),
        Transaction(
          targetTreeNode = budget.tree[listOf("someone", "external", "external")]!!,
          balance = Balance(2248, LocalDate.parse("2020-01-15"), Balance.Type.CONFIRMED),
          description = null,
          type = Transaction.Type.INCOME
        )
      )
      testAccountStatement.balanceFromStart shouldBe listOf(-1502, 2248)
      val externalAccountNode = budget.tree[listOf("someone", "external", "external")]
      val externalAccountMonthlyStatements = budget.nodeToStatement[externalAccountNode]!!
      externalAccountMonthlyStatements.size shouldBe 2
      val externalAccountStatement = externalAccountMonthlyStatements[JAN / 2020]!! as TransactionStatement
      externalAccountStatement.transactions shouldBe setOf(
        Transaction(
          targetTreeNode = budget.tree[listOf("someone", "external", "test-account")]!!,
          balance = Balance(3750, LocalDate.parse("2020-01-17"), Balance.Type.PROJECTED),
          description = null,
          type = Transaction.Type.INCOME,
        ),
        Transaction(
          targetTreeNode = budget.tree[listOf("someone", "external", "test-account")]!!,
          balance = Balance(-2248, LocalDate.parse("2020-01-15"), Balance.Type.CONFIRMED),
          description = null,
          type = Transaction.Type.EXPENSE,
        )
      )
    }

    it("fails with transfer and no grp") {
      val budgetBuilder = BudgetBuilder()
      val relativeFilePath = Paths.get("path/test.yaml")
      val testAccountData = YamlData(
        name = "test-account",
        path = listOf("external"),
        openedOn = JAN / 2020,
        owner = listOf("someone"),
        transfersTo = mapOf(
          "external" to listOf(
            TransferYamlData(
              date = LocalDate(2020, 1, 17),
              pamt = 37.5
            )
          )
        )
      )

      val exception = shouldThrow<IllegalArgumentException> {
        loadYamlFile(budgetBuilder, testAccountData, relativeFilePath)
      }
      exception.message shouldBe "For account 'test-account' transfer to 'external' " +
        "does not have 'grp' field, while processing path/test.yaml"
    }

    it("fails with transfer and no date") {
      val budgetBuilder = BudgetBuilder()
      val relativeFilePath = Paths.get("path/test.yaml")
      val testAccountData = YamlData(
        name = "test-account",
        path = listOf("external"),
        openedOn = JAN / 2020,
        owner = listOf("someone"),
        transfersTo = mapOf(
          "external" to listOf(
            TransferYamlData(
              grp = JAN / 2020,
              pamt = 37.5
            )
          )
        )
      )

      val exception = shouldThrow<IllegalArgumentException> {
        loadYamlFile(budgetBuilder, testAccountData, relativeFilePath)
      }
      exception.message shouldBe "For account 'test-account' transfer to 'external' " +
        "does not have a valid 'date' field, while processing path/test.yaml"
    }

    it("fails with transfer and too far apart dates") {
      val budgetBuilder = BudgetBuilder()
      val relativeFilePath = Paths.get("path/test.yaml")
      val testAccountData = YamlData(
        name = "test-account",
        path = listOf("external"),
        openedOn = JAN / 2020,
        owner = listOf("someone"),
        transfersTo = mapOf(
          "external" to listOf(
            TransferYamlData(
              grp = JAN / 2020,
              date = LocalDate(2020, 4, 1),
              pamt = 37.5
            )
          )
        )
      )

      val exception = shouldThrow<IllegalArgumentException> {
        loadYamlFile(budgetBuilder, testAccountData, relativeFilePath)
      }
      exception.message shouldBe "For account 'test-account' transfer to 'external' for Jan2020 date " +
        "2020-04-01 (Apr2020) are too far apart, while processing path/test.yaml"
    }

    it("fails with transfer and no balance") {
      val budgetBuilder = BudgetBuilder()
      val relativeFilePath = Paths.get("path/test.yaml")
      val testAccountData = YamlData(
        name = "test-account",
        path = listOf("external"),
        openedOn = JAN / 2020,
        owner = listOf("someone"),
        transfersTo = mapOf(
          "external" to listOf(
            TransferYamlData(
              grp = JAN / 2020,
              date = LocalDate(2020, 1, 17)
            )
          )
        )
      )

      val exception = shouldThrow<IllegalArgumentException> {
        loadYamlFile(budgetBuilder, testAccountData, relativeFilePath)
      }
      exception.message shouldBe "For account 'test-account' transfer to 'external' " +
        "for Jan2020 2020-01-17 does not have 'pamt' or 'camt' field, " +
        "while processing path/test.yaml"
    }
  }
})
