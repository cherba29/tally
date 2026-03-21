package io.kotest.provided

import com.diffplug.selfie.junit5.SelfieExtension // selfie-runner-junit5
//import com.diffplug.selfie.kotest.SelfieExtension // selfie-runner-kotest
import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.spec.SpecExecutionOrder
import kotlin.time.Duration.Companion.seconds


class ProjectConfig : AbstractProjectConfig() {
  // Order specs alphabetically
  override val specExecutionOrder = SpecExecutionOrder.Lexicographic


  override val extensions = listOf(SelfieExtension(this))


  override val timeout = 5.seconds
  // Global setup
  override suspend fun beforeProject() {
    println("Starting test suite...")
  }
}
