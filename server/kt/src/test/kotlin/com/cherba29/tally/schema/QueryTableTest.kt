package com.cherba29.tally.schema

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.MonthName.JAN
import com.cherba29.tally.core.MonthName.MAR
import com.cherba29.tally.core.NodeId
import com.cherba29.tally.data.builder.budget
import com.cherba29.tally.data.yaml.toObjectNode
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
      val payload = budget {
        setAccount(
          listOf("john", "internal", "test-account"), Account(
            NodeId("test-account", isSummary = false),
            openedOn = MAR / 2026,
          )
        )
      }
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
      val account = Account(
        NodeId("test-account", isSummary = false, owners = setOf("john")),
        openedOn = MAR / 2026,
      )
      val payload = budget {
        setAccount(listOf("john", "internal", "test-account"), account)
      }
      val exception = shouldThrow<IllegalArgumentException> {
        buildGqlTable(
          payload = payload,
          owner = "john",
          startMonth = MAR / 2026,
          endMonth = MAR / 2026
        )
      }
      exception.message shouldBe "Did not find monthly statements at 'john'"
    }

    it("empty - no open accounts") {
      val accountPath1 = listOf("john", "external", "test-account1")
      val account1 = Account(
        NodeId(name = "test-account1", isSummary = false, path = listOf("external"), owners = setOf("john")),
        openedOn = MAR / 2026,
        closedOn = MAR / 2026
      )
      val accountPath2 = listOf("john", "external", "test-account2")
      val account2 = Account(
        NodeId(name = "test-account2", isSummary = false, path = listOf("external"), owners = setOf("john")),
        openedOn = MAR / 2026,
        closedOn = MAR / 2026
      )
      val payload = budget {
        setAccount(accountPath1, account1)
        setAccount(accountPath2, account2)
        addTransfer(
          fromAccountPath = accountPath1,
          fromMonth = MAR / 2026,
          toAccountName = "test-account2",
          toMonth = MAR / 2026,
          balance = Balance(
            amount = 100,
            date = LocalDate(2026, 3, 1),
            type = Balance.Type.CONFIRMED
          ),
          description = "test transfer"
        )
      }

      val exception = shouldThrow<IllegalArgumentException> {
        buildGqlTable(
          payload = payload,
          owner = "john",
          startMonth = JAN / 2026,
          endMonth = JAN / 2026
        )
      }
      exception.message shouldBe "Bad month range, budget has Mar2026..Mar2026 yet Jan2026..Jan2026 was requested"
    }

    it("single open account without path") {
      val accountPath = listOf("john", "external", "test-account")
      val account = Account(
        NodeId(name = "test-account", isSummary = false, path = listOf("external"), owners = setOf("john")),
        openedOn = JAN / 2026
      )
      val payload = budget {
        setAccount(accountPath, account)
        setBalance(
          accountPath, MAR / 2026, Balance(
            amount = 100,
            date = LocalDate(2026, 3, 1),
            type = Balance.Type.CONFIRMED
          )
        )
      }

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
        NodeId(name = "test-account", isSummary = false, path = listOf("internal"), owners = setOf("john")),
        openedOn = JAN / 2026
      )
      val payload = budget {
        setAccount(accountPath, account)
        setBalance(
          accountPath, MAR / 2026, Balance(
            amount = 100,
            date = LocalDate(2026, 3, 1),
            type = Balance.Type.CONFIRMED
          )
        )
      }

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
