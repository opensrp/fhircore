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
import ca.uhn.fhir.rest.gclient.NumberClientParam
import ca.uhn.fhir.rest.gclient.QuantityClientParam
import ca.uhn.fhir.rest.gclient.ReferenceClientParam
import ca.uhn.fhir.rest.gclient.StringClientParam
import ca.uhn.fhir.rest.gclient.TokenClientParam
import ca.uhn.fhir.rest.gclient.UriClientParam
import com.google.android.fhir.search.Search
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.domain.model.DataQuery
import org.smartregister.fhircore.engine.domain.model.FilterCriterionConfig

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

fun Search.filterBy(dataQuery: DataQuery) {
  dataQuery.filterCriteria.forEach { filterCriterion: FilterCriterionConfig ->
    when (filterCriterion) {
      is FilterCriterionConfig.DateFilterCriterionConfig ->
        filter(
          dateParameter = DateClientParam(dataQuery.paramName),
          {
            this.prefix = filterCriterion.prefix
            this.value =
              when {
                filterCriterion.valueDate != null -> of(DateType(filterCriterion.valueDate))
                filterCriterion.valueDateTime != null ->
                  of(DateTimeType(filterCriterion.valueDateTime))
                else -> null
              }
          },
          operation = dataQuery.operation,
        )
      is FilterCriterionConfig.NumberFilterCriterionConfig ->
        filter(
          numberParameter = NumberClientParam(dataQuery.paramName),
          {
            this.prefix = filterCriterion.prefix
            this.value = filterCriterion.value
          },
          operation = dataQuery.operation
        )
      is FilterCriterionConfig.QuantityFilterCriterionConfig ->
        filter(
          quantityParameter = QuantityClientParam(dataQuery.paramName),
          {
            this.prefix = filterCriterion.prefix
            this.value = filterCriterion.value
            this.system = filterCriterion.system
            this.unit = filterCriterion.unit
          },
          operation = dataQuery.operation
        )
      is FilterCriterionConfig.ReferenceFilterCriterionConfig ->
        filter(
          referenceParameter = ReferenceClientParam(dataQuery.paramName),
          { this.value = filterCriterion.value },
          operation = dataQuery.operation
        )
      is FilterCriterionConfig.StringFilterCriterionConfig ->
        filter(
          stringParameter = StringClientParam(dataQuery.paramName),
          {
            this.value = filterCriterion.value
            this.modifier = filterCriterion.modifier
          },
          operation = dataQuery.operation
        )
      is FilterCriterionConfig.TokenFilterCriterionConfig ->
        filter(
          tokenParameter = TokenClientParam(dataQuery.paramName),
          {
            val configuredCode = filterCriterion.value
            if (configuredCode?.code != null) {
              this.value =
                of(Coding(configuredCode.system, configuredCode.code, configuredCode.display))
            }
          },
          operation = dataQuery.operation
        )
      is FilterCriterionConfig.UriFilterCriterionConfig ->
        filter(
          uriParam = UriClientParam(dataQuery.paramName),
          { this.value = filterCriterion.value },
          operation = dataQuery.operation
        )
    }
  }
}
