package com.cherba29.tally

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.MonthName.MAR
import com.cherba29.tally.data.Loader
import com.cherba29.tally.data.builder.budget
import com.cherba29.tally.testing.toSnapshot
import com.diffplug.selfie.coroutines.expectSelfie
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.coEvery
import io.mockk.mockk

class TableServiceTest : DescribeSpec({
  describe("buildGqlTable") {
    it("empty") {
      val account = Account(
        "test-account", isSummary = false, owners = setOf("john"), path = listOf("internal"),
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
  }
})
