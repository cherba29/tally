package com.cherba29.tally.data

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.BalanceType
import com.cherba29.tally.core.Month
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.writeText
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate

class LoaderTest : DescribeSpec({
  describe("listFiles") {
    it("returns empty on empty directory") {
      val tallyPath = tempdir("tally-", keepOnFailure = false).toPath()
      listFiles(tallyPath) shouldBe listOf()
    }

    it("returns recursively yaml file entries") {
      val tallyPath = tempdir("tally-", keepOnFailure = false).toPath()
      val subdir1 = (tallyPath / "subdir1").createDirectories()
      (subdir1 / "file1.json").createFile()
      (subdir1 / "file2.yaml").createFile()
      val subdir2 = (tallyPath / "subdir2").createDirectories()
      (subdir2 / "file1.json").createFile()
      (subdir2 / "file2.yaml").createFile()

      listFiles(tallyPath) shouldBe listOf("subdir1/file2.yaml", "subdir2/file2.yaml")
    }
  }

  describe("loadBudget") {
    it("just single account") {
      val tallyPath = tempdir("tally-", keepOnFailure = false).toPath()
      (tallyPath / "file2.yaml").createFile().writeText(
        """
        name: test-account
        owner: [someone]
        type: external
        opened_on: Mar2019
        balances:
          - { grp: Mar2019, date: 2019-03-01, camt: 100.00 }
        """.trimIndent()
      )

      Loader(tallyPath).use { loader ->
        val result = loader.loadBudget()

        result.statements.size shouldBe 1
        val tranStatement = result.statements["test-account"]?.get("Mar2019")!!
        tranStatement.month shouldBe Month(2019, 2)
        tranStatement.account.name shouldBe "test-account"

        result.budget.balances["test-account"]?.get("Mar2019") shouldBe Balance(
          10000.0, LocalDate(2019, 3, 1),
          BalanceType.CONFIRMED
        )
      }
    }

    it("reloads when changed") {
      val tallyPath = tempdir("tally-", keepOnFailure = false).toPath()
      (tallyPath / "file2.yaml").createFile().writeText(
        """
          name: test-account
          owner: [someone]
          type: external
          opened_on: Mar2019
          balances:
            - { grp: Mar2019, date: 2019-03-01, camt: 100.00 }
          """.trimIndent()
      )

      Loader(tallyPath).use { loader ->
        val result1 = loader.loadBudget()
        result1.budget.balances["test-account"]?.get("Mar2019") shouldBe Balance(
          10000.0, LocalDate(2019, 3, 1),
          BalanceType.CONFIRMED
        )
        val loadedOn = loader.loadedOn

        (tallyPath / "file2.yaml").writeText(
          """
          name: test-account
          owner: [someone]
          type: external
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

        val result2 = loader.loadBudget()
        result2.budget.balances["test-account"]?.get("Mar2019") shouldBe Balance(
          20000.0, LocalDate(2019, 3, 1),
          BalanceType.CONFIRMED
        )
      }
    }
  }
})
