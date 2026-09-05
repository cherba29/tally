package com.cherba29.tally.data.builder

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.MonthName.JUL
import com.cherba29.tally.core.TreeNode
import com.cherba29.tally.core.root
import com.cherba29.tally.core.Transaction
import com.cherba29.tally.data.Profile
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate

class TransactionStatementBuilderTest : DescribeSpec({
  describe("Creation") {
    it("empty") {
      val testMonthRange = JUL / 2026..JUL / 2026
      val builder = TransactionStatementBuilder()
      builder.month = testMonthRange.first

      val transactionStatement = builder.build()
      transactionStatement.monthRange shouldBe testMonthRange
      transactionStatement.startBalance shouldBe null
      transactionStatement.endBalance shouldBe null
      transactionStatement.change shouldBe null
      transactionStatement.transactions shouldBe listOf()
    }

    it("single transfer") {
      val testTree = root(Profile()) {
        leaf("test-account1", Profile())
        leaf("test-account2", Profile())
      }
      val testTreeLeafNode2 = testTree["test-account2"] as TreeNode.Leaf
      val testMonthRange = JUL / 2026..JUL / 2026
      val testTransferBalance = Balance(
        amount = 100,
        date = LocalDate(2026, 7, 4),
        type = Balance.Type.CONFIRMED,
        description = "test transfer"
      )
      val testTransfer = Transaction(
        targetTreeNode = testTreeLeafNode2,
        description = "test transfer",
        balance = -testTransferBalance,
        type = Transaction.Type.TRANSFER
      )
      val builder = TransactionStatementBuilder()
      builder.month = testMonthRange.first
      builder.addTransfer(testTransfer)
      val transactionStatement = builder.build()
      transactionStatement.monthRange shouldBe testMonthRange
      transactionStatement.startBalance shouldBe null
      transactionStatement.endBalance shouldBe null
      transactionStatement.change shouldBe null
      transactionStatement.transactions shouldBe listOf(
        Transaction(
          targetTreeNode = testTreeLeafNode2,
          balance = -testTransferBalance,
          description = "test transfer",
          type = Transaction.Type.TRANSFER
        )
      )
    }
    it("to and from transfers") {
      val testTree = root(Profile()) {
        leaf("test-account1", Profile())
        leaf("test-account2", Profile())
      }
      val testTreeLeafNode2 = testTree["test-account2"] as TreeNode.Leaf
      val testMonthRange = JUL / 2026..JUL / 2026
      val testTransferBalance = Balance(
        amount = 100,
        date = LocalDate(2026, 7, 4),
        type = Balance.Type.CONFIRMED,
        description = "test transfer"
      )
      val testTransferFrom = Transaction(
        targetTreeNode = testTreeLeafNode2,
        description = "test transfer 1->2",
        balance = -testTransferBalance,
        type = Transaction.Type.TRANSFER
      )
      val testTransferTo = Transaction(
        targetTreeNode = testTreeLeafNode2,
        description = "test transfer 2->1",
        balance = testTransferBalance,
        type = Transaction.Type.TRANSFER
      )
      val testStartBalance = Balance(1000, LocalDate(2026, 7, 1), Balance.Type.PROJECTED)
      val builder = TransactionStatementBuilder()
      builder.month = testMonthRange.first
      builder.startBalance = testStartBalance
      builder.addTransfer(testTransferFrom)
      builder.addTransfer(testTransferTo)
      val transactionStatement = builder.build()
      transactionStatement.monthRange shouldBe testMonthRange
      transactionStatement.startBalance shouldBe testStartBalance
      transactionStatement.endBalance shouldBe null
      transactionStatement.change shouldBe null
      transactionStatement.transactions shouldBe listOf(
        Transaction(
          targetTreeNode = testTreeLeafNode2,
          balance = testTransferBalance,
          description = "test transfer 2->1",
          type = Transaction.Type.TRANSFER
        ),
        Transaction(
          targetTreeNode = testTreeLeafNode2,
          balance = -testTransferBalance,
          description = "test transfer 1->2",
          type = Transaction.Type.TRANSFER
        ),
      )
      transactionStatement.balanceFromStart shouldBe listOf(1000, 900)
    }
  }
})
