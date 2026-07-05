package io.kotest.provided

// For kotest runner use import com.diffplug.selfie.kotest.SelfieExtension // selfie-runner-kotest
import com.diffplug.selfie.junit5.SelfieExtension // selfie-runner-junit5
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
