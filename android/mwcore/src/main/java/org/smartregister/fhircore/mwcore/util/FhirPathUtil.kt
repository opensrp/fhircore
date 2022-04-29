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

package org.smartregister.fhircore.mwcore.util

import ca.uhn.fhir.context.FhirContext
import org.hl7.fhir.r4.hapi.fluentpath.FhirPathR4
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.StringType
import org.smartregister.fhircore.mwcore.configuration.view.Filter
import org.smartregister.fhircore.mwcore.configuration.view.isSimilar
import timber.log.Timber

object FhirPathUtil {
  val fhirPath = FhirPathR4(FhirContext.forR4Cached())

  fun Base.getPathValue(path: String) =
    fhirPath.evaluate(this, path, Base::class.java).firstOrNull()

  fun doesSatisfyFilter(resource: Resource, filter: Filter): Boolean? {
    if (filter.valueCoding == null && filter.valueString == null) return true

    // get property mentioned as filter and match value
    // e.g. category: CodeableConcept in Condition
    return resource
      .getNamedProperty(filter.key)
      .values
      .firstOrNull()
      ?.let {
        when (it) {
          // match relevant type and value
          is CodeableConcept -> filter.valueCoding!!.isSimilar(it)
          is Coding -> filter.valueCoding!!.isSimilar(it)
          is StringType -> it.value == filter.valueString
          else -> false
        }
      }
      .also {
        if (it == null)
          Timber.i("${resource.resourceType}, ${filter.key}: could not resolve key value filter")
      }
  }
}
