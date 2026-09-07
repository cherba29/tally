package com.cherba29.tally.data.builder

import com.cherba29.tally.core.MonthName.MAY
import com.cherba29.tally.utils.root
import com.cherba29.tally.data.Profile
import com.cherba29.tally.statement.SummaryStatement
import com.cherba29.tally.statement.TransactionStatement
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class SummaryMapBuilderTest : DescribeSpec({
  describe("Creation") {
    it("basic") {
      val builder = SummaryMapBuilder()
      builder.build(root(Profile()) {}).isEmpty() shouldBe true
    }
  }
  describe("statements") {
    it("add single") {
      val tree = root(Profile()) {
        branch("john", Profile()) {
          branch("internal", Profile()) {
            leaf("test-account", Profile())
          }
        }
      }
      val node = tree[listOf("john", "internal", "test-account")]!!
      val builder = SummaryMapBuilder()
      builder.addStatement(
        node,
        MAY / 2026,
        statement = TransactionStatement(
          monthRange = MAY / 2026..MAY / 2026,
          startBalance = null
        )
      )
      val summaryStatements = builder.build(tree)
      summaryStatements.isEmpty() shouldBe false
      summaryStatements.keys.map { it.path }.toSet() shouldBe setOf(listOf("john", "internal"), listOf("john"))
      val treeNode = tree[listOf("john", "internal")]!!
      val stmt = summaryStatements[treeNode]!![MAY / 2026]!!
      stmt.monthRange shouldBe MAY / 2026..MAY / 2026
      stmt.statements.size shouldBe 1
      val subStatement = stmt.statements[node] as TransactionStatement
      subStatement.monthRange shouldBe MAY / 2026..MAY / 2026
    }

    it("add single and propagate up") {
      val tree = root(Profile()) {
        branch("john", Profile()) {
          branch("internal", Profile()) {
            leaf("test-account", Profile())
          }
        }
      }

      val node = tree[listOf("john", "internal", "test-account")]!!
      val aggregator = SummaryMapBuilder()
      aggregator.addStatement(
        node,
        MAY / 2026,
        statement = TransactionStatement(
          monthRange = MAY / 2026..MAY / 2026,
          startBalance = null
        )
      )
      val summaryStatements = aggregator.build(tree)

      summaryStatements.isEmpty() shouldBe false
      summaryStatements.size shouldBe 2
      summaryStatements.keys.map { it.path }.toSet() shouldBe setOf(listOf("john", "internal"), listOf("john"))
      val stmt = summaryStatements[tree[listOf("john", "internal")]]!![MAY / 2026]!!
      stmt.monthRange shouldBe MAY / 2026..MAY / 2026
      stmt.statements.size shouldBe 1
      val subStatement = stmt.statements[node] as TransactionStatement
      subStatement.monthRange shouldBe MAY / 2026..MAY / 2026

      val topStmt = summaryStatements[tree[listOf("john")]]!![MAY / 2026]!!
      topStmt.monthRange shouldBe MAY / 2026..MAY / 2026
      topStmt.statements.keys shouldBe setOf(tree[listOf("john", "internal")])

      val topSubStatement = topStmt.statements[tree[listOf("john", "internal")]] as SummaryStatement
      topSubStatement.monthRange shouldBe MAY / 2026..MAY / 2026
    }
  }

  it("multiple propagate up") {
    val tree = root(Profile()) {
      branch("john", Profile()) {
        branch("internal", Profile()) {
          leaf("test-account1", Profile())
        }
        branch("external", Profile(isExternal = true)) {
          leaf("test-account2", Profile(isExternal = true))
          leaf("test-account3", Profile(isExternal = true))
        }
      }
    }

    val node1 = tree[listOf("john", "internal", "test-account1")]!!
    val node2 = tree[listOf("john", "external", "test-account2")]!!
    val node3 = tree[listOf("john", "external", "test-account3")]!!
    val aggregator = SummaryMapBuilder()
    aggregator.addStatement(
      node1,
      MAY / 2026,
      statement = TransactionStatement(
        monthRange = MAY / 2026..MAY / 2026,
        startBalance = null
      )
    )
    aggregator.addStatement(
      node2,
      MAY / 2026,
      statement = TransactionStatement(
        monthRange = MAY / 2026..MAY / 2026,
        startBalance = null
      )
    )
    aggregator.addStatement(
      node3,
      MAY / 2026,
      statement = TransactionStatement(
        monthRange = MAY / 2026..MAY / 2026,
        startBalance = null
      )
    )
    val summaryStatements = aggregator.build(tree)

    summaryStatements.isEmpty() shouldBe false
    summaryStatements.size shouldBe 3
    summaryStatements.keys.map { it.path }.toSet() shouldBe setOf(
      listOf("john", "internal"),
      listOf("john", "external"),
      listOf("john")
    )
    val stmtInternal = summaryStatements[tree[listOf("john", "internal")]]!!
    val stmtInternalMay = stmtInternal[MAY / 2026]!!
    stmtInternalMay.monthRange shouldBe MAY / 2026..MAY / 2026
    stmtInternalMay.statements.keys shouldBe setOf(node1)
    val subStmtInternalMay = stmtInternalMay.statements[node1] as TransactionStatement
    subStmtInternalMay.monthRange shouldBe MAY / 2026..MAY / 2026

    val stmtExternal = summaryStatements[tree[listOf("john", "external")]]!![MAY / 2026]!!
    stmtExternal.monthRange shouldBe MAY / 2026..MAY / 2026
    stmtExternal.statements.size shouldBe 2
    val subStmt1External = stmtExternal.statements[node2] as TransactionStatement
    subStmt1External.monthRange shouldBe MAY / 2026..MAY / 2026
    val subStmt2External = stmtExternal.statements[node3] as TransactionStatement
    subStmt2External.monthRange shouldBe MAY / 2026..MAY / 2026

    val topStmt = summaryStatements[tree[listOf("john")]]!![MAY / 2026]!!
    topStmt.monthRange shouldBe MAY / 2026..MAY / 2026
    topStmt.statements.size shouldBe 2

    val topInternalSubStatement = topStmt.statements[tree[listOf("john", "internal")]] as SummaryStatement
    topInternalSubStatement.monthRange shouldBe MAY / 2026..MAY / 2026

    val topExternalSubStatement = topStmt.statements[tree[listOf("john", "external")]] as SummaryStatement
    topExternalSubStatement.monthRange shouldBe MAY / 2026..MAY / 2026
  }
})
