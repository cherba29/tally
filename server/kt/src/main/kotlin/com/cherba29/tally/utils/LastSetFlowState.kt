package com.cherba29.tally.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.lang.AutoCloseable

// Eagerly runs given flow is separate coroutine,
// providing access to last non-null state.
// If state is null then accessing it will block until it is set.
class LastSetFlowState<R : Any>(
  updates: Flow<R?>,
  scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
) : AutoCloseable {
  private val stateFlow = MutableStateFlow<R?>(null)
  private val watcherJob: Job = updates.filterNotNull().onEach { stateFlow.value = it }.launchIn(scope)

  // Will block if not yet set.
  suspend fun last(): R = stateFlow.filterNotNull().first()
  override fun close() = watcherJob.cancel()
}
