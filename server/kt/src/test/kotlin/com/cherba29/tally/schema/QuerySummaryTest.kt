package com.cherba29.tally.schema

import com.cherba29.tally.core.Month
import com.cherba29.tally.core.MonthName.APR
import com.cherba29.tally.core.MonthName.MAR
import com.cherba29.tally.core.NodeId
import com.cherba29.tally.data.builder.SummaryStatementBuilder
import com.cherba29.tally.statement.SummaryStatement
import com.cherba29.tally.statement.TransactionStatement
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class QuerySummaryTest : DescribeSpec({
  describe("buildSummaryData") {
    it("empty") {
      val exception = shouldThrow<NotFoundException> {
        buildSummaryData(
          summaries = mapOf(),
          owner = "john",
          startMonth = MAR / 2026,
          endMonth = MAR / 2026,
          summaryName = "internal"
        )
      }
      exception.message shouldBe "Summary internal for john not found."
    }

    it("missing months") {
      val summaries = mapOf(listOf("john", "internal") to mapOf<Month, SummaryStatement>())
      val exception = shouldThrow<NotFoundException> {
        buildSummaryData(
          summaries,
          owner = "john",
          startMonth = MAR / 2026,
          endMonth = MAR / 2026,
          summaryName = "internal"
        )
      }
      exception.message shouldBe "Summary internal for john for months [Mar2026, Mar2026] not found."
    }

    it("single") {
      val nodeId = NodeId("summary", owners = setOf("john"), path = listOf("internal"), isSummary = true)
      val summaries = mapOf(
        listOf("john", "internal") to mapOf(
          MAR / 2026 to SummaryStatement(
            nodeId = nodeId,
            monthRange = MAR / 2026..MAR / 2026
          )
        )
      )
      val data = buildSummaryData(
        summaries,
        owner = "john",
        startMonth = MAR / 2026,
        endMonth = MAR / 2026,
        summaryName = "internal"
      )
      data shouldBe GqlSummaryData(
        statements = listOf(),
        total = GqlSummaryStatement(
          name = "summary",
          month = MAR / 2026,
          accounts = listOf(),
          addSub = 0,
          income = 0,
          change = 0,
          inFlows = 0,
          outFlows = 0,
          percentChange = 0f,
          annualizedPercentChange = 0f,
          totalPayments = 0,
          totalTransfers = 0,
          unaccounted = 0,
          endBalance = null,
          startBalance = null
        )
      )
    }

    it("single with transaction statement") {
      val nodeId = NodeId("summary", owners = setOf("john"), path = listOf("internal"), isSummary = true)
      val summaryStatement = SummaryStatementBuilder.builder {
        this.nodeId = nodeId
        monthRange = MAR / 2026..MAR / 2026
        addStatement(
          TransactionStatement(
            nodeId = NodeId("test-account", owners = setOf("john"), path = listOf("internal"), isSummary = true),
            monthRange = MAR / 2026..MAR / 2026,
            isClosed = false,
            startBalance = null
          )
        )
      }
      val summaries = mapOf(listOf("john", "internal") to mapOf(MAR / 2026 to summaryStatement))
      val data = buildSummaryData(
        summaries,
        owner = "john",
        startMonth = MAR / 2026,
        endMonth = MAR / 2026,
        summaryName = "internal"
      )
      data shouldBe GqlSummaryData(
        statements = listOf(
          GqlStatement(
            name = "test-account",
            month = MAR / 2026,
            isClosed = false,
            isCovered = true,
            isProjectedCovered = true,
            hasProjectedTransfer = false,
            startBalance = null,
            endBalance = null,
            inFlows = 0,
            outFlows = 0,
            income = 0,
            totalPayments = 0,
            totalTransfers = 0,
            change = 0,
            addSub = 0,
            percentChange = 0f,
            annualizedPercentChange = 0f,
            unaccounted = 0,
            transactions = listOf()
          )
        ),
        total = GqlSummaryStatement(
          name = "summary",
          month = MAR / 2026,
          accounts = listOf("test-account"),
          addSub = 0,
          income = 0,
          change = 0,
          inFlows = 0,
          outFlows = 0,
          percentChange = 0f,
          annualizedPercentChange = 0f,
          totalPayments = 0,
          totalTransfers = 0,
          unaccounted = 0,
          endBalance = null,
          startBalance = null
        )
      )
    }

    it("single with multiple transaction statement") {
      val nodeId = NodeId("summary", owners = setOf("john"), path = listOf("internal"), isSummary = true)
      val summaryStatement = SummaryStatementBuilder.builder {
        this.nodeId = nodeId
        monthRange = MAR / 2026..MAR / 2026
        addStatement(
          TransactionStatement(
            nodeId = NodeId("test-account1", owners = setOf("john"), path = listOf("internal"), isSummary = true),
            monthRange = MAR / 2026..MAR / 2026,
            isClosed = false,
            startBalance = null
          )
        )
        addStatement(
          SummaryStatement(
            nodeId = NodeId("test-account2", owners = setOf("john"), path = listOf("internal"), isSummary = true),
            monthRange = MAR / 2026..MAR / 2026,
          )
        )
      }
      val summaries = mapOf(listOf("john", "internal") to mapOf(MAR / 2026 to summaryStatement))
      val data = buildSummaryData(
        summaries,
        owner = "john",
        startMonth = MAR / 2026,
        endMonth = MAR / 2026,
        summaryName = "internal"
      )
      data shouldBe GqlSummaryData(
        statements = listOf(
          GqlStatement(
            name = "test-account1",
            month = MAR / 2026,
            isClosed = false,
            isCovered = true,
            isProjectedCovered = true,
            hasProjectedTransfer = false,
            startBalance = null,
            endBalance = null,
            inFlows = 0,
            outFlows = 0,
            income = 0,
            totalPayments = 0,
            totalTransfers = 0,
            change = 0,
            addSub = 0,
            percentChange = 0f,
            annualizedPercentChange = 0f,
            unaccounted = 0,
            transactions = listOf()
          ),
          GqlStatement(
            name = "test-account2",
            month = MAR / 2026,
            isClosed = false,
            isCovered = true,
            isProjectedCovered = true,
            hasProjectedTransfer = false,
            startBalance = null,
            endBalance = null,
            inFlows = 0,
            outFlows = 0,
            income = 0,
            totalPayments = 0,
            totalTransfers = 0,
            change = 0,
            addSub = 0,
            percentChange = 0f,
            annualizedPercentChange = 0f,
            unaccounted = 0,
            transactions = listOf()
          )
        ),
        total = GqlSummaryStatement(
          name = "summary",
          month = MAR / 2026,
          accounts = listOf("test-account1", "test-account2"),
          addSub = 0,
          income = 0,
          change = 0,
          inFlows = 0,
          outFlows = 0,
          percentChange = 0f,
          annualizedPercentChange = 0f,
          totalPayments = 0,
          totalTransfers = 0,
          unaccounted = 0,
          endBalance = null,
          startBalance = null
        )
      )
    }

    it("multiple months") {
      val nodeId = NodeId("summary", owners = setOf("john"), path = listOf("internal"), isSummary = true)
      val summaries = mapOf(
        listOf("john", "internal") to mapOf(
          MAR / 2026 to SummaryStatement(
            nodeId = nodeId,
            monthRange = MAR / 2026..MAR / 2026
          ),
          APR / 2026 to SummaryStatement(
            nodeId = nodeId,
            monthRange = APR / 2026..APR / 2026
          )
        )
      )
      val data = buildSummaryData(
        summaries,
        owner = "john",
        startMonth = MAR / 2026,
        endMonth = APR / 2026,
        summaryName = "internal"
      )
      data shouldBe GqlSummaryData(
        statements = listOf(),
        total = GqlSummaryStatement(
          name = "summary",
          month = MAR / 2026,
          accounts = listOf(),
          addSub = 0,
          income = 0,
          change = 0,
          inFlows = 0,
          outFlows = 0,
          percentChange = 0f,
          annualizedPercentChange = 0f,
          totalPayments = 0,
          totalTransfers = 0,
          unaccounted = 0,
          endBalance = null,
          startBalance = null
        )
      )
    }

    it("multiple months null start month") {
      val nodeId = NodeId("summary", owners = setOf("john"), path = listOf("internal"), isSummary = true)
      val summaries = mapOf(
        listOf("john", "internal") to mapOf(
          MAR / 2026 to SummaryStatement(
            nodeId = nodeId,
            monthRange = MAR / 2026..MAR / 2026
          ),
          APR / 2026 to SummaryStatement(
            nodeId = nodeId,
            monthRange = APR / 2026..APR / 2026
          )
        )
      )
      val data = buildSummaryData(
        summaries,
        owner = "john",
        startMonth = null,
        endMonth = MAR / 2026,
        summaryName = "internal"
      )
      data shouldBe GqlSummaryData(
        statements = listOf(),
        total = GqlSummaryStatement(
          name = "summary",
          month = MAR / 2026,
          accounts = listOf(),
          addSub = 0,
          income = 0,
          change = 0,
          inFlows = 0,
          outFlows = 0,
          percentChange = 0f,
          annualizedPercentChange = 0f,
          totalPayments = 0,
          totalTransfers = 0,
          unaccounted = 0,
          endBalance = null,
          startBalance = null
        )
      )
    }

  }
})