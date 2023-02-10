/*
 * Copyright 2021-2023 Ona Systems, Inc
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

import ca.uhn.fhir.rest.gclient.DateClientParam
import ca.uhn.fhir.rest.gclient.ReferenceClientParam
import ca.uhn.fhir.rest.gclient.StringClientParam
import ca.uhn.fhir.rest.gclient.TokenClientParam
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.StringFilterModifier
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.domain.model.Code
import org.smartregister.fhircore.engine.domain.model.DataQuery

fun Search.filterByResourceTypeId(
  reference: ReferenceClientParam,
  resourceType: ResourceType,
  resourceId: String
) {
  filter(reference, { value = "${resourceType.name}/$resourceId" })
}

fun Search.filterByResourceTypeId(
  token: TokenClientParam,
  resourceType: ResourceType,
  resourceId: String
) {
  filter(token, { value = of("${resourceType.name}/$resourceId") })
}

fun Search.filterBy(filter: DataQuery) {
  when (filter.filterType) {
    Enumerations.SearchParamType.TOKEN -> filterToken(filter)
    Enumerations.SearchParamType.STRING -> filterString(filter)
    Enumerations.SearchParamType.DATE -> filterDate(filter)
    else ->
      throw UnsupportedOperationException("Can not apply ${filter.filterType} as search filter")
  }
}

fun Search.filterToken(dataQuery: DataQuery) {
  // TODO TokenFilter in SDK is not fully implemented and ignores all types but Coding
  when (dataQuery.valueType) {
    Enumerations.DataType.CODING ->
      filter(TokenClientParam(dataQuery.key), { value = of(dataQuery.valueCoding!!.asCoding()) })
    Enumerations.DataType.CODEABLECONCEPT ->
      filter(
        TokenClientParam(dataQuery.key),
        { value = of(dataQuery.valueCoding!!.asCodeableConcept()) }
      )
    else ->
      throw UnsupportedOperationException("SDK does not support value type ${dataQuery.valueType}")
  }
}

fun Search.filterString(dataQuery: DataQuery) {
  // TODO StringFilter in SDK is not fully implemented and ignores all types but String and Boolean
  when (dataQuery.valueType) {
    Enumerations.DataType.STRING ->
      filter(
        StringClientParam(dataQuery.key),
        {
          this.modifier = StringFilterModifier.MATCHES_EXACTLY
          this.value = dataQuery.valueString!!
        }
      )
    Enumerations.DataType.BOOLEAN ->
      filter(
        StringClientParam(dataQuery.key),
        {
          this.modifier = StringFilterModifier.MATCHES_EXACTLY
          this.value = dataQuery.valueBoolean.toString()
        }
      )
    else ->
      throw UnsupportedOperationException("SDK does not support value type ${dataQuery.valueType}")
  }
}

fun Search.filterDate(dataQuery: DataQuery) {
  when (dataQuery.valueType) {
    Enumerations.DataType.DATE ->
      filter(
        DateClientParam(dataQuery.key),
        {
          this.prefix = dataQuery.paramPrefix
          this.value = of(DateType(dataQuery.valueDate))
        }
      )
    else ->
      throw UnsupportedOperationException("SDK does not support value type ${dataQuery.valueType}")
  }
}

fun Code.asCoding() = Coding(this.system, this.code, this.display)

fun Code.asCodeableConcept() =
  CodeableConcept().apply {
    addCoding(this@asCodeableConcept.asCoding())
    text = this@asCodeableConcept.display
  }
