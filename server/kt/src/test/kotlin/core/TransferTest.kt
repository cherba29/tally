package com.cherba29.tally.core

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate

class TransferTest : DescribeSpec({
  describe("Creation") {
    it("basic") {
      val account1 = Account(
        name = "test-account1",
        type = AccountType.CHECKING,
        owners = listOf("john"),
      )
      val account2 = Account(
        name = "test-account2",
        type = AccountType.CREDIT,
        owners = listOf("john"),
      )
      val month = Month(2020, 1)
      val balance = Balance(100.0, LocalDate(2020, 2, 3), BalanceType.CONFIRMED)
      val transfer = Transfer(
        fromAccount = account1,
        toAccount = account2,
        fromMonth = month,
        toMonth = month,
        description = "test",
        balance
      )
      transfer.fromAccount.name shouldBe "test-account1"
      transfer.toAccount.name shouldBe "test-account2"
      transfer.fromMonth.toString() shouldBe "Feb2020"
      transfer.toMonth.toString() shouldBe "Feb2020"
      transfer.balance.amount shouldBe 100.0
      transfer.balance.date.toString() shouldBe "2020-02-03"
      transfer.description shouldBe "test"
    }
  }
})