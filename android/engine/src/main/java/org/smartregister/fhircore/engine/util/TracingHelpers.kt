package org.smartregister.fhircore.engine.util

object TracingHelpers {
  private val tracingQuestionnaires: List<String> = listOf("art-client-viral-load-test-results", "phone-tracing-outcome", "art-client-welcome-service-high-or-detectable-viral-load", "art-client-viral-load-collection", "exposed-infant-convert-to-art-client")
  const val tracingBundleId = "tracing"
  fun requireTracingTasks(id: String): Boolean = tracingQuestionnaires.firstOrNull { x -> x == id} != null
}