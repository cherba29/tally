package com.cherba29.tally.core

import com.cherba29.tally.core.MonthName.FEB
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate

class TransferTest : DescribeSpec({
  describe("Creation") {
    it("basic") {
      val account1 = Account(
        nodeId = NodeId(
          "test-account1",
          path = listOf("external"),
          owners = setOf("john")
        ),
        openedOn = FEB / 2020,
      )
      val account2 = Account(
        nodeId = NodeId(
          "test-account2",
          path = listOf("external"),
          owners = setOf("john")
        ),
        openedOn = FEB / 2020,
      )
      val month = Month(2020, 1)
      val balance = Balance(100, LocalDate(2020, 2, 3), Balance.Type.CONFIRMED)
      val transfer = Transfer(
        fromAccount = account1,
        toAccount = account2,
        fromMonth = month,
        toMonth = month,
        description = "test",
        balance
      )
      transfer.fromAccount.nodeId.name shouldBe "test-account1"
      transfer.toAccount.nodeId.name shouldBe "test-account2"
      transfer.fromMonth.toString() shouldBe "Feb2020"
      transfer.toMonth.toString() shouldBe "Feb2020"
      transfer.balance.amount shouldBe 100.0
      transfer.balance.date.toString() shouldBe "2020-02-03"
      transfer.description shouldBe "test"
    }
  }
})