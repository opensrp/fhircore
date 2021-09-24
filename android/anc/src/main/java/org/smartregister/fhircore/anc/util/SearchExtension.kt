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

package org.smartregister.fhircore.anc.util

import ca.uhn.fhir.rest.gclient.ReferenceClientParam
import ca.uhn.fhir.rest.gclient.StringClientParam
import ca.uhn.fhir.rest.gclient.TokenClientParam
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.StringFilterModifier
import java.lang.UnsupportedOperationException
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.anc.sdk.QuestionnaireUtils.asCodeableConcept

fun Search.filterByPatient(reference: ReferenceClientParam, patientId: String) {
  filter(reference) { this.value = "${ResourceType.Patient.name}/$patientId" }
}

fun Search.filterByPatientName(name: String?) {
  if (name?.isNotBlank() == true) {
    filter(Patient.NAME) {
      value = name.trim()
      modifier = StringFilterModifier.CONTAINS
    }
  }
}

fun Search.filterBy(filter: SearchFilter) {
  when (filter.filterType) {
    Enumerations.SearchParamType.TOKEN -> filterToken(filter)
    Enumerations.SearchParamType.STRING ->
      filter(StringClientParam(filter.key)) {
        this.modifier = StringFilterModifier.MATCHES_EXACTLY
        this.value = filter.valueString!!
      }
    else ->
      throw UnsupportedOperationException("Can not apply ${filter.filterType} as search filter")
  }
}

fun Search.filterToken(filter: SearchFilter) {
  // TODO TokenFilter in SDK is not fully implemented and ignores all types but Coding
  when (filter.valueType) {
    Enumerations.DataType.CODING -> filter(TokenClientParam(filter.key), filter.valueCoding!!)
    Enumerations.DataType.CODEABLECONCEPT -> filter(TokenClientParam(filter.key), filter.valueCoding!!.asCodeableConcept()!!)
    else ->
      throw UnsupportedOperationException("SDK does not support value type ${filter.valueType}")
  }
}
