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
import ca.uhn.fhir.rest.gclient.StringClientParam
import ca.uhn.fhir.rest.gclient.TokenClientParam
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.StringFilterModifier
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.view.SearchFilter
import org.smartregister.fhircore.engine.configuration.view.asCodeableConcept
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
  when (filter.filterType) {
    Enumerations.SearchParamType.TOKEN -> filterToken(filter)
    Enumerations.SearchParamType.STRING ->
      filter(
        StringClientParam(filter.key),
        {
          this.modifier = StringFilterModifier.MATCHES_EXACTLY
          this.value = filter.valueString!!
        }
      )
    else ->
      throw UnsupportedOperationException("Can not apply ${filter.filterType} as search filter")
  }
}

fun Search.filterToken(filter: SearchFilter) {
  // TODO TokenFilter currently only supports Coding, CodeableConcept and String
  when (filter.valueType) {
    Enumerations.DataType.CODING ->
      filter(TokenClientParam(filter.key), { value = of(filter.valueCoding!!.asCoding()) })
    Enumerations.DataType.CODEABLECONCEPT ->
      filter(TokenClientParam(filter.key), { value = of(filter.valueCoding!!.asCodeableConcept()) })
    Enumerations.DataType.STRING ->
      filter(TokenClientParam(filter.key), { value = of(filter.valueString!!) })
    else ->
      throw UnsupportedOperationException("SDK does not support value type ${filter.valueType}")
  }
}
