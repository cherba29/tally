package com.cherba29.tally.schema

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.MonthName.JAN
import com.cherba29.tally.core.MonthName.MAR
import com.cherba29.tally.core.NodeId
import com.cherba29.tally.data.budget
import com.cherba29.tally.data.yaml.toObjectNode
import com.cherba29.tally.statement.SummaryStatement
import com.cherba29.tally.statement.TransactionStatement
import com.cherba29.tally.utils.Map3
import com.diffplug.selfie.coroutines.expectSelfie
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate


class QueryTableTest : DescribeSpec({
  fun GqlTable.toSnapshot(): String {
    val mapper = YAMLMapper.builder()
      .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
      .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
      .build()
    val arrayNode = mapper.createArrayNode()
    toObjectNode(arrayNode.addObject())
    return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(arrayNode)
  }

  describe("buildGqlTable") {
    it("no owner") {
      val payload = budget {}
      val exception = shouldThrow<IllegalArgumentException> {
        buildGqlTable(
          payload = payload,
          owner = null,
          startMonth = MAR / 2026,
          endMonth = MAR / 2026
        )
      }
      exception.message shouldBe "No owner is specified and one cannot be derived from accounts"
    }

    it("empty") {
      val payload = budget {}
      val exception = shouldThrow<IllegalArgumentException> {
        buildGqlTable(
          payload = payload,
          owner = "john",
          startMonth = MAR / 2026,
          endMonth = MAR / 2026
        )
      }
      exception.message shouldBe "Did not find summary statement at '/' " +
          "for owner 'john' in payload summaries"
    }

    it("empty - no open accounts") {
      val account = Account(
        NodeId(name = "test-account", path = listOf("external"), owners = setOf("john")),
        openedOn = MAR / 2026,
      )
      val summary = SummaryStatement(
        account.nodeId,
        MAR / 2026 .. MAR / 2026
      )
      val summaries = Map3<SummaryStatement>()
      summaries.set("john", "/", "Mar2026", summary)
      val payload = budget {}
      payload.summaries = summaries

      val table = buildGqlTable(
        payload = payload,
        owner = "john",
        startMonth = MAR / 2026,
        endMonth = MAR / 2026
      )
      table.months shouldBe listOf()
      table.owners shouldBe listOf()
      table.currentOwner shouldBe "john"
      table.rows shouldBe listOf()
    }

    it("single open account without path") {
      val accountPath = listOf("john", "external", "test-account")
      val account = Account(
        NodeId(name = "test-account", path = listOf("external"), owners = setOf("john")),
        openedOn = JAN / 2026
      )
      val summary = SummaryStatement(
        account.nodeId,
        MAR / 2026 .. MAR / 2026
      )
      val summaries = Map3<SummaryStatement>()
      summaries.set("john", "/", "Mar2026", summary)
      summaries.set("john", "/external", "Mar2026", summary)
      val payload = budget {
          setAccount(accountPath, account)
          setBalance(
            accountPath, MAR / 2026, Balance(
              amount = 100,
              date = LocalDate(2026, 3, 1),
              type = Balance.Type.CONFIRMED
          ))
        }
      payload.statements = mapOf(account.nodeId to mapOf(MAR / 2026 to TransactionStatement(
          account.nodeId,
          MAR / 2026 .. MAR / 2026,
          isClosed = false,
          startBalance = Balance(100, LocalDate(2026, 3, 1), Balance.Type.CONFIRMED)
        )))
      payload.summaries = summaries

      val table = buildGqlTable(
        payload = payload,
        owner = "john",
        startMonth = MAR / 2026,
        endMonth = MAR / 2026
      )
      expectSelfie(table.toSnapshot()).toMatchDisk()
    }

    it("single open account with path") {
      val accountPath = listOf("john", "internal", "test-account")
      val account = Account(
        NodeId(name = "test-account", path = listOf("internal"), owners = setOf("john")),
        openedOn = JAN / 2026
      )
      val summary = SummaryStatement(
        account.nodeId,
        MAR / 2026 .. MAR / 2026
      )
      val transactionStatement = TransactionStatement(
        account.nodeId,
        MAR / 2026 .. MAR / 2026,
        isClosed = false,
        startBalance = Balance(
          amount = 100,
          date = LocalDate(2026, 3, 1),
          type = Balance.Type.CONFIRMED
        )
      )
      val summaries = Map3<SummaryStatement>()
      summaries.set("john", "/internal", "Mar2026", summary)
      summaries.set("john", "/", "Mar2026", summary)
      val payload =  budget {
          setAccount(accountPath, account)
          setBalance(
            accountPath, MAR / 2026, Balance(
              amount = 100,
              date = LocalDate(2026, 3, 1),
              type = Balance.Type.CONFIRMED
            ))
        }
      payload.statements = mapOf(account.nodeId to mapOf(MAR / 2026 to transactionStatement))
      payload.summaries = summaries

      val table = buildGqlTable(
        payload = payload,
        owner = "john",
        startMonth = MAR / 2026,
        endMonth = MAR / 2026
      )
      expectSelfie(table.toSnapshot()).toMatchDisk()
    }
  }
})