package com.cherba29.tally.data

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import java.nio.file.Paths
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.writeText
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList

class PathWatcherTest : DescribeSpec({
  coroutineTestScope = true
  coroutineDebugProbes = true

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

      val result = mutableListOf<WatchResult>()
      folder.watchedEventFlow { true }.takeWhile { !it.reprocess }.toList(result)
      result shouldBe listOf(WatchResult(Paths.get("file2.yaml"), reprocess=false))
    }

    it("returns single file in subdirectory") {
      val folder = tempdir("tally-", keepOnFailure = false).toPath()
      (folder / "subfolder").createDirectory()
      (folder / "subfolder" / "file2.yaml").createFile()

      val result = mutableListOf<WatchResult>()
      folder.watchedEventFlow { true }.takeWhile { !it.reprocess }.toList(result)
      result shouldBe listOf(WatchResult(Paths.get("subfolder/file2.yaml"), reprocess=false))
    }
  }

    describe("emits modified") {


      it("returns single file in root") {
        val folder = tempdir("tally-", keepOnFailure = false).toPath()
        val targetFile = (folder / "file2.yaml").createFile()

        val result = mutableListOf<WatchResult>()
        val job = folder.watchedEventFlow { true }.onEach { result.add(it) }.launchIn(this)

        testCoroutineScheduler.runCurrent()
        targetFile.writeText("hello")
        testCoroutineScheduler.advanceUntilIdle()
        job.cancelAndJoin()
        testCoroutineScheduler.runCurrent()

        result shouldBe listOf(
          WatchResult(relativePath=Paths.get("file2.yaml"), reprocess=false),
          WatchResult(relativePath=null, reprocess=true),
          WatchResult(relativePath=Paths.get("file2.yaml"), reprocess=true)
        )
      }
    }
})
