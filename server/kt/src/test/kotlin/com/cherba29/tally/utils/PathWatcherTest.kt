package com.cherba29.tally.utils

import app.cash.turbine.test
import app.cash.turbine.turbineScope
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.engine.coroutines.backgroundScope
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
import java.nio.file.Paths
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.div
import kotlin.io.path.writeText
import kotlin.time.Duration.Companion.milliseconds

class PathWatcherTest : DescribeSpec({
  describe("empty directory") {
    it("returns empty on non-existent directory") {
      val folder = Paths.get("tmp/tally-123")
      val result = mutableListOf<WatchResult>()
      folder.watchedEventFlow { true }.takeWhile { it.action != WatchResult.Action.REPROCESS }.toList(result)
      result shouldBe listOf()
    }

    it("returns empty on empty directory") {
      val folder = tempdir("tally-", keepOnFailure = false).toPath()
      val result = mutableListOf<WatchResult>()
      folder.watchedEventFlow { true }.takeWhile { it.action != WatchResult.Action.REPROCESS }.toList(result)
      result shouldBe listOf()
    }

    it("returns empty if only subdirectory exists") {
      val folder = tempdir("tally-", keepOnFailure = false).toPath()
      (folder / "subdirectory").createDirectory()
      val result = mutableListOf<WatchResult>()
      folder.watchedEventFlow { true }.takeWhile { it.action != WatchResult.Action.REPROCESS }.toList(result)
      result shouldBe listOf()
    }
  }

  describe("emits existing") {
    it("returns single file in root") {
      val folder = tempdir("tally-", keepOnFailure = false).toPath()
      (folder / "file2.yaml").createFile()

      folder.watchedEventFlow { true }.takeWhile { it.action != WatchResult.Action.REPROCESS }.test {
        awaitItem() shouldBe WatchResult(folder, Paths.get("file2.yaml"), WatchResult.Action.ADD)
        awaitComplete()
      }
    }

    it("returns single file in subdirectory") {
      val folder = tempdir("tally-", keepOnFailure = false).toPath()
      (folder / "subfolder").createDirectory()
      (folder / "subfolder" / "file2.yaml").createFile()

      folder.watchedEventFlow { true }.takeWhile { it.action != WatchResult.Action.REPROCESS }.test {
        awaitItem() shouldBe WatchResult(folder, Paths.get("subfolder/file2.yaml"), WatchResult.Action.ADD)
        awaitComplete()
      }
    }
  }

  describe("emits modified") {
    coroutineTestScope = true

    it("returns single file in root") {
      val folder = tempdir("tally-", keepOnFailure = false).toPath()
      val targetFile = (folder / "file2.yaml").createFile()

      turbineScope {
        val flow = folder.watchedEventFlow { true }.testIn(backgroundScope)
        flow.awaitItem() shouldBe WatchResult(folder, relativePath = Paths.get("file2.yaml"), WatchResult.Action.ADD)
        flow.awaitItem() shouldBe WatchResult(folder, relativePath = null, WatchResult.Action.REPROCESS)
        targetFile.writeText("hello")
        flow.awaitItem() shouldBe WatchResult(folder, relativePath = Paths.get("file2.yaml"), WatchResult.Action.REPROCESS)
        flow.cancelAndConsumeRemainingEvents() shouldBe listOf()
      }
    }

    it("multiple updates as one") {
      val folder = tempdir("tally-", keepOnFailure = false).toPath()
      val targetFile = (folder / "file2.yaml").createFile()

      turbineScope {
        val flow = folder.watchedEventFlow { true }.testIn(backgroundScope)
        flow.awaitItem() shouldBe WatchResult(folder, relativePath = Paths.get("file2.yaml"), WatchResult.Action.ADD)
        flow.awaitItem() shouldBe WatchResult(folder, relativePath = null, WatchResult.Action.REPROCESS)
        targetFile.toFile().bufferedWriter().use {
          it.write("First line")
          it.write("Second line")
        }
        flow.awaitItem() shouldBe WatchResult(folder, relativePath = Paths.get("file2.yaml"), WatchResult.Action.REPROCESS)
        flow.cancelAndConsumeRemainingEvents() shouldBe listOf()
      }
    }

    it("returns new file") {
      val folder = tempdir("tally-", keepOnFailure = false).toPath()
      (folder / "file1.yaml").createFile()

      turbineScope {
        val flow = folder.watchedEventFlow { true }.testIn(backgroundScope)
        flow.awaitItem() shouldBe WatchResult(folder, relativePath = Paths.get("file1.yaml"), WatchResult.Action.ADD)
        flow.awaitItem() shouldBe WatchResult(folder, relativePath = null, WatchResult.Action.REPROCESS)
        (folder / "file2.yaml").createFile()
        flow.awaitItem() shouldBe WatchResult(folder, relativePath = Paths.get("file2.yaml"), WatchResult.Action.REPROCESS)
        flow.cancelAndConsumeRemainingEvents() shouldBe listOf()
      }
    }

    it("returns new file within new directory")  {
      val folder = tempdir("tally-", keepOnFailure = false).toPath()
      (folder / "file1.yaml").createFile()

      turbineScope {
        val flow = folder.watchedEventFlow { true }.testIn(backgroundScope)
        flow.awaitItem() shouldBe WatchResult(folder, relativePath = Paths.get("file1.yaml"), WatchResult.Action.ADD)
        flow.awaitItem() shouldBe WatchResult(folder, relativePath = null, WatchResult.Action.REPROCESS)
        val subFolder = (folder / "subpath").createDirectory()
        (subFolder / "file2.yaml").createFile()
        flow.awaitItem() shouldBe WatchResult(folder, relativePath = Paths.get("subpath/file2.yaml"), WatchResult.Action.ADD)
        flow.awaitItem() shouldBe WatchResult(folder, relativePath = null, WatchResult.Action.REPROCESS)
        flow.cancelAndConsumeRemainingEvents() shouldBe listOf()
      }
    }

    it("notifies of file removal") {
      val folder = tempdir("tally-", keepOnFailure = false).toPath()
      val file1 = (folder / "file1.yaml").createFile()

      turbineScope {
        val flow = folder.watchedEventFlow { true }.testIn(backgroundScope)
        flow.awaitItem() shouldBe WatchResult(folder, relativePath = Paths.get("file1.yaml"), WatchResult.Action.ADD)
        flow.awaitItem() shouldBe WatchResult(folder, relativePath = null, WatchResult.Action.REPROCESS)
        file1.deleteIfExists() shouldBe true
        flow.awaitItem() shouldBe WatchResult(folder, relativePath = Paths.get("file1.yaml"), WatchResult.Action.REMOVE)
        flow.cancelAndConsumeRemainingEvents() shouldBe listOf()
      }
    }

  }
})
