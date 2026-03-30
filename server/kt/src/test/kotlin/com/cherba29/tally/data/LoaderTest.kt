package com.cherba29.tally.data

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.MonthName.MAR
import com.cherba29.tally.core.NodeId
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.writeText
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate

class LoaderTest : DescribeSpec({
  describe("loadBudget") {
    it("just single account") {
      val tallyPath = tempdir("tally-", keepOnFailure = false).toPath()
      (tallyPath / "file2.yaml").createFile().writeText(
        """
        name: test-account
        owner: [someone]
        path: [external]
        opened_on: Mar2019
        balances:
          - { grp: Mar2019, date: 2019-03-01, camt: 100.00 }
        """.trimIndent()
      )

      Loader(tallyPath).use { loader ->
        val result = loader.budget

        val nodeId = NodeId("test-account", listOf("someone"), listOf("external"))
        result.statements.size shouldBe 1
        val tranStatement = result.statements[nodeId]?.get(MAR / 2019)!!
        tranStatement.month shouldBe Month(2019, 2)
        tranStatement.nodeId.name shouldBe "test-account"

        result.budget.balances[nodeId]?.get(MAR / 2019) shouldBe Balance(
          10000, LocalDate(2019, 3, 1),
          Balance.Type.CONFIRMED
        )
      }
    }

    it("reloads when changed") {
      val tallyPath = tempdir("tally-", keepOnFailure = false).toPath()
      (tallyPath / "file2.yaml").createFile().writeText(
        """
          name: test-account
          owner: [someone]
          path: [external]
          opened_on: Mar2019
          balances:
            - { grp: Mar2019, date: 2019-03-01, camt: 100.00 }
          """.trimIndent()
      )

      Loader(tallyPath).use { loader ->
        val result1 = loader.budget
        val nodeId = NodeId("test-account", listOf("someone"), listOf("external"))
        result1.budget.balances[nodeId]?.get(MAR / 2019) shouldBe Balance(
          10000, LocalDate(2019, 3, 1),
          Balance.Type.CONFIRMED
        )
        val loadedOn = loader.loadedOn

        (tallyPath / "file2.yaml").writeText(
          """
          name: test-account
          owner: [someone]
          path: [external]
          opened_on: Mar2019
          balances:
            - { grp: Mar2019, date: 2019-03-01, camt: 200.00 }
          """.trimIndent()
        )
        repeat(10) {
          if (loadedOn < loader.loadedOn) return@repeat
          delay(200)
        }
        loadedOn shouldBeLessThan loader.loadedOn

        val result2 = loader.budget
        result2.budget.balances[nodeId]?.get(MAR / 2019) shouldBe Balance(
          20000, LocalDate(2019, 3, 1),
          Balance.Type.CONFIRMED
        )
      }
    }

    it("reloads when changed ignoring error") {
      val tallyPath = tempdir("tally-", keepOnFailure = false).toPath()
      (tallyPath / "file2.yaml").createFile().writeText(
        """
          name: test-account
          owner: [someone]
          path: [ external ]
          opened_on: Mar2019
          balances:
            - { grp: Mar2019, date: 2019-03-01, camt: 100.00 }
          """.trimIndent()
      )

      Loader(tallyPath).use { loader ->
        val result1 = loader.budget
        val nodeId = NodeId("test-account", listOf("someone"), listOf("external"))
        result1.budget.balances[nodeId]?.get(MAR / 2019) shouldBe Balance(
          10000, LocalDate(2019, 3, 1),
          Balance.Type.CONFIRMED
        )
        val loadedOn = loader.loadedOn

        (tallyPath / "file2.yaml").writeText(
          """
          name: test-account
          owner: [someone]
          path: [ external ]
          opened_on: Mar2019
          balances:
            - { grp: Mar2019, date: 2019-0301, camt: 200.00 }
          """.trimIndent()
        )
        // TODO: inject change flow into loader for better testing.
        repeat(10) {
          if (loadedOn < loader.loadedOn) return@repeat
          delay(200)
        }
        loader.loadedOn shouldBe loadedOn

        val result2 = loader.budget
        result2.budget.balances[nodeId]?.get(MAR / 2019) shouldBe Balance(
          10000, LocalDate(2019, 3, 1),
          Balance.Type.CONFIRMED
        )
      }
    }

  }
})
