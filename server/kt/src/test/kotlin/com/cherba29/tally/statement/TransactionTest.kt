package com.cherba29.tally.statement

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.MonthName.DEC
import com.cherba29.tally.core.NodeId
import com.cherba29.tally.data.budget
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class TransactionTest : DescribeSpec({
  describe("Build") {
    it("bad account name on transfer") {
      val path1 = listOf("john", "external", "test-account1")
      val node1 = NodeId(
        name = "test-account1",
        path = listOf("external"),
        owners = setOf("john")
      )
      val account1 = Account(node1, openedOn = DEC / 2021)
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
  }
})