package com.cherba29.tally.data

import app.cash.turbine.test
import app.cash.turbine.turbineScope
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.engine.coroutines.backgroundScope
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import java.nio.file.Paths
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.writeText
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList

class PathWatcherTest : DescribeSpec({
  //coroutineTestScope = true

  describe("empty directory") {
    it("returns empty on non-existent directory") {
      val folder = Paths.get("tmp/tally-123")
      val result = mutableListOf<WatchResult>()
      folder.watchedEventFlow { true }.takeWhile { !it.reprocess }.toList(result)
      result shouldBe listOf()
    }

    it("returns empty on empty directory") {
      val folder = tempdir("tally-", keepOnFailure = false).toPath()
      val result = mutableListOf<WatchResult>()
      folder.watchedEventFlow { true }.takeWhile { !it.reprocess }.toList(result)
      result shouldBe listOf()
    }

    it("returns empty if only subdirectory exists") {
      val folder = tempdir("tally-", keepOnFailure = false).toPath()
      (folder / "subdirectory").createDirectory()
      val result = mutableListOf<WatchResult>()
      folder.watchedEventFlow { true }.takeWhile { !it.reprocess }.toList(result)
      result shouldBe listOf()
    }

  }

  describe("emits existing") {
    it("returns single file in root") {
      val folder = tempdir("tally-", keepOnFailure = false).toPath()
      (folder / "file2.yaml").createFile()

      folder.watchedEventFlow { true }.takeWhile { !it.reprocess }.test {
        awaitItem() shouldBe WatchResult(folder, Paths.get("file2.yaml"), reprocess=false)
        awaitComplete()
      }
    }

    it("returns single file in subdirectory") {
      val folder = tempdir("tally-", keepOnFailure = false).toPath()
      (folder / "subfolder").createDirectory()
      (folder / "subfolder" / "file2.yaml").createFile()

      folder.watchedEventFlow { true }.takeWhile { !it.reprocess }.test {
        awaitItem() shouldBe WatchResult(folder, Paths.get("subfolder/file2.yaml"), reprocess=false)
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
        flow.awaitItem() shouldBe WatchResult(folder, relativePath = Paths.get("file2.yaml"), reprocess = false)
        flow.awaitItem() shouldBe WatchResult(folder, relativePath = null, reprocess = true)
        targetFile.writeText("hello")
        flow.awaitItem() shouldBe WatchResult(folder, relativePath = Paths.get("file2.yaml"), reprocess = true)
        flow.cancelAndConsumeRemainingEvents() shouldBe listOf()
      }
    }
  }
})
