package com.cherba29.tally

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.MonthName.JAN
import com.cherba29.tally.core.MonthName.MAR
import com.cherba29.tally.data.Loader
import com.cherba29.tally.data.builder.budget
import com.cherba29.tally.testing.toSnapshot
import com.diffplug.selfie.coroutines.expectSelfie
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.datetime.LocalDate

class TableServiceTest : DescribeSpec({
  describe("buildGqlTable") {
    it("empty") {
      val account = Account(
        "test-account", owners = setOf("john"), path = listOf("internal"),
        openedOn = MAR / 2026,
      )
      val loader = mockk<Loader> {
        coEvery { budget() } returns budget {
          setAccount(listOf("john", "internal", "test-account1"), account)
        }
      }

      val table = TableService(loader).table("john", startMonth = MAR / 2026, endMonth = MAR / 2026)
      expectSelfie(table.toSnapshot()).toMatchDisk()
    }

    it("empty - no open accounts") {
      val accountPath1 = listOf("john", "external", "test-account1")
      val account1 = Account(
        name = "test-account1", path = listOf("external"), owners = setOf("john"),
        openedOn = MAR / 2026,
        closedOn = MAR / 2026
      )
      val accountPath2 = listOf("john", "external", "test-account2")
      val account2 = Account(
        name = "test-account2", path = listOf("external"), owners = setOf("john"),
        openedOn = MAR / 2026,
        closedOn = MAR / 2026
      )
      val payload = budget {
        setAccount(accountPath1, account1)
        setAccount(accountPath2, account2)
        addTransfer(
          fromAccountPath = accountPath1,
          fromMonth = MAR / 2026,
          toAccountName = "test-account2",
          toMonth = MAR / 2026,
          balance = Balance(
            amount = 100,
            date = LocalDate(2026, 3, 1),
            type = Balance.Type.CONFIRMED
          ),
          description = "test transfer"
        )
      }

      val loader = mockk<Loader> { coEvery { budget() } returns payload }
      val exception = shouldThrow<IllegalArgumentException> {
        TableService(loader).table("john", startMonth = JAN / 2026, endMonth = JAN / 2026)
      }
      exception.message shouldBe "Bad month range, budget has Mar2026..Mar2026 yet Jan2026..Jan2026 was requested"
    }

    it("single open account without path") {
      val accountPath = listOf("john", "external", "test-account")
      val account = Account(
        name = "test-account", path = listOf("external"), owners = setOf("john"),
        openedOn = JAN / 2026
      )
      val payload = budget {
        setAccount(accountPath, account)
        setBalance(
          accountPath, MAR / 2026, Balance(
            amount = 100,
            date = LocalDate(2026, 3, 1),
            type = Balance.Type.CONFIRMED
          )
        )
      }
      val loader = mockk<Loader> { coEvery { budget() } returns payload }
      val table = TableService(loader).table("john", startMonth = MAR / 2026, endMonth = MAR / 2026)
      expectSelfie(table.toSnapshot()).toMatchDisk()
    }

    it("single open account with path") {
      val accountPath = listOf("john", "internal", "test-account")
      val account = Account(
        name = "test-account", path = listOf("internal"), owners = setOf("john"),
        openedOn = JAN / 2026
      )
      val payload = budget {
        setAccount(accountPath, account)
        setBalance(
          accountPath, MAR / 2026, Balance(
            amount = 100,
            date = LocalDate(2026, 3, 1),
            type = Balance.Type.CONFIRMED
          )
        )
      }
      val loader = mockk<Loader> { coEvery { budget() } returns payload }
      val table = TableService(loader).table("john", startMonth = MAR / 2026, endMonth = MAR / 2026)
      expectSelfie(table.toSnapshot()).toMatchDisk()
    }
  }
})
