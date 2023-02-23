/*
 * Copyright 2021 Ona Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.smartregister.fhircore.engine.util

object TracingHelpers {
  private val tracingQuestionnaires: List<String> =
    listOf(
      "art-client-viral-load-test-results",
      "phone-tracing-outcome",
      "home-tracing-outcome",
      "art-client-welcome-service-high-or-detectable-viral-load",
      "art-client-viral-load-collection",
      "exposed-infant-convert-to-art-client",
      "patient-finish-visit",
      "contact-and-community-positive-hiv-test-and-next-appointment"
    )
  const val tracingBundleId = "tracing"
  fun requireTracingTasks(id: String): Boolean =
    tracingQuestionnaires.firstOrNull { x -> x == id } != null
}
