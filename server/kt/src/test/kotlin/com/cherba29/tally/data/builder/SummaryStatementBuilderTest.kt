package com.cherba29.tally.data.builder

import com.cherba29.tally.core.MonthName.MAY
import com.cherba29.tally.core.root
import com.cherba29.tally.statement.Statement
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe


class SummaryStatementBuilderTest : DescribeSpec({
  describe("Creation") {
    it("basic") {
      val builder = SummaryStatementBuilder()
      builder.build(root {}).isEmpty() shouldBe true
    }
  }
  describe("statements") {
    it("add single") {
      val tree = root {
        branch("john") {
          branch("internal") {
            leaf("test-account")
          }
        }
      }
      val builder = SummaryStatementBuilder()
      builder.addStatement(
        statement = Statement(
          tree[listOf("john", "internal", "test-account")]!!,
          monthRange = MAY / 2026..MAY / 2026,
        )
      )
      val summaryStatements = builder.build(tree)
      summaryStatements.isEmpty() shouldBe false
      summaryStatements.keys.map { it.path }.toSet() shouldBe setOf(listOf("john", "internal"), listOf("john"))
      val treeNode = tree[listOf("john", "internal")]!!
      val stmt = summaryStatements[treeNode]!![MAY/2026]!!
      stmt.treeNode.path shouldBe listOf("john", "internal")
      stmt.treeNode.isExternal shouldBe false
      stmt.treeNode.children.isNotEmpty() shouldBe true
      stmt.monthRange shouldBe MAY / 2026 .. MAY / 2026
      stmt.statements.size shouldBe 1
      val subStatement = stmt.statements.first()
      subStatement.treeNode.name shouldBe "test-account"
      subStatement.monthRange shouldBe MAY / 2026 .. MAY / 2026
      subStatement.treeNode.path shouldBe listOf("john", "internal", "test-account")
    }

    it("add single and propagate up") {
      val tree = root {
        branch("john") {
          branch("internal") {
            leaf("test-account")
          }
        }
      }

      val aggregator = SummaryStatementBuilder()
      aggregator.addStatement(
        statement = Statement(
          tree[listOf("john", "internal", "test-account")]!!,
          monthRange = MAY / 2026 .. MAY / 2026,
        )
      )
      val summaryStatements = aggregator.build(tree)

      summaryStatements.isEmpty() shouldBe false
      summaryStatements.size shouldBe 2
      summaryStatements.keys.map { it.path }.toSet() shouldBe setOf(listOf("john", "internal"), listOf("john"))
      val stmt = summaryStatements[tree[listOf("john", "internal")]]!![MAY / 2026]!!
      stmt.treeNode.isExternal shouldBe false
      stmt.treeNode.children.isNotEmpty() shouldBe true
      stmt.monthRange shouldBe MAY / 2026 .. MAY / 2026
      stmt.treeNode.path shouldBe listOf("john", "internal")
      stmt.statements.size shouldBe 1
      val subStatement = stmt.statements.first()
      subStatement.monthRange shouldBe MAY / 2026 .. MAY / 2026
      subStatement.treeNode.path shouldBe listOf("john", "internal", "test-account")

      val topStmt = summaryStatements[tree[listOf("john")]]!![MAY / 2026]!!
      topStmt.monthRange shouldBe MAY / 2026 .. MAY / 2026
      topStmt.treeNode.path shouldBe listOf("john")
      topStmt.statements.size shouldBe 1

      val topSubStatement = topStmt.statements.first()
      topSubStatement.monthRange shouldBe MAY / 2026 .. MAY / 2026
      topSubStatement.treeNode.path shouldBe listOf("john", "internal")
    }
  }

  it("multiple propagate up") {
    val tree = root {
      branch("john") {
        branch("internal") {
          leaf("test-account1")
        }
        branch("external") {
          leaf("test-account2")
          leaf("test-account3")
        }
      }
    }

    val aggregator = SummaryStatementBuilder()
    aggregator.addStatement(
      statement = Statement(
        tree[listOf("john", "internal", "test-account1")]!!,
        monthRange = MAY / 2026 .. MAY / 2026,
      )
    )
    aggregator.addStatement(
      statement = Statement(
        tree[listOf("john", "external", "test-account2")]!!,
        monthRange = MAY / 2026 .. MAY / 2026,
      )
    )
    aggregator.addStatement(
      statement = Statement(
        tree[listOf("john", "external", "test-account3")]!!,
        monthRange = MAY / 2026 .. MAY / 2026,
      )
    )
    val summaryStatements = aggregator.build(tree)

    summaryStatements.isEmpty() shouldBe false
    summaryStatements.size shouldBe 3
    summaryStatements.keys.map { it.path }.toSet() shouldBe setOf(
      listOf("john", "internal"), listOf("john", "external"), listOf("john"))
    val stmtInternal = summaryStatements[tree[listOf("john", "internal")]]!![MAY / 2026]!!
    stmtInternal.treeNode.isExternal shouldBe false
    stmtInternal.treeNode.children.isNotEmpty() shouldBe true
    stmtInternal.monthRange shouldBe MAY / 2026 .. MAY / 2026
    stmtInternal.treeNode.path shouldBe listOf("john", "internal")
    stmtInternal.statements.size shouldBe 1
    val subStmtInternal = stmtInternal.statements.first()
    subStmtInternal.monthRange shouldBe MAY / 2026 .. MAY / 2026
    subStmtInternal.treeNode.path shouldBe listOf("john", "internal", "test-account1")

    val stmtExternal = summaryStatements[tree[listOf("john", "external")]]!![MAY / 2026]!!
    stmtExternal.treeNode.isExternal shouldBe true
    stmtExternal.treeNode.children.isNotEmpty() shouldBe true
    stmtExternal.monthRange shouldBe MAY / 2026 .. MAY / 2026
    stmtExternal.treeNode.path shouldBe listOf("john", "external")
    stmtExternal.statements.size shouldBe 2
    val subStmt1External = stmtExternal.statements[0]
    subStmt1External.monthRange shouldBe MAY / 2026 .. MAY / 2026
    subStmt1External.treeNode.path shouldBe listOf("john", "external", "test-account2")
    val subStmt2External = stmtExternal.statements[1]
    subStmt2External.monthRange shouldBe MAY / 2026 .. MAY / 2026
    subStmt2External.treeNode.path shouldBe listOf("john", "external", "test-account3")

    val topStmt = summaryStatements[tree[listOf("john")]]!![MAY / 2026]!!
    topStmt.treeNode.children.isNotEmpty() shouldBe true
    topStmt.treeNode.isExternal shouldBe false
    topStmt.monthRange shouldBe MAY / 2026 .. MAY / 2026
    topStmt.treeNode.path shouldBe listOf("john")
    topStmt.statements.size shouldBe 2

    val topInternalSubStatement = topStmt.statements[0]
    topInternalSubStatement.monthRange shouldBe MAY / 2026 .. MAY / 2026
    topInternalSubStatement.treeNode.path shouldBe listOf("john", "internal")

    val topExternalSubStatement = topStmt.statements[1]
    topExternalSubStatement.monthRange shouldBe MAY / 2026 .. MAY / 2026
    topExternalSubStatement.treeNode.path shouldBe listOf("john", "external")
  }
})
