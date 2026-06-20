package com.cherba29.tally

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.MonthName.APR
import com.cherba29.tally.core.MonthName.MAR
import com.cherba29.tally.core.NodeId
import com.cherba29.tally.core.root
import com.cherba29.tally.data.Budget
import com.cherba29.tally.data.Loader
import com.cherba29.tally.data.builder.budget
import com.cherba29.tally.data.yaml.toObjectNode
import com.cherba29.tally.schema.GqlSummaryData
import com.diffplug.selfie.coroutines.expectSelfie
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk

class SummaryServiceTest : DescribeSpec({
  fun GqlSummaryData.toSnapshot(): String {
    val mapper = YAMLMapper.builder()
      .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
      .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
      .build()
    val arrayNode = mapper.createArrayNode()
    toObjectNode(arrayNode.addObject())
    return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(arrayNode)
  }

  describe("buildSummaryData") {
    it("empty") {
      val nodeId = NodeId("test-account1", owners = setOf("john"), path = listOf("internal"), isSummary = true)
      val account = Account(nodeId, openedOn = MAR / 2026)
      val loader = mockk<Loader> {
        coEvery { budget() } returns budget {
          setAccount(listOf("john", "internal", "test-account1"), account)
        }
      }

      val exception = shouldThrow<NotFoundException> {
        SummaryService(loader).summary(
          owner = "john",
          startMonth = MAR / 2026,
          endMonth = MAR / 2026,
          accountType = "internal"
        )
      }
      exception.message shouldBe "Summary internal for john for months [Mar2026, Mar2026] not found."
    }

    it("missing months") {
      val loader = mockk<Loader> {
        coEvery { budget() } returns Budget(
          months = MAR / 2026..MAR / 2026,
          tree = root {},
          leafToAccount = mapOf(),
          accounts = mapOf(),
          nodeToStatement = mapOf(),
          statements = mapOf()
        )
      }

      val exception = shouldThrow<NotFoundException> {
        SummaryService(loader).summary(
          owner = "john",
          startMonth = MAR / 2026,
          endMonth = MAR / 2026,
          accountType = "internal"
        )
      }
      exception.message shouldBe "Summary internal for john for months [Mar2026, Mar2026] not found."
    }
    
    it("single") {
      val nodeId = NodeId("test-account1", owners = setOf("john"), path = listOf("internal"), isSummary = true)
      val account = Account(nodeId, openedOn = MAR / 2026)
      val loader = mockk<Loader> {
        coEvery { budget() } returns budget {
          setAccount(listOf("john", "internal", "test-account1"), account)
          setBalance(
            listOf("john", "internal", "test-account1"), MAR / 2026,
            Balance.confirmed(100, "2026-03-01")
          )
        }
      }
      val data = SummaryService(loader).summary(
        owner = "john",
        startMonth = MAR / 2026,
        endMonth = MAR / 2026,
        accountType = "internal"
      )
      expectSelfie(data.toSnapshot()).toMatchDisk()
    }

    it("with transaction statement") {
      val nodeId1 = NodeId("test-account1", owners = setOf("john"), path = listOf("internal"), isSummary = false)
      val account1 = Account(nodeId1, openedOn = MAR / 2026)
      val nodeId2 = NodeId("test-account2", owners = setOf("john"), path = listOf("internal"), isSummary = false)
      val account2 = Account(nodeId2, openedOn = MAR / 2026)
      val loader = mockk<Loader> {
        coEvery { budget() } returns budget {
          setAccount(listOf("john", "internal", "test-account1"), account1)
          setAccount(listOf("john", "internal", "test-account2"), account2)
          setBalance(listOf("john", "internal", "test-account1"), MAR / 2026, Balance.confirmed(100, "2026-03-01"))
          setBalance(listOf("john", "internal", "test-account2"), MAR / 2026, Balance.confirmed(200, "2026-03-01"))
          addTransfer(
            fromAccountPath = listOf("john", "internal", "test-account1"),
            fromMonth = MAR / 2026,
            toAccountName = "test-account2",
            toMonth = MAR / 2026,
            balance = Balance.confirmed(50, "2026-03-02"),
            description = "transfer from 1 to 2"
          )
        }
      }

      val data = SummaryService(loader).summary(
        owner = "john",
        startMonth = MAR / 2026,
        endMonth = MAR / 2026,
        accountType = "internal"
      )
      expectSelfie(data.toSnapshot()).toMatchDisk()
    }

    it("single with multiple transaction statement") {
      val nodeId1 = NodeId("test-account1", owners = setOf("john"), path = listOf("internal"), isSummary = false)
      val account1 = Account(nodeId1, openedOn = MAR / 2026)
      val nodeId2 = NodeId("test-account2", owners = setOf("john"), path = listOf("external"), isSummary = false)
      val account2 = Account(nodeId2, openedOn = MAR / 2026)

      val loader = mockk<Loader> {
        coEvery { budget() } returns budget {
          setAccount(listOf("john", "internal", "test-account1"), account1)
          setAccount(listOf("john", "external", "test-account2"), account2)
          setBalance(listOf("john", "internal", "test-account1"), MAR / 2026, Balance.confirmed(100, "2026-03-01"))
          setBalance(listOf("john", "external", "test-account2"), MAR / 2026, Balance.confirmed(200, "2026-03-01"))
          addTransfer(
            fromAccountPath = listOf("john", "internal", "test-account1"),
            fromMonth = MAR / 2026,
            toAccountName = "test-account2",
            toMonth = MAR / 2026,
            balance = Balance.confirmed(50, "2026-04-02"),
            description = "transfer from 1 to 2"
          )
        }
      }
      val data = SummaryService(loader).summary(
        owner = "john",
        startMonth = MAR / 2026,
        endMonth = MAR / 2026,
        accountType = "internal"
      )
      expectSelfie(data.toSnapshot()).toMatchDisk()
    }

    it("multiple months") {
      val nodeId1 = NodeId("test-account1", owners = setOf("john"), path = listOf("internal"), isSummary = false)
      val account1 = Account(nodeId1, openedOn = MAR / 2026)
      val nodeId2 = NodeId("test-account2", owners = setOf("john"), path = listOf("external"), isSummary = false)
      val account2 = Account(nodeId2, openedOn = MAR / 2026)

      val loader = mockk<Loader> {
        coEvery { budget() } returns budget {
          setAccount(listOf("john", "internal", "test-account1"), account1)
          setAccount(listOf("john", "external", "test-account2"), account2)
          setBalance(listOf("john", "internal", "test-account1"), MAR / 2026, Balance.confirmed(100, "2026-03-01"))
          setBalance(listOf("john", "external", "test-account2"), MAR / 2026, Balance.confirmed(200, "2026-03-01"))
          setBalance(listOf("john", "internal", "test-account1"), APR / 2026, Balance.confirmed(150, "2026-04-01"))
          setBalance(listOf("john", "external", "test-account2"), APR / 2026, Balance.confirmed(250, "2026-04-01"))
          addTransfer(
            fromAccountPath = listOf("john", "internal", "test-account1"),
            fromMonth = MAR / 2026,
            toAccountName = "test-account2",
            toMonth = MAR / 2026,
            balance = Balance.confirmed(50, "2026-03-02"),
            description = "transfer from 1 to 2"
          )
          addTransfer(
            fromAccountPath = listOf("john", "external", "test-account2"),
            fromMonth = APR / 2026,
            toAccountName = "test-account1",
            toMonth = APR / 2026,
            balance = Balance.confirmed(75, "2026-04-02"),
            description = "transfer from 2 to 1"
          )
        }
      }

      val data = SummaryService(loader).summary(
        owner = "john",
        startMonth = MAR / 2026,
        endMonth = APR / 2026,
        accountType = "internal"
      )
      expectSelfie(data.toSnapshot()).toMatchDisk()
    }

    it("multiple months null start month") {
      val nodeId1 = NodeId("test-account1", owners = setOf("john"), path = listOf("internal"), isSummary = false)
      val account1 = Account(nodeId1, openedOn = MAR / 2026)
      val nodeId2 = NodeId("test-account2", owners = setOf("john"), path = listOf("external"), isSummary = false)
      val account2 = Account(nodeId2, openedOn = MAR / 2026)

      val loader = mockk<Loader> {
        coEvery { budget() } returns budget {
          setAccount(listOf("john", "internal", "test-account1"), account1)
          setAccount(listOf("john", "external", "test-account2"), account2)
          setBalance(listOf("john", "internal", "test-account1"), MAR / 2026, Balance.confirmed(100, "2026-03-01"))
          setBalance(listOf("john", "external", "test-account2"), MAR / 2026, Balance.confirmed(200, "2026-03-01"))
          setBalance(listOf("john", "internal", "test-account1"), APR / 2026, Balance.confirmed(150, "2026-04-01"))
          setBalance(listOf("john", "external", "test-account2"), APR / 2026, Balance.confirmed(250, "2026-04-01"))
          addTransfer(
            fromAccountPath = listOf("john", "internal", "test-account1"),
            fromMonth = MAR / 2026,
            toAccountName = "test-account2",
            toMonth = MAR / 2026,
            balance = Balance.confirmed(50, "2026-03-02"),
            description = "transfer from 1 to 2"
          )
          addTransfer(
            fromAccountPath = listOf("john", "external", "test-account2"),
            fromMonth = APR / 2026,
            toAccountName = "test-account1",
            toMonth = APR / 2026,
            balance = Balance.confirmed(75, "2026-04-02"),
            description = "transfer from 2 to 1"
          )
        }
      }
      val data = SummaryService(loader).summary(
        owner = "john",
        startMonth = null,
        endMonth = MAR / 2026,
        accountType = "internal"
      )
      expectSelfie(data.toSnapshot()).toMatchDisk()
    }
  }
})