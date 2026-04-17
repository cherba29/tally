package com.cherba29.tally.data

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.Budget
import com.cherba29.tally.core.MonthName.MAR
import com.cherba29.tally.core.NodeId
import com.cherba29.tally.utils.Map3
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.engine.coroutines.testScheduler
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.writeText
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.datetime.LocalDate
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

@OptIn(ExperimentalCoroutinesApi::class)
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

      Loader(tallyPath.watchedEventFlow { true }).use { loader ->
        val result = loader.budget()

        val nodeId = NodeId("test-account", setOf("someone"), listOf("external"))
        result.statements.size shouldBe 1
        val tranStatement = result.statements[nodeId]?.get(MAR / 2019)!!
        tranStatement.monthRange shouldBe MAR / 2019..MAR / 2019
        tranStatement.nodeId.name shouldBe "test-account"

        result.budget.balances[nodeId]?.get(MAR / 2019) shouldBe Balance(
          10000, LocalDate(2019, 3, 1),
          Balance.Type.CONFIRMED
        )
      }
    }

    describe("reloads when changed") {
      coroutineTestScope = true

      it("reloads when changed") {
        val channel = Channel<WatchResult>(Channel.BUFFERED)
        val rootPath = Paths.get("/tmp")
        val relativePath = Paths.get("file.yaml")
        channel.trySend(WatchResult(rootPath, relativePath, false)).isSuccess shouldBe true
        val processedBudget = mockk<ProcessedBudget> {
          var count = 1
          every { addFile(any<Path>(), any()) } answers { }
          every { reProcess() } answers { }
          every { dataPayload } answers {
            DataPayload(mockk<Budget>(),mapOf(
              NodeId("testAccount${count++}") to mapOf()
            ), Map3())
          }
        }
        val timeSource = TestTimeSource()

        timeSource += 50.seconds
        Loader(channel.receiveAsFlow(), this, timeSource, processedBudget).use { loader ->
          channel.trySend(WatchResult(rootPath, null, true)).isSuccess shouldBe true

          val result1 = loader.budget()
          result1.statements.size shouldBe 1
          result1.statements[NodeId("testAccount1")] shouldNotBe null
          val loadedOn = loader.loadedOn
          loader.loadedOn shouldBe 0.seconds

          timeSource += 10.seconds

          channel.trySend(WatchResult(rootPath, relativePath, true)).isSuccess shouldBe true
          timeSource += 10.seconds

          testScheduler.advanceTimeBy(1000)

          // Now new reloaded budget.
          loadedOn!! shouldBeLessThan loader.loadedOn!!
          loader.loadedOn shouldBe 20.seconds

          val result2 = loader.budget()
          result1.statements.size shouldBe 1
          result2.statements[NodeId("testAccount1")] shouldBe null
          result2.statements[NodeId("testAccount2")] shouldNotBe null

          verify { processedBudget.addFile(rootPath, relativePath) }
          verify(exactly = 2) { processedBudget.reProcess() }
        }
      }

      it("ignoring error") {
        val timeSource = TestTimeSource()
        val channel = Channel<WatchResult>(Channel.BUFFERED)
        val rootPath = Paths.get("/tmp")
        val relativePath = Paths.get("file.yaml")
        channel.trySend(WatchResult(rootPath, relativePath, false)).isSuccess shouldBe true
        val processedBudget = mockk<ProcessedBudget> {
          var count = 1
          every { addFile(any<Path>(), any()) } answers {
            if (count > 1) throw IllegalArgumentException("error")
          }
          every { reProcess() } answers { }
          every { dataPayload } answers {
            DataPayload(mockk<Budget>(),mapOf(
              NodeId("testAccount${count++}") to mapOf()
            ), Map3())
          }
        }
        timeSource += 50.seconds
        Loader(channel.receiveAsFlow(), this, timeSource, processedBudget).use { loader ->
          channel.trySend(WatchResult(rootPath, null, true)).isSuccess shouldBe true
          timeSource += 100.seconds

          val result1 = loader.budget()
          result1.statements.size shouldBe 1
          result1.statements[NodeId("testAccount1")] shouldNotBe null
          loader.loadedOn shouldBe 100.seconds

          timeSource += 100.seconds
          channel.trySend(WatchResult(rootPath, relativePath, true)).isSuccess shouldBe true
          testScheduler.advanceTimeBy(1000)

          loader.loadedOn shouldBe 100.seconds  // Should not change since reprocess fails.

          val result2 = loader.budget()
          result2.statements.size shouldBe 1
          result2.statements[NodeId("testAccount1")] shouldNotBe null
          result2.statements[NodeId("testAccount2")] shouldBe null
          channel.close() shouldBe true

          verify { processedBudget.addFile(rootPath, relativePath) }
          verify(exactly = 1) { processedBudget.reProcess() }  // Second call was not made due to error.
        }
      }
    }
  }
})
