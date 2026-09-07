package com.cherba29.tally

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.MonthName.APR
import com.cherba29.tally.core.MonthName.FEB
import com.cherba29.tally.core.MonthName.MAR
import com.cherba29.tally.utils.root
import com.cherba29.tally.data.Budget
import com.cherba29.tally.data.Loader
import com.cherba29.tally.data.Profile
import com.cherba29.tally.data.builder.BudgetBuilder
import com.cherba29.tally.data.builder.budget
import com.cherba29.tally.testing.toSnapshot
import com.diffplug.selfie.coroutines.expectSelfie
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk

class SummaryServiceTest : DescribeSpec({
  describe("buildSummaryData") {
    it("empty") {
      val account = Account("test-account1", owners = setOf("john"), path = listOf("internal"), openedOn = MAR / 2026)
      val loader = mockk<Loader> {
        coEvery { budget() } returns budget {
          setAccount(listOf("john", "internal", "test-account1"), account)
        }
      }

      val exception = shouldThrow<NotFoundException> {
        SummaryService(loader).summary(
          startMonth = APR / 2026,
          endMonth = APR / 2026,
          accountPath = "john/internal"
        )
      }
      exception.message shouldBe "Summary 'john/internal' for months [Apr2026, Apr2026] not found."
    }

    it("missing months") {
      val loader = mockk<Loader> {
        coEvery { budget() } returns Budget(
          months = MAR / 2026..MAR / 2026,
          tree = root(Profile()) {},
          nodeToStatement = mapOf()
        )
      }

      val exception = shouldThrow<NotFoundException> {
        SummaryService(loader).summary(
          startMonth = APR / 2026,
          endMonth = APR / 2026,
          accountPath = "john/internal"
        )
      }
      exception.message shouldBe "Summary 'john/internal' not found."
    }

    it("single") {
      val account = Account("test-account1", owners = setOf("john"), path = listOf("internal"), openedOn = MAR / 2026)
      val loader = mockk<Loader> {
        coEvery { budget() } returns budget {
          setAccount(listOf("john", "internal", "test-account1"), account)
          setBalance(
            listOf("john", "internal", "test-account1"),
            MAR / 2026,
            Balance.confirmed(100, "2026-03-01")
          )
        }
      }
      val data = SummaryService(loader).summary(
        startMonth = MAR / 2026,
        endMonth = MAR / 2026,
        accountPath = "john/internal"
      )
      expectSelfie(data.toSnapshot()).toMatchDisk()
    }

    it("with transaction statement") {
      val account1 = Account("test-account1", owners = setOf("john"), path = listOf("internal"), openedOn = MAR / 2026)
      val account2 = Account("test-account2", owners = setOf("john"), path = listOf("internal"), openedOn = MAR / 2026)
      val loader = mockk<Loader> {
        coEvery { budget() } returns budget {
          setAccount(listOf("john", "internal", "test-account1"), account1)
          setAccount(listOf("john", "internal", "test-account2"), account2)
          setBalance(listOf("john", "internal", "test-account1"), MAR / 2026, Balance.confirmed(100, "2026-03-01"))
          setBalance(listOf("john", "internal", "test-account2"), MAR / 2026, Balance.confirmed(200, "2026-03-01"))
          addTransfer(
            BudgetBuilder.TransferRecord(
              fromAccountPath = listOf("john", "internal", "test-account1"),
              toAccountName = "test-account2",
              month = MAR / 2026,
              balance = Balance.confirmed(50, "2026-03-02"),
              description = "transfer from 1 to 2",
              tags = listOf()
            )
          )
        }
      }

      val data = SummaryService(loader).summary(
        startMonth = MAR / 2026,
        endMonth = MAR / 2026,
        accountPath = "john/internal"
      )
      expectSelfie(data.toSnapshot()).toMatchDisk()
    }

    it("single with multiple transaction statement") {
      val account1 = Account("test-account1", owners = setOf("john"), path = listOf("internal"), openedOn = MAR / 2026)
      val account2 = Account("test-account2", owners = setOf("john"), path = listOf("external"), openedOn = MAR / 2026)

      val loader = mockk<Loader> {
        coEvery { budget() } returns budget {
          setAccount(listOf("john", "internal", "test-account1"), account1)
          setAccount(listOf("john", "external", "test-account2"), account2)
          setBalance(listOf("john", "internal", "test-account1"), MAR / 2026, Balance.confirmed(100, "2026-03-01"))
          setBalance(listOf("john", "external", "test-account2"), MAR / 2026, Balance.confirmed(200, "2026-03-01"))
          addTransfer(
            BudgetBuilder.TransferRecord(
              fromAccountPath = listOf("john", "internal", "test-account1"),
              toAccountName = "test-account2",
              month = MAR / 2026,
              balance = Balance.confirmed(50, "2026-04-02"),
              description = "transfer from 1 to 2",
              tags = listOf()
            )
          )
        }
      }
      val data = SummaryService(loader).summary(
        startMonth = MAR / 2026,
        endMonth = MAR / 2026,
        accountPath = "john/internal"
      )
      expectSelfie(data.toSnapshot()).toMatchDisk()
    }

    it("multiple months") {
      val account1 = Account("test-account1", owners = setOf("john"), path = listOf("internal"), openedOn = MAR / 2026)
      val account2 = Account("test-account2", owners = setOf("john"), path = listOf("external"), openedOn = MAR / 2026)

      val loader = mockk<Loader> {
        coEvery { budget() } returns budget {
          setAccount(listOf("john", "internal", "test-account1"), account1)
          setAccount(listOf("john", "external", "test-account2"), account2)
          setBalance(listOf("john", "internal", "test-account1"), MAR / 2026, Balance.confirmed(100, "2026-03-01"))
          setBalance(listOf("john", "external", "test-account2"), MAR / 2026, Balance.confirmed(200, "2026-03-01"))
          setBalance(listOf("john", "internal", "test-account1"), APR / 2026, Balance.confirmed(150, "2026-04-01"))
          setBalance(listOf("john", "external", "test-account2"), APR / 2026, Balance.confirmed(250, "2026-04-01"))
          addTransfer(
            BudgetBuilder.TransferRecord(
              fromAccountPath = listOf("john", "internal", "test-account1"),
              toAccountName = "test-account2",
              month = MAR / 2026,
              balance = Balance.confirmed(50, "2026-03-02"),
              description = "transfer from 1 to 2",
              tags = listOf()
            )
          )
          addTransfer(
            BudgetBuilder.TransferRecord(
              fromAccountPath = listOf("john", "external", "test-account2"),
              toAccountName = "test-account1",
              month = APR / 2026,
              balance = Balance.confirmed(75, "2026-04-02"),
              description = "transfer from 2 to 1",
              tags = listOf()
            )
          )
        }
      }

      val data = SummaryService(loader).summary(
        startMonth = MAR / 2026,
        endMonth = APR / 2026,
        accountPath = "john/internal"
      )
      expectSelfie(data.toSnapshot()).toMatchDisk()
    }

    it("multiple months null start month") {
      val account1 = Account("test-account1", owners = setOf("john"), path = listOf("internal"), openedOn = MAR / 2026)
      val account2 = Account("test-account2", owners = setOf("john"), path = listOf("external"), openedOn = MAR / 2026)

      val loader = mockk<Loader> {
        coEvery { budget() } returns budget {
          setAccount(listOf("john", "internal", "test-account1"), account1)
          setAccount(listOf("john", "external", "test-account2"), account2)
          setBalance(listOf("john", "internal", "test-account1"), MAR / 2026, Balance.confirmed(100, "2026-03-01"))
          setBalance(listOf("john", "external", "test-account2"), MAR / 2026, Balance.confirmed(200, "2026-03-01"))
          setBalance(listOf("john", "internal", "test-account1"), APR / 2026, Balance.confirmed(150, "2026-04-01"))
          setBalance(listOf("john", "external", "test-account2"), APR / 2026, Balance.confirmed(250, "2026-04-01"))
          addTransfer(
            BudgetBuilder.TransferRecord(
              fromAccountPath = listOf("john", "internal", "test-account1"),
              toAccountName = "test-account2",
              month = MAR / 2026,
              balance = Balance.confirmed(50, "2026-03-02"),
              description = "transfer from 1 to 2",
              tags = listOf()
            )
          )
          addTransfer(
            BudgetBuilder.TransferRecord(
              fromAccountPath = listOf("john", "external", "test-account2"),
              toAccountName = "test-account1",
              month = APR / 2026,
              balance = Balance.confirmed(75, "2026-04-02"),
              description = "transfer from 2 to 1",
              tags = listOf()
            )
          )
        }
      }
      val data = SummaryService(loader).summary(
        startMonth = null,
        endMonth = MAR / 2026,
        accountPath = "john/internal"
      )
      expectSelfie(data.toSnapshot()).toMatchDisk()
    }

    it("closed accounts excluded") {
      val account1 = Account("test-account1", owners = setOf("john"), path = listOf("internal"), openedOn = MAR / 2025, closedOn = APR / 2025)
      val account2 = Account("test-account2", owners = setOf("john"), path = listOf("internal"), openedOn = MAR / 2025)

      val path1 = listOf("john", "internal", "test-account1")
      // Test with subcategory so it passes closed test.
      val path2 = listOf("john", "internal", "subcategory", "test-account2")
      val loader = mockk<Loader> {
        coEvery { budget() } returns budget {
          setAccount(path1, account1)
          setAccount(path2, account2)
          setBalance(path1, MAR / 2025, Balance.confirmed(100, "2026-03-01"))
          setBalance(path2, MAR / 2026, Balance.confirmed(200, "2026-03-01"))
          setBalance(path1, APR / 2025, Balance.confirmed(150, "2026-04-01"))
          setBalance(path2, APR / 2026, Balance.confirmed(250, "2026-04-01"))
          addTransfer(
            BudgetBuilder.TransferRecord(
              fromAccountPath = path1,
              toAccountName = "test-account2",
              month = MAR / 2025,
              balance = Balance.confirmed(50, "2026-03-02"),
              description = "transfer from 1 to 2",
              tags = listOf()
            )
          )
          addTransfer(
            BudgetBuilder.TransferRecord(
              fromAccountPath = path2,
              toAccountName = "test-account1",
              month = APR / 2025,
              balance = Balance.confirmed(75, "2026-04-02"),
              description = "transfer from 2 to 1",
              tags = listOf()
            )
          )
        }
      }
      val service = SummaryService(loader)
      val data = service.summary(
        startMonth = MAR / 2026,
        endMonth = MAR / 2026,
        accountPath = "john/internal"
      )
      expectSelfie(data.toSnapshot()).toMatchDisk()
    }
  }

  describe("transfers summary") {
    it("with single transaction statement") {
      val account1 = Account("test-account1", owners = setOf("john"), path = listOf("internal"), openedOn = MAR / 2026)
      val account2 = Account("test-account2", owners = setOf("john"), path = listOf("internal"), openedOn = MAR / 2026)
      val loader = mockk<Loader> {
        coEvery { budget() } returns budget {
          setAccount(listOf("john", "internal", "test-account1"), account1)
          setAccount(listOf("john", "internal", "test-account2"), account2)
          setBalance(listOf("john", "internal", "test-account1"), MAR / 2026, Balance.confirmed(100, "2026-03-01"))
          setBalance(listOf("john", "internal", "test-account2"), MAR / 2026, Balance.confirmed(200, "2026-03-01"))
          addTransfer(
            BudgetBuilder.TransferRecord(
              fromAccountPath = listOf("john", "internal", "test-account1"),
              toAccountName = "test-account2",
              month = MAR / 2026,
              balance = Balance.confirmed(50, "2026-03-02"),
              description = "transfer from 1 to 2",
              tags = listOf()
            )
          )
        }
      }

      val data = SummaryService(loader).transfersSummary(
        startMonth = MAR / 2026,
        endMonth = MAR / 2026,
        accountPath = "john/internal/test-account1"
      )
      expectSelfie(data.toSnapshot()).toMatchDisk()
    }
    it("transfers summary multiple months") {
      val account1 = Account("test-account1", owners = setOf("john"), path = listOf("internal"), openedOn = FEB / 2026)
      val account2 = Account("test-account2", owners = setOf("john"), path = listOf("external"), openedOn = MAR / 2026)
      val account3 = Account("test-account3", owners = setOf("john"), path = listOf("internal"), openedOn = FEB / 2026)
      val loader = mockk<Loader> {
        coEvery { budget() } returns budget {
          setAccount(listOf("john", "internal", "test-account1"), account1)
          setAccount(listOf("john", "external", "test-account2"), account2)
          setAccount(listOf("john", "internal", "test-account3"), account3)
          setBalance(listOf("john", "internal", "test-account1"), FEB / 2026, Balance.confirmed(0, "2026-02-01"))
          setBalance(listOf("john", "internal", "test-account1"), MAR / 2026, Balance.confirmed(100, "2026-03-01"))
          setBalance(listOf("john", "external", "test-account2"), MAR / 2026, Balance.confirmed(200, "2026-03-01"))
          setBalance(listOf("john", "external", "test-account2"), APR / 2026, Balance.confirmed(200, "2026-04-01"))
          addTransfer(
            BudgetBuilder.TransferRecord(
              fromAccountPath = listOf("john", "internal", "test-account3"),
              toAccountName = "test-account1",
              month = FEB / 2026,
              balance = Balance.confirmed(100, "2026-02-02"),
              description = "initial",
              tags = listOf()
            )
          )
          addTransfer(
            BudgetBuilder.TransferRecord(
              fromAccountPath = listOf("john", "internal", "test-account3"),
              toAccountName = "test-account1",
              month = MAR / 2026,
              balance = Balance.confirmed(10, "2026-03-02"),
              description = "contribution",
              tags = listOf()
            )
          )
          addTransfer(
            BudgetBuilder.TransferRecord(
              fromAccountPath = listOf("john", "external", "test-account2"),
              toAccountName = "test-account1",
              month = MAR / 2026,
              balance = Balance.confirmed(20, "2026-03-02"),
              description = "gain",
              tags = listOf()
            )
          )
        }
      }

      val data = SummaryService(loader).transfersSummary(
        startMonth = FEB / 2026,
        endMonth = MAR / 2026,
        accountPath = "john/internal/test-account1"
      )
      expectSelfie(data.toSnapshot()).toMatchDisk()
    }
  }
})
