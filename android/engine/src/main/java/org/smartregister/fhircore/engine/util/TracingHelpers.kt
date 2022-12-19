package org.smartregister.fhircore.engine.util

object TracingHelpers {
  private val tracingQuestionnaires: List<String> = listOf("art-client-viral-load-test-results", "2", "3", "4")
  val tracingBundleId = "tracing"
  fun requireTracingTasks(id: String): Boolean = tracingQuestionnaires.firstOrNull { x -> x == id} != null
}