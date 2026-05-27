package com.cherba29.tally.data.builder

import com.cherba29.tally.core.MonthName.MAY
import com.cherba29.tally.core.NodeId
import com.cherba29.tally.statement.Statement
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe


class SummaryStatementBuilderTest : DescribeSpec({
  describe("Creation") {
    it("basic") {
      val aggregator = SummaryStatementBuilder()
      aggregator.summaryStatements.isEmpty() shouldBe true
    }
  }
  describe("statements") {
    it("add single") {
      val aggregator = SummaryStatementBuilder()
      aggregator.addStatement(
        owner = "john",
        statement = Statement(
          nodeId = NodeId("test-account", isSummary = false, setOf("john"), listOf("internal")),
          monthRange = MAY / 2026..MAY / 2026,
        )
      )
      aggregator.summaryStatements.isEmpty() shouldBe false
      aggregator.summaryStatements.size shouldBe 1
      val stmt = aggregator.summaryStatements[listOf("john", "internal")]!![MAY/2026]!!
      stmt.nodeId.name shouldBe "internal"
      stmt.nodeId.isExternal shouldBe false
      stmt.nodeId.isSummary shouldBe true
      stmt.monthRange shouldBe MAY / 2026 .. MAY / 2026
      stmt.nodeId.owners shouldBe listOf("john")
      stmt.statements.size shouldBe 1
      val subStatement = stmt.statements.first()
      subStatement.nodeId.name shouldBe "test-account"
      subStatement.monthRange shouldBe MAY / 2026 .. MAY / 2026
      subStatement.nodeId.owners shouldBe listOf("john")
    }

    it("add single and propagate up") {
      val aggregator = SummaryStatementBuilder()
      aggregator.addStatement(
        owner = "john",
        statement = Statement(
          nodeId = NodeId("/test-account", isSummary = true, path = listOf("internal")),
          monthRange = MAY / 2026 .. MAY / 2026,
        )
      )
      aggregator.propagateUpThePath2()

      aggregator.summaryStatements.isEmpty() shouldBe false
      aggregator.summaryStatements.size shouldBe 2
      aggregator.summaryStatements.keys shouldBe setOf(listOf("john", "internal"), listOf("john", ""))
      val stmt = aggregator.summaryStatements[listOf("john", "internal")]!![MAY / 2026]!!
      stmt.nodeId.name shouldBe "internal"
      stmt.nodeId.isExternal shouldBe false
      stmt.nodeId.isSummary shouldBe true
      stmt.monthRange shouldBe MAY / 2026 .. MAY / 2026
      stmt.nodeId.owners shouldBe listOf("john")
      stmt.statements.size shouldBe 1
      val subStatement = stmt.statements.first()
      subStatement.nodeId.name shouldBe "/test-account"
      subStatement.monthRange shouldBe MAY / 2026 .. MAY / 2026
      subStatement.nodeId.owners shouldBe listOf()

      val topStmt = aggregator.summaryStatements[listOf("john", "")]!![MAY / 2026]!!
      topStmt.nodeId.name shouldBe ""
      topStmt.monthRange shouldBe MAY / 2026 .. MAY / 2026
      topStmt.nodeId.owners shouldBe listOf("john")
      topStmt.statements.size shouldBe 1

      val topSubStatement = topStmt.statements.first()
      topSubStatement.nodeId.name shouldBe "internal"
      topSubStatement.monthRange shouldBe MAY / 2026 .. MAY / 2026
      topSubStatement.nodeId.owners shouldBe listOf("john")
    }
  }

  it("multiple propagate up") {
    val aggregator = SummaryStatementBuilder()
    aggregator.addStatement(
      owner = "john",
      statement = Statement(
        nodeId = NodeId("test-account1", isSummary = false, path = listOf("internal")),
        monthRange = MAY / 2026 .. MAY / 2026,
      )
    )
    aggregator.addStatement(
      owner = "john",
      statement = Statement(
        nodeId = NodeId("test-account2", isSummary = false, path = listOf("external")),
        monthRange = MAY / 2026 .. MAY / 2026,
      )
    )
    aggregator.addStatement(
      owner = "john",
      statement = Statement(
        nodeId = NodeId("test-account3", isSummary = false, path = listOf("external")),
        monthRange = MAY / 2026 .. MAY / 2026,
      )
    )
    aggregator.propagateUpThePath2()

    aggregator.summaryStatements.isEmpty() shouldBe false
    aggregator.summaryStatements.size shouldBe 3
    aggregator.summaryStatements.keys shouldBe setOf(
      listOf("john", "internal"), listOf("john", "external"), listOf("john", ""))
    val stmtInternal = aggregator.summaryStatements[listOf("john", "internal")]!![MAY / 2026]!!
    stmtInternal.nodeId.name shouldBe "internal"
    stmtInternal.nodeId.isExternal shouldBe false
    stmtInternal.nodeId.isSummary shouldBe true
    stmtInternal.monthRange shouldBe MAY / 2026 .. MAY / 2026
    stmtInternal.nodeId.owners shouldBe listOf("john")
    stmtInternal.statements.size shouldBe 1
    val subStmtInternal = stmtInternal.statements.first()
    subStmtInternal.nodeId.name shouldBe "test-account1"
    subStmtInternal.monthRange shouldBe MAY / 2026 .. MAY / 2026
    subStmtInternal.nodeId.owners shouldBe listOf()

    val stmtExternal = aggregator.summaryStatements[listOf("john", "external")]!![MAY / 2026]!!
    stmtExternal.nodeId.name shouldBe "external"
    stmtExternal.nodeId.isExternal shouldBe true
    stmtExternal.nodeId.isSummary shouldBe true
    stmtExternal.monthRange shouldBe MAY / 2026 .. MAY / 2026
    stmtExternal.nodeId.owners shouldBe listOf("john")
    stmtExternal.statements.size shouldBe 2
    val subStmt1External = stmtExternal.statements[0]
    subStmt1External.nodeId.name shouldBe "test-account2"
    subStmt1External.monthRange shouldBe MAY / 2026 .. MAY / 2026
    subStmt1External.nodeId.owners shouldBe listOf()
    val subStmt2External = stmtExternal.statements[1]
    subStmt2External.nodeId.name shouldBe "test-account3"
    subStmt2External.monthRange shouldBe MAY / 2026 .. MAY / 2026
    subStmt2External.nodeId.owners shouldBe listOf()

    val topStmt = aggregator.summaryStatements[listOf("john", "")]!![MAY / 2026]!!
    topStmt.nodeId.isSummary shouldBe true
    topStmt.nodeId.isExternal shouldBe false
    topStmt.nodeId.name shouldBe ""
    topStmt.monthRange shouldBe MAY / 2026 .. MAY / 2026
    topStmt.nodeId.owners shouldBe listOf("john")
    topStmt.statements.size shouldBe 2

    val topInternalSubStatement = topStmt.statements[0]
    topInternalSubStatement.nodeId.name shouldBe "external"
    topInternalSubStatement.monthRange shouldBe MAY / 2026 .. MAY / 2026
    topInternalSubStatement.nodeId.owners shouldBe listOf("john")

    val topExternalSubStatement = topStmt.statements[1]
    topExternalSubStatement.nodeId.name shouldBe "internal"
    topExternalSubStatement.monthRange shouldBe MAY / 2026 .. MAY / 2026
    topExternalSubStatement.nodeId.owners shouldBe listOf("john")
  }
})
