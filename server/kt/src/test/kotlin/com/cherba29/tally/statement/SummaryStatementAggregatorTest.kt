package com.cherba29.tally.statement

import com.cherba29.tally.core.MonthName.MAY
import com.cherba29.tally.core.NodeId
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe


class SummaryStatementAggregatorTest : DescribeSpec({
  describe("Creation") {
    it("basic") {
      val aggregator = SummaryStatementAggregator()
      aggregator.summaryStatements.isEmpty shouldBe true
    }
  }
  describe("statements") {
    it("add single") {
      val aggregator = SummaryStatementAggregator()
      aggregator.addStatement(
        summaryName = "",
        owner = "john",
        statement = Statement(
          nodeId = NodeId("test-account"),
          monthRange = MAY / 2026 .. MAY / 2026,
        )
      )
      aggregator.summaryStatements.isEmpty shouldBe false
      aggregator.summaryStatements.size shouldBe 1
      val stmt = aggregator.summaryStatements["john", "", "May2026"]!!
      stmt.nodeId.name shouldBe ""
      stmt.monthRange shouldBe MAY / 2026 .. MAY / 2026
      stmt.nodeId.owners shouldBe listOf("john")
      stmt.statements.size shouldBe 1
      val subStatement = stmt.statements.first()
      subStatement.nodeId.name shouldBe "test-account"
      subStatement.monthRange shouldBe MAY / 2026 .. MAY / 2026
      subStatement.nodeId.owners shouldBe listOf()
    }

    it("add single and propagate up") {
      val aggregator = SummaryStatementAggregator()
      aggregator.addStatement(
        summaryName = "/internal",
        owner = "john",
        statement = Statement(
          nodeId = NodeId("/test-account", path = listOf("internal")),
          monthRange = MAY / 2026 .. MAY / 2026,
        )
      )
      aggregator.propagateUpThePath2()

      aggregator.summaryStatements.isEmpty shouldBe false
      aggregator.summaryStatements.size shouldBe 2
      val stmt = aggregator.summaryStatements["john", "/internal", "May2026"]!!
      stmt.nodeId.name shouldBe "/internal"
      stmt.monthRange shouldBe MAY / 2026 .. MAY / 2026
      stmt.nodeId.owners shouldBe listOf("john")
      stmt.statements.size shouldBe 1
      val subStatement = stmt.statements.first()
      subStatement.nodeId.name shouldBe "/test-account"
      subStatement.monthRange shouldBe MAY / 2026 .. MAY / 2026
      subStatement.nodeId.owners shouldBe listOf()

      val topStmt = aggregator.summaryStatements["john", "/", "May2026"]!!
      topStmt.nodeId.name shouldBe "/"
      topStmt.monthRange shouldBe MAY / 2026 .. MAY / 2026
      topStmt.nodeId.owners shouldBe listOf("john")
      topStmt.statements.size shouldBe 1

      val topSubStatement = topStmt.statements.first()
      topSubStatement.nodeId.name shouldBe "/internal"
      topSubStatement.monthRange shouldBe MAY / 2026 .. MAY / 2026
      topSubStatement.nodeId.owners shouldBe listOf("john")
    }
  }
})