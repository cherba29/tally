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
      val transactionStatement = builder.fromTransfers(
        leafTreeNode = testTreeLeafNode,
        monthRange = testMonthRange,
        isClosed = false,
        transfers = listOf(),
        startBalance = null
      )
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
        fromMonth = JUL / 2026,
        toMonth = JUL / 2026,
        description = "test transfer",
        balance = testTransferBalance
      )
      val builder = TransactionStatementBuilder()
      val error = shouldThrow<IllegalStateException> {
        builder.fromTransfers(
          leafTreeNode = testTreeLeafNode3,
          monthRange = testMonthRange,
          isClosed = false,
          transfers = listOf(testTransfer),
          startBalance = null
        )
      }
      error.message shouldBe "Setting transfer from (test-account1 to test-account2) for 'test-account3' account statement!"
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
        fromMonth = JUL / 2026,
        toMonth = JUL / 2026,
        description = "test transfer",
        balance = testTransferBalance
      )
      val builder = TransactionStatementBuilder()
      val transactionStatement = builder.fromTransfers(
        leafTreeNode = testTreeLeafNode1,
        monthRange = testMonthRange,
        isClosed = false,
        transfers = listOf(testTransfer),
        startBalance = null
      )
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
        fromMonth = JUL / 2026,
        toMonth = JUL / 2026,
        description = "test transfer",
        balance = testTransferBalance
      )
      val testTransferTo = Transfer(
        fromAccount = testTreeLeafNode2,
        toAccount = testTreeLeafNode1,
        fromMonth = JUL / 2026,
        toMonth = JUL / 2026,
        description = "test transfer",
        balance = testTransferBalance
      )
      val testStartBalance = Balance(1000, LocalDate(2026, 7, 1), Balance.Type.PROJECTED)
      val builder = TransactionStatementBuilder()
      val transactionStatement = builder.fromTransfers(
        leafTreeNode = testTreeLeafNode1,
        monthRange = testMonthRange,
        isClosed = false,
        transfers = listOf(testTransferFrom, testTransferTo),
        startBalance = testStartBalance
      )
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
        ),
      )
    }
  }
})
