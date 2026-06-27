package com.cherba29.tally.data

import com.cherba29.tally.core.MonthName.JUL
import com.cherba29.tally.core.MonthName.JUN
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.lang.IllegalArgumentException
import java.nio.file.Paths
import kotlinx.datetime.LocalDate

class YamlDataParserTest : DescribeSpec({
  val yamlDataParser = YamlDataParser()
  describe("basic") {
    it("empty") {
      val relativeFilePath = Paths.get("path/file.yaml")
      val error = shouldThrow<IllegalArgumentException> {
        yamlDataParser.parseContent("", relativeFilePath)
      }
      error.message shouldContain "No content"
    }
    it("just name") {
      val relativeFilePath = Paths.get("path/file.yaml")
      val data = yamlDataParser.parseContent("name: test-account", relativeFilePath)
      data shouldBe YamlData(
        name = "test-account",
        desc = null,
        number = null,
        path = null,
        type = null,
        openedOn = null,
        closedOn = null,
        owner = null,
        url = null,
        phone = null,
        address = null,
        username = null,
        pswd = null,
        balances = null,
        transfersTo = null
      )
    }
    it("all top fields") {
      val relativeFilePath = Paths.get("path/file.yaml")
      val data = yamlDataParser.parseContent("""
        name: test-account
        desc: "basic test account"
        number: 123
        path: [top, branch, leaf]
        type: "credit"
        opened_on: Jun2026
        closed_on: Jun2027
        owner: [john, bob]
        url: http://test-account.com
        phone: 555-123-4567
        address: 123 main st
        username: john123
        pswd: pwd123
        balances:
        transfers_to:
      """, relativeFilePath)
      data shouldBe YamlData(
        name = "test-account",
        desc = "basic test account",
        number = "123",
        path = listOf("top", "branch", "leaf"),
        type = "credit",
        openedOn = JUN / 2026,
        closedOn = JUN / 2027,
        owner = listOf("john", "bob"),
        url = "http://test-account.com",
        phone = "555-123-4567",
        address = "123 main st",
        username = "john123",
        pswd = "pwd123",
        balances = null,
        transfersTo = null
      )
    }
    it("with balances") {
      val relativeFilePath = Paths.get("path/file.yaml")
      val data = yamlDataParser.parseContent("""
        name: test-account
        balances:
          - { grp: Jul2026, date: 2026-07-01, camt: 2.00 }
          - { grp: Jun2026, date: 2026-06-01, camt: 1.00 }
      """, relativeFilePath)
      data shouldBe YamlData(
        name = "test-account",
        desc = null,
        number = null,
        path = null,
        type = null,
        openedOn = null,
        closedOn = null,
        owner = null,
        url = null,
        phone = null,
        address = null,
        username = null,
        pswd = null,
        balances = listOf(
          BalanceYamlData(grp=JUL / 2026, date=LocalDate(2026, 7, 1), camt=2.0, pamt=null, desc=null),
          BalanceYamlData(grp=JUN / 2026, date=LocalDate(2026, 6, 1), camt=1.0, pamt=null, desc=null)
        ),
        transfersTo = null
      )
    }
    it("with transfers") {
      val relativeFilePath = Paths.get("path/file.yaml")
      val data = yamlDataParser.parseContent("""
        name: test-account
        transfers_to:
          test-account2:
            - { grp: Jun2026, date: 2026-06-20, camt: 500.0, desc: "Refund" }
      """, relativeFilePath)
      data shouldBe YamlData(
        name = "test-account",
        desc = null,
        number = null,
        path = null,
        type = null,
        openedOn = null,
        closedOn = null,
        owner = null,
        url = null,
        phone = null,
        address = null,
        username = null,
        pswd = null,
        balances = null,
        transfersTo = mapOf(
          "test-account2" to listOf(
            TransferYamlData(
              grp = JUN / 2026,
              date = LocalDate(2026, 6, 20),
              camt = 500.0,
              pamt = null,
              desc = "Refund",
              cat = null,
              tags = null
            )
          )
        )
      )
    }
    it("fails with bad balance month") {
      val relativeFilePath = Paths.get("path/file.yaml")
      val content = """
      name: test-account
      owner: [ someone ]
      path: [ external ]
      opened_on: Jan2020
      balances:
      - { grp: Xxx2020, date: 2020-01-01, camt:  0.00 }
      """.trimIndent()
      val exception = shouldThrow<kotlin.IllegalArgumentException> {
        yamlDataParser.parseContent(content, relativeFilePath)
      }
      exception.message shouldContain "Bad month name 'Xxx' for 'Xxx2020', valid names " +
          "[Jan, Feb, Mar, Apr, May, Jun, Jul, Aug, Sep, Oct, Nov, Dec]"
    }
    it("fails with bad balance date") {
      val relativeFilePath = Paths.get("path/file.yaml")
      val content = """
      name: test-account
      owner: [ someone ]
      path: [ external ]
      opened_on: Jan2020
      balances:
      - { grp: Jan2020, date: 20200101, camt:  0.00 }
      """.trimIndent()
      val exception = shouldThrow<kotlin.IllegalArgumentException> {
        yamlDataParser.parseContent(content, relativeFilePath)
      }
      exception.message shouldContain "Text '20200101' could not be parsed"
      exception.message shouldContain "while processing path/file.yaml"
    }
  }
})
