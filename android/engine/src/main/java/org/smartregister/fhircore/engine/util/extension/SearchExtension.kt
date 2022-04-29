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

package org.smartregister.fhircore.engine.util.extension

import ca.uhn.fhir.rest.gclient.ReferenceClientParam
import ca.uhn.fhir.rest.gclient.TokenClientParam
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.StringFilterModifier
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.view.SearchFilter
import org.smartregister.fhircore.engine.configuration.view.asCoding

fun Search.filterByResourceTypeId(
  reference: ReferenceClientParam,
  resourceType: ResourceType,
  resourceId: String
) {
  filter(reference, { value = "${resourceType.name}/$resourceId" })
}

fun Search.filterByPatientName(name: String?) {
  if (name?.isNotBlank() == true) {
    filter(
      Patient.NAME,
      {
        modifier = StringFilterModifier.CONTAINS
        value = name.trim()
      }
    )
  }
}

fun Search.filterBy(filter: SearchFilter) {
  if (filter.valueCoding != null) filterToken(filter)
  else if (filter.valueReference != null) filterReference(filter)
}

fun Search.filterToken(filter: SearchFilter) {
  // TODO TokenFilter in SDK is not fully implemented and ignores all types but Coding
  filter(TokenClientParam(filter.key), { value = of(filter.valueCoding!!.asCoding()) })
}

fun Search.filterReference(filter: SearchFilter) {
  filter(ReferenceClientParam(filter.key), { value = filter.valueReference!!.referencePart() })
}
