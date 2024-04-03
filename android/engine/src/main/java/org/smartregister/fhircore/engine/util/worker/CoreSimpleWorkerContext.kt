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

package org.smartregister.fhircore.engine.util.worker

import ca.uhn.fhir.context.FhirContext
import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.hl7.fhir.r4.hapi.ctx.HapiWorkerContext
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.Resource

class CoreSimpleWorkerContext : SimpleWorkerContext() {
  private val hapi =
    HapiWorkerContext(
      FhirContext.forR4Cached(),
      FhirContext.forR4Cached().validationSupport,
    )

  init {
    setExpansionProfile(Parameters())
    isCanRunWithoutTerminology = true
  }

  override fun <T : Resource?> fetchResourceWithException(theClass: Class<T>?, uri: String?): T? {
    if (uri == null) {
      return null
    }
    if (
      uri.startsWith("https://terminology.hl7.org") ||
        uri.startsWith("http://snomed.info/sct") ||
        uri.startsWith(
          "https://d-tree.org",
        )
    ) {
      return null
    }
    return hapi.fetchResourceWithException(theClass, uri)
      ?: super.fetchResourceWithException(theClass, uri)
  }
}
