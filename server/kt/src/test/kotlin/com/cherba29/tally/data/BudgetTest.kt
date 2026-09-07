package com.cherba29.tally.data

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.MonthName.*
import com.cherba29.tally.utils.TreeNode
import com.cherba29.tally.utils.root
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class BudgetTest : DescribeSpec({
  describe("isClosed") {
    it("empty budget all accounts are closed") {
      val tree = root(Profile()) { }
      val budget = Budget(
        months = AUG / 2026..SEP / 2026, tree = tree, nodeToStatement = mapOf()
      )
      budget.isClosed(tree, AUG / 2026) shouldBe true
    }

    it("single open account") {
      val tree = root(Profile()) {
        leaf("test-account", Profile(
          account = Account(
            name = "test-account", path = listOf(), owners = setOf("john"), openedOn = AUG / 2026
          )
        ))
      }
      val node = tree["test-account"] as TreeNode.Leaf
      val budget = Budget(
        months = AUG / 2026..SEP / 2026,
        tree = tree,
        nodeToStatement = mapOf()
      )
      budget.isClosed(tree, AUG / 2026) shouldBe false
      budget.isClosed(node, AUG / 2026) shouldBe false
      budget.isClosed(node, SEP / 2026) shouldBe false
      budget.isClosed(node, DEC / 2026) shouldBe false
      budget.isClosed(node, JUL / 2026) shouldBe true
    }

    it("nested open - closed accounts") {
      val tree = root(Profile()) {
        branch("internal", Profile()) {
          leaf("test-account1", Profile(
            Account(
              name = "test-account1", path = listOf("internal"), owners = setOf("john"), openedOn = AUG / 2026
            )
          ))
        }
        branch("external", Profile(isExternal = true)) {
          leaf("test-account2", Profile(
            Account(
              name = "test-account2",
              path = listOf("external"),
              owners = setOf("john"),
              openedOn = AUG / 2026,
              closedOn = AUG / 2026
            ),
            isExternal = true
          ))
        }
        leaf("test-account3", Profile(
          Account(
            name = "test-account3",
            path = listOf(),
            owners = setOf("john"),
            openedOn = AUG / 2026,
            closedOn = SEP / 2026
          )
        ))
      }
      val node1 = tree[listOf("internal", "test-account1")] as TreeNode.Leaf
      val internalNode = tree[listOf("internal")] as TreeNode.Branch
      val node2 = tree[listOf("external", "test-account2")] as TreeNode.Leaf
      val externalNode = tree[listOf("external")] as TreeNode.Branch
      val node3 = tree[listOf("test-account3")] as TreeNode.Leaf
      val budget = Budget(
        months = AUG / 2026..SEP / 2026, tree = tree, nodeToStatement = mapOf()
      )
      (JUL / 2026..DEC / 2026).associateWith { budget.isClosed(node1, it) } shouldBe mapOf(
        JUL / 2026 to true,
        AUG / 2026 to false,
        SEP / 2026 to false,
        OCT / 2026 to false,
        NOV / 2026 to false,
        DEC / 2026 to false
      )
      (JUL / 2026..DEC / 2026).associateWith { budget.isClosed(node2, it) } shouldBe mapOf(
        JUL / 2026 to true,
        AUG / 2026 to false,
        SEP / 2026 to true,
        OCT / 2026 to true,
        NOV / 2026 to true,
        DEC / 2026 to true
      )
      (JUL / 2026..DEC / 2026).associateWith { budget.isClosed(node3, it) } shouldBe mapOf(
        JUL / 2026 to true,
        AUG / 2026 to false,
        SEP / 2026 to false,
        OCT / 2026 to true,
        NOV / 2026 to true,
        DEC / 2026 to true
      )
      (JUL / 2026..DEC / 2026).associateWith { budget.isClosed(internalNode, it) } shouldBe mapOf(
        JUL / 2026 to true,
        AUG / 2026 to false,
        SEP / 2026 to false,
        OCT / 2026 to false,
        NOV / 2026 to false,
        DEC / 2026 to false
      )
      (JUL / 2026..DEC / 2026).associateWith { budget.isClosed(externalNode, it) } shouldBe mapOf(
        JUL / 2026 to true,
        AUG / 2026 to false,
        SEP / 2026 to true,
        OCT / 2026 to true,
        NOV / 2026 to true,
        DEC / 2026 to true
      )
      (JUL / 2026..DEC / 2026).associateWith { budget.isClosed(tree, it) } shouldBe mapOf(
        JUL / 2026 to true,
        AUG / 2026 to false,
        SEP / 2026 to false,
        OCT / 2026 to false,
        NOV / 2026 to false,
        DEC / 2026 to false
      )
    }
  }
})
