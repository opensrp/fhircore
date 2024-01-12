package org.smartregister.fhircore.quest.sdk

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

internal fun <T> runBlockingOnWorkerThread(block: suspend (CoroutineScope) -> T) =
  runBlocking(Dispatchers.IO) { block(this) }
