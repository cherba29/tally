package com.cherba29.tally.data

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.MonthName.MAR
import com.cherba29.tally.core.root
import com.cherba29.tally.utils.WatchResult
import com.cherba29.tally.utils.watchedEventFlow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.engine.coroutines.testScheduler
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
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
import kotlin.time.Duration
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

        result.nodeToStatement.size shouldBe 3
        val tranStatement = result.nodeToStatement[result.tree[listOf("someone", "external", "test-account")]]?.get(MAR / 2019)!!
        tranStatement.monthRange shouldBe MAR / 2019..MAR / 2019
        tranStatement.treeNode.name shouldBe "test-account"

        tranStatement.startBalance shouldBe Balance(
          10000, LocalDate(2019, 3, 1),
          Balance.Type.CONFIRMED
        )
      }
    }

    describe("load") {
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

        val result = Loader.loadFrom(tallyPath)

        result.nodeToStatement.size shouldBe 3
        val tranStatement =
          result.nodeToStatement[result.tree[listOf("someone", "external", "test-account")]]?.get(MAR / 2019)!!
        tranStatement.monthRange shouldBe MAR / 2019..MAR / 2019
        tranStatement.treeNode.name shouldBe "test-account"

        tranStatement.startBalance shouldBe Balance(
          10000, LocalDate(2019, 3, 1),
          Balance.Type.CONFIRMED
        )
      }
    }

    describe("reloads when changed") {
      coroutineTestScope = true

      it("reloads when changed") {
        val testTimeSource = TestTimeSource()
        val channel = Channel<WatchResult>(Channel.BUFFERED)
        val rootPath = Paths.get("/tmp")
        val relativePath = Paths.get("file.yaml")
        channel.trySend(WatchResult(rootPath, relativePath, false)).isSuccess shouldBe true
        val processedBudget = mockk<ProcessedBudget> {
          var count = 1
          val startTime = testTimeSource.markNow()
          var mockLoadedOn: Duration? = null
          every { timeSource } answers { testTimeSource }
          every { loadedOn } answers { mockLoadedOn }
          every { addFile(any<Path>(), any()) } answers { }
          every { reProcess() } answers { mockLoadedOn = startTime.elapsedNow() }
          every { dataPayload } answers {
            mockk<Budget> {
              every { tree } returns root { leaf("testAccount${count++}") }
            }
          }
        }

        testTimeSource += 50.seconds
        Loader(channel.receiveAsFlow(), this, testTimeSource, processedBudget).use { loader ->
          channel.trySend(WatchResult(rootPath, null, true)).isSuccess shouldBe true

          val result1 = loader.budget()
          result1.tree shouldBe root { leaf("testAccount1") }
          val loadedOn = loader.loadedOn
          loader.loadedOn shouldBe 50.seconds

          testTimeSource += 10.seconds

          channel.trySend(WatchResult(rootPath, relativePath, true)).isSuccess shouldBe true
          testTimeSource += 10.seconds

          testScheduler.advanceTimeBy(1000)

          // Now new reloaded budget.
          loadedOn!! shouldBeLessThan loader.loadedOn!!
          loader.loadedOn shouldBe 70.seconds

          val result2 = loader.budget()
          result2.tree shouldBe root { leaf("testAccount2") }

          verify { processedBudget.addFile(rootPath, relativePath) }
          verify(exactly = 2) { processedBudget.reProcess() }
        }
      }

      it("ignoring add file error") {
        val testTimeSource = TestTimeSource()
        val channel = Channel<WatchResult>(Channel.BUFFERED)
        val rootPath = Paths.get("/tmp")
        val relativePath = Paths.get("file.yaml")
        channel.trySend(WatchResult(rootPath, relativePath, false)).isSuccess shouldBe true
        val processedBudget = mockk<ProcessedBudget> {
          var count = 1
          val startTime = testTimeSource.markNow()
          var mockLoadedOn: Duration? = null
          every { timeSource } answers { testTimeSource }
          every { loadedOn } answers { mockLoadedOn }
          every { addFile(any<Path>(), any()) } answers {
            if (count > 1) throw IllegalArgumentException("error")
          }
          every { reProcess() } answers { mockLoadedOn = startTime.elapsedNow() }
          every { dataPayload } answers {
            mockk<Budget> {
              every { tree } returns root { leaf("testAccount${count++}") }
            }
          }
        }
        testTimeSource += 50.seconds
        Loader(channel.receiveAsFlow(), this, testTimeSource, processedBudget).use { loader ->
          channel.trySend(WatchResult(rootPath, null, true)).isSuccess shouldBe true
          testTimeSource += 100.seconds

          val result1 = loader.budget()
          result1.tree shouldBe root { leaf("testAccount1") }
          loader.loadedOn shouldBe 150.seconds

          testTimeSource += 100.seconds
          channel.trySend(WatchResult(rootPath, relativePath, true)).isSuccess shouldBe true
          testScheduler.advanceTimeBy(1000)

          loader.loadedOn shouldBe 150.seconds  // Should not change since add file fails.

          val result2 = loader.budget()
          result2.tree shouldBe root { leaf("testAccount1") }
          channel.close() shouldBe true

          verify { processedBudget.addFile(rootPath, relativePath) }
          verify(exactly = 1) { processedBudget.reProcess() }  // Second call was not made due to error.
        }
      }

      it("ignoring reprocess error") {
        val testTimeSource = TestTimeSource()
        val channel = Channel<WatchResult>(Channel.BUFFERED)
        val rootPath = Paths.get("/tmp")
        val relativePath = Paths.get("file.yaml")
        channel.trySend(WatchResult(rootPath, relativePath, false)).isSuccess shouldBe true
        val processedBudget = mockk<ProcessedBudget> {
          var count = 1
          val startTime = testTimeSource.markNow()
          var mockLoadedOn: Duration? = null
          every { timeSource } answers { testTimeSource }
          every { loadedOn } answers { mockLoadedOn }
          every { addFile(any<Path>(), any()) } answers { }
          every { reProcess() } answers {
            if (count > 1) throw IllegalArgumentException("error")
            mockLoadedOn = startTime.elapsedNow()
          }
          every { dataPayload } answers {
            mockk<Budget> {
              every { tree } returns root { leaf("testAccount${count++}") }
            }
          }
        }
        testTimeSource += 50.seconds
        Loader(channel.receiveAsFlow(), this, testTimeSource, processedBudget).use { loader ->
          channel.trySend(WatchResult(rootPath, null, true)).isSuccess shouldBe true
          testTimeSource += 100.seconds

          val result1 = loader.budget()
          result1.tree shouldBe root { leaf("testAccount1") }
          loader.loadedOn shouldBe 150.seconds

          testTimeSource += 100.seconds
          channel.trySend(WatchResult(rootPath, relativePath, true)).isSuccess shouldBe true
          testScheduler.advanceTimeBy(1000)

          loader.loadedOn shouldBe 150.seconds  // Should not change since reprocess fails.

          val result2 = loader.budget()
          result2.tree shouldBe root { leaf("testAccount1") }
          channel.close() shouldBe true

          verify(exactly = 2) { processedBudget.addFile(rootPath, relativePath) }
          verify(exactly = 2) { processedBudget.reProcess() }
          verify(exactly = 1) { processedBudget.dataPayload }  // Second call was not made due to error.
        }
      }
    }
  }
})
