package com.cherba29.tally.utils

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.engine.coroutines.testScheduler
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

class LastSetFlowStateTest : DescribeSpec({
  coroutineTestScope = true

  describe("provides latest") {
    it("empty") {
      val flow = MutableStateFlow<Int?>(null)
      LastSetFlowState(flow, this).use { state ->
        // Blocks if value is not set.
        withTimeoutOrNull(200.milliseconds) { state.last() } shouldBe null
        flow.emit(1)
        // Now succeeds without block.
        state.last() shouldBe 1
      }
    }

    it("single") {
      LastSetFlowState(flowOf(2), this).use { state ->
        state.last() shouldBe 2
      }
    }
    it("has latest") {
      val flow = MutableStateFlow<Int?>(2)

      LastSetFlowState(flow, this).use { state ->
        state.last() shouldBe 2
        flow.emit(3)
        testScheduler.runCurrent()
        state.last() shouldBe 3
        flow.emit(4)
        flow.emit(5)
        testScheduler.runCurrent()
        state.last() shouldBe 5
        flow.emit(6)
        state.last() shouldBe 5  // Still 5 since flow did not get chance to run yet.
      }
    }
  }
  describe("close") {
    it("stops updates to latest") {
      val flow = MutableStateFlow<Int?>(2)

      val state = LastSetFlowState(flow, this)
      state.last() shouldBe 2
      flow.emit(3)
      testScheduler.runCurrent()
      state.last() shouldBe 3  // Updates last when flow updates.
      state.close()
      testScheduler.runCurrent()  // Now flow should get closed/canceled.
      flow.emit(4)
      testScheduler.runCurrent()
      state.last() shouldBe 3  // Still 3 now that flow was closed it no longer updates.
    }
  }
})