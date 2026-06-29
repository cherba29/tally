package com.cherba29.tally.core

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.comparables.shouldNotBeLessThan
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate

class TransferTest : DescribeSpec({
  describe("Creation") {
    it("basic") {
      val tree = root {
        branch("john") {
          branch("external") {
            leaf("test-account1")
            leaf("test-account2")
          }
        }
      }
      val month = Month(2020, 1)
      val balance = Balance(100, LocalDate(2020, 2, 3), Balance.Type.CONFIRMED)
      val transfer = Transfer(
        fromAccount = tree[listOf("john", "external", "test-account1")] as TreeNode.Leaf,
        toAccount = tree[listOf("john", "external", "test-account2")] as TreeNode.Leaf,
        fromMonth = month,
        toMonth = month,
        description = "test",
        balance
      )
      transfer.fromAccount.name shouldBe "test-account1"
      transfer.toAccount.name shouldBe "test-account2"
      transfer.fromMonth.toString() shouldBe "Feb2020"
      transfer.toMonth.toString() shouldBe "Feb2020"
      transfer.balance.amount shouldBe 100L
      transfer.balance.date.toString() shouldBe "2020-02-03"
      transfer.description shouldBe "test"
    }
  }
  describe("order") {
    it("basic") {
      val tree = root {
        branch("john") {
          branch("external") {
            leaf("test-account1")
            leaf("test-account2")
          }
        }
      }
      val month = Month(2020, 1)
      val balance = Balance(100, LocalDate(2020, 2, 3), Balance.Type.CONFIRMED)
      val transfer1 = Transfer(
        fromAccount = tree[listOf("john", "external", "test-account1")] as TreeNode.Leaf,
        toAccount = tree[listOf("john", "external", "test-account2")] as TreeNode.Leaf,
        fromMonth = month,
        toMonth = month,
        description = "test",
        balance
      )
      val transfer2 = Transfer(
        fromAccount = tree[listOf("john", "external", "test-account1")] as TreeNode.Leaf,
        toAccount = tree[listOf("john", "external", "test-account2")] as TreeNode.Leaf,
        fromMonth = month,
        toMonth = month,
        description = "test",
        balance
      )
      transfer1 shouldNotBeLessThan transfer2
      transfer2 shouldNotBeLessThan transfer1
      transfer1 shouldBe transfer2
    }
  }
})
