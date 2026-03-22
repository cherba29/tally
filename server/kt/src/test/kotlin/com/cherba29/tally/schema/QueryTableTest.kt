package com.cherba29.tally.schema

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.MonthName.JAN
import com.cherba29.tally.core.MonthName.MAR
import com.cherba29.tally.core.budget
import com.cherba29.tally.data.Loader.DataPayload
import com.cherba29.tally.statement.SummaryStatement
import com.cherba29.tally.statement.TransactionStatement
import com.cherba29.tally.utils.Map3
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate

class QueryTableTest : DescribeSpec({
  describe("buildGqlTable") {
    it("no owner") {
      val payload = DataPayload(
        budget = budget {},
        statements = mapOf(),
        summaries = Map3()
      )
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
      val payload = DataPayload(
        budget =  budget {},
        statements = mapOf(),
        summaries = Map3()
      )
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
        name = "test-account",
        type = Account.Type.EXTERNAL,
        owners = listOf("john"),
      )
      val summary = SummaryStatement(
        account,
        month = MAR / 2026,
        startMonth = MAR / 2026
      )
      val summaries = Map3<SummaryStatement>()
      summaries.set("john", "/", "Mar2026", summary)
      val payload = DataPayload(
        budget =  budget {},
        statements = mapOf(),
        summaries
      )
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
      val account = Account(
        name = "test-account",
        type = Account.Type.EXTERNAL,
        owners = listOf("john"),
        openedOn = JAN / 2026
      )
      val summary = SummaryStatement(
        account,
        month = MAR / 2026,
        startMonth = MAR / 2026
      )
      val summaries = Map3<SummaryStatement>()
      summaries.set("john", "/", "Mar2026", summary)
      val payload = DataPayload(
        budget = budget {
          setAccount(account)
          setBalance(
            "test-account", MAR / 2026, Balance(
              amount = 100,
              date = LocalDate(2026, 3, 1),
              type = Balance.Type.CONFIRMED
          ))
        },
        statements = mapOf(),
        summaries
      )
      val table = buildGqlTable(
        payload = payload,
        owner = "john",
        startMonth = MAR / 2026,
        endMonth = MAR / 2026
      )
      table.months shouldBe listOf(MAR / 2026)
      table.owners shouldBe listOf("john")
      table.currentOwner shouldBe "john"
      table.rows shouldBe listOf(
        GqlTableRow(
          title="john",
          account=GqlAccount(
            name="test-account",
            description="",
            path=listOf(),
            type="EXTERNAL",
            external=true,
            summary=false,
            number=null,
            openedOn=JAN / 2026,
            closedOn=null,
            owners=listOf("john"),
            url="",
            address="",
            userName="",
            password="",
            phone=""
          ),
          indent=0,
          isSpace=false,
          isTotal=true,
          isNormal=false,
          cells=listOf(
            GqlTableCell(
              month=MAR / 2026,
              isClosed=false,
              addSub=0,
              balance=null,
              isProjected=true,
              isCovered=false,
              isProjectedCovered=false,
              hasProjectedTransfer=false,
              percentChange=0.0f,
              annualizedPercentChange=0.0f,
              unaccounted=null,
              balanced=true
            )
          )
        )
      )
    }

    it("single open account with path") {
      val account = Account(
        name = "test-account",
        type = Account.Type.EXTERNAL,
        path = listOf("outside"),
        owners = listOf("john"),
        openedOn = JAN / 2026
      )
      val summary = SummaryStatement(
        account,
        month = MAR / 2026,
        startMonth = MAR / 2026
      )
      val transactionStatement = TransactionStatement(
        account,
        month = MAR / 2026,
        startBalance = Balance(
          amount = 100,
          date = LocalDate(2026, 3, 1),
          type = Balance.Type.CONFIRMED
        )
      )
      val summaries = Map3<SummaryStatement>()
      summaries.set("john", "/outside", "Mar2026", summary)
      summaries.set("john", "/", "Mar2026", summary)
      val payload = DataPayload(
        budget = budget {
          setAccount(account)
          setBalance(
            "test-account", MAR / 2026, Balance(
              amount = 100,
              date = LocalDate(2026, 3, 1),
              type = Balance.Type.CONFIRMED
            ))
        },
        statements = mapOf(account.name to mapOf(MAR / 2026 to transactionStatement)),
        summaries
      )
      val table = buildGqlTable(
        payload = payload,
        owner = "john",
        startMonth = MAR / 2026,
        endMonth = MAR / 2026
      )
      table.months shouldBe listOf(MAR / 2026)
      table.owners shouldBe listOf("john")
      table.currentOwner shouldBe "john"
      table.rows shouldBe listOf(
        GqlTableRow(
          title="john",
          account=GqlAccount(
            name="test-account",
            description="", path=listOf("outside"),
            type="EXTERNAL",
            external=true,
            summary=false,
            number=null,
            openedOn=JAN / 2026,
            closedOn=null,
            owners=listOf("john"),
            url="",
            address="",
            userName="",
            password="",
            phone=""
          ),
          indent=0,
          isSpace=false,
          isTotal=true,
          isNormal=false,
          cells=listOf(
            GqlTableCell(
              month=MAR / 2026,
              isClosed=false,
              addSub=0,
              balance=null,
              isProjected=true,
              isCovered=false,
              isProjectedCovered=false,
              hasProjectedTransfer=false,
              percentChange=0.0f,
              annualizedPercentChange=0.0f,
              unaccounted=null,
              balanced=true
            )
          )
        ),
        GqlTableRow(
          title="outside",
          account=GqlAccount(
            name="test-account",
            description="",
            path=listOf("outside"),
            type="EXTERNAL",
            external=true,
            summary=false,
            number=null,
            openedOn=JAN / 2026,
            closedOn=null,
            owners=listOf("john"),
            url="",
            address="",
            userName="",
            password="",
            phone=""
          ),
          indent=1,
          isSpace=false,
          isTotal=true,
          isNormal=false,
          cells=listOf(
            GqlTableCell(
              month=MAR / 2026,
              isClosed=false,
              addSub=0,
              balance=null,
              isProjected=true,
              isCovered=false,
              isProjectedCovered=false,
              hasProjectedTransfer=false,
              percentChange=0.0f,
              annualizedPercentChange=0.0f,
              unaccounted=null,
              balanced=true
            )
          )
        ),
        GqlTableRow(
          title="test-account",
          account=GqlAccount(
            name="test-account",
            description="",
            path=listOf("outside"),
            type="EXTERNAL",
            external=true,
            summary=false,
            number=null,
            openedOn=JAN / 2026,
            closedOn=null,
            owners=listOf("john"),
            url="",
            address="",
            userName="",
            password="",
            phone=""
          ),
          indent=2,
          isSpace=false,
          isTotal=false,
          isNormal=true,
          cells=listOf(
            GqlTableCell(
              month=MAR / 2026,
              isClosed=false,
              addSub=0,
              balance=null,
              isProjected=false,
              isCovered=false,
              isProjectedCovered=false,
              hasProjectedTransfer=false,
              percentChange=0.0f,
              annualizedPercentChange=0.0f,
              unaccounted=null,
              balanced=true
            )
          )
        )
      )
    }
  }
})