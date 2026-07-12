package com.cherba29.tally.data.builder

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.MonthName.JUL
import com.cherba29.tally.core.Transfer
import com.cherba29.tally.core.TreeNode
import com.cherba29.tally.core.root
import com.cherba29.tally.statement.Transaction
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate

class TransactionStatementBuilderTest : DescribeSpec({
  describe("Creation") {
    it("empty") {
      val testTree = root {
        leaf("test-account")
      }
      val testTreeLeafNode = testTree["test-account"] as TreeNode.Leaf
      val testMonthRange = JUL / 2026..JUL / 2026
      val builder = TransactionStatementBuilder()
      builder.treeNode = testTreeLeafNode
      builder.month = testMonthRange.first
      builder.isClosed = false

      val transactionStatement = builder.build()
      transactionStatement.treeNode shouldBe testTreeLeafNode
      transactionStatement.monthRange shouldBe testMonthRange
      transactionStatement.startBalance shouldBe null
      transactionStatement.endBalance shouldBe null
      transactionStatement.change shouldBe null
      transactionStatement.transactions shouldBe listOf()
    }

    it("bad to from accounts") {
      val testTree = root {
        leaf("test-account1")
        leaf("test-account2")
        leaf("test-account3")
      }
      val testTreeLeafNode1 = testTree["test-account1"] as TreeNode.Leaf
      val testTreeLeafNode2 = testTree["test-account2"] as TreeNode.Leaf
      val testTreeLeafNode3 = testTree["test-account3"] as TreeNode.Leaf
      val testMonthRange = JUL / 2026..JUL / 2026
      val testTransferBalance = Balance(
        amount = 100,
        date = LocalDate(2026, 7, 4),
        type = Balance.Type.CONFIRMED,
        description = "test transfer"
      )
      val testTransfer = Transfer(
        fromAccount = testTreeLeafNode1,
        toAccount = testTreeLeafNode2,
        month = JUL / 2026,
        description = "test transfer",
        balance = testTransferBalance,
        tags = listOf()
      )
      val builder = TransactionStatementBuilder()
      builder.treeNode = testTreeLeafNode3
      builder.month = testMonthRange.first
      builder.isClosed = false
      val error = shouldThrow<IllegalStateException> {
        builder.addTransfer(testTransfer)
      }
      error.message shouldBe "Setting transfer from (test-account1 to test-account2) for " +
        "'test-account3' account statement!"
    }

    it("single transfer") {
      val testTree = root {
        leaf("test-account1")
        leaf("test-account2")
      }
      val testTreeLeafNode1 = testTree["test-account1"] as TreeNode.Leaf
      val testTreeLeafNode2 = testTree["test-account2"] as TreeNode.Leaf
      val testMonthRange = JUL / 2026..JUL / 2026
      val testTransferBalance = Balance(
        amount = 100,
        date = LocalDate(2026, 7, 4),
        type = Balance.Type.CONFIRMED,
        description = "test transfer"
      )
      val testTransfer = Transfer(
        fromAccount = testTreeLeafNode1,
        toAccount = testTreeLeafNode2,
        month = JUL / 2026,
        description = "test transfer",
        balance = testTransferBalance,
        tags = listOf()
      )
      val builder = TransactionStatementBuilder()
      builder.treeNode = testTreeLeafNode1
      builder.month = testMonthRange.first
      builder.isClosed = false
      builder.addTransfer(testTransfer)
      val transactionStatement = builder.build()
      transactionStatement.treeNode shouldBe testTreeLeafNode1
      transactionStatement.monthRange shouldBe testMonthRange
      transactionStatement.startBalance shouldBe null
      transactionStatement.endBalance shouldBe null
      transactionStatement.change shouldBe null
      transactionStatement.transactions shouldBe listOf(
        Transaction(
          treeNode = testTreeLeafNode2,
          balance = -testTransferBalance,
          description = "test transfer",
          type = Transaction.Type.EXPENSE,
          balanceFromStart = null
        )
      )
    }
    it("to and from transfers") {
      val testTree = root {
        leaf("test-account1")
        leaf("test-account2")
      }
      val testTreeLeafNode1 = testTree["test-account1"] as TreeNode.Leaf
      val testTreeLeafNode2 = testTree["test-account2"] as TreeNode.Leaf
      val testMonthRange = JUL / 2026..JUL / 2026
      val testTransferBalance = Balance(
        amount = 100,
        date = LocalDate(2026, 7, 4),
        type = Balance.Type.CONFIRMED,
        description = "test transfer"
      )
      val testTransferFrom = Transfer(
        fromAccount = testTreeLeafNode1,
        toAccount = testTreeLeafNode2,
        month = JUL / 2026,
        description = "test transfer",
        balance = testTransferBalance,
        tags = listOf()
      )
      val testTransferTo = Transfer(
        fromAccount = testTreeLeafNode2,
        toAccount = testTreeLeafNode1,
        month = JUL / 2026,
        description = "test transfer",
        balance = testTransferBalance,
        tags = listOf()
      )
      val testStartBalance = Balance(1000, LocalDate(2026, 7, 1), Balance.Type.PROJECTED)
      val builder = TransactionStatementBuilder()
      builder.treeNode = testTreeLeafNode1
      builder.month = testMonthRange.first
      builder.isClosed = false
      builder.startBalance = testStartBalance
      builder.addTransfer(testTransferFrom)
      builder.addTransfer(testTransferTo)
      val transactionStatement = builder.build()
      transactionStatement.treeNode shouldBe testTreeLeafNode1
      transactionStatement.monthRange shouldBe testMonthRange
      transactionStatement.startBalance shouldBe testStartBalance
      transactionStatement.endBalance shouldBe null
      transactionStatement.change shouldBe null
      transactionStatement.transactions shouldBe listOf(
        Transaction(
          treeNode = testTreeLeafNode2,
          balance = testTransferBalance,
          description = "test transfer",
          type = Transaction.Type.INCOME,
          balanceFromStart = 1000
        ),
        Transaction(
          treeNode = testTreeLeafNode2,
          balance = -testTransferBalance,
          description = "test transfer",
          type = Transaction.Type.EXPENSE,
          balanceFromStart = 900
        )
      )
    }
  }
})
