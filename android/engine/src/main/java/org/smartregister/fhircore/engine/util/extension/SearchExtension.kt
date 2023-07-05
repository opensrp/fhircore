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
import com.google.android.fhir.search.filter.DateParamFilterCriterion
import com.google.android.fhir.search.filter.NumberParamFilterCriterion
import com.google.android.fhir.search.filter.QuantityParamFilterCriterion
import com.google.android.fhir.search.filter.ReferenceParamFilterCriterion
import com.google.android.fhir.search.filter.StringParamFilterCriterion
import com.google.android.fhir.search.filter.TokenParamFilterCriterion
import com.google.android.fhir.search.filter.UriParamFilterCriterion
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.Enumerations.DataType
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.domain.model.DataQuery
import org.smartregister.fhircore.engine.domain.model.FilterCriterionConfig
import org.smartregister.fhircore.engine.domain.model.FilterCriterionConfig.DateFilterCriterionConfig
import org.smartregister.fhircore.engine.domain.model.FilterCriterionConfig.NumberFilterCriterionConfig
import org.smartregister.fhircore.engine.domain.model.FilterCriterionConfig.QuantityFilterCriterionConfig
import org.smartregister.fhircore.engine.domain.model.FilterCriterionConfig.StringFilterCriterionConfig
import org.smartregister.fhircore.engine.domain.model.FilterCriterionConfig.TokenFilterCriterionConfig
import org.smartregister.fhircore.engine.domain.model.FilterCriterionConfig.UriFilterCriterionConfig
import timber.log.Timber

/**
 * This extension function is used to configure [DataQuery] s against the [Search] DSL. This
 * extension covers all queries for for the supported [DataType] s. Filters of the same [DataType]
 * are grouped together in a query and a configured [DataQuery.operation] is used to determine
 * either to use either 'AND' or 'OR' in the where clause of the Query. Optional
 * [configComputedRuleValues] is provided to substitute [FilterCriterionConfig.computedRule]
 * placeholders with actual values.
 */
@Suppress("UNCHECKED_CAST")
fun Search.filterBy(dataQuery: DataQuery, configComputedRuleValues: Map<String, Any>) {
  val filterQueriesMap: Map<DataType, List<FilterCriterionConfig>> =
    dataQuery.filterCriteria.groupBy { it.dataType }
  filterQueriesMap.forEach { dataTypeListEntry ->
    when (dataTypeListEntry.key) {
      DataType.QUANTITY ->
        filterByQuantity(
          quantityFilterCriterionConfigs =
            dataTypeListEntry.value as List<QuantityFilterCriterionConfig>,
          dataQuery = dataQuery,
          configComputedRuleValues = configComputedRuleValues
        )
      DataType.DATETIME, DataType.DATE, DataType.TIME ->
        filterByDateTime(
          dateFilterCriterionConfigs = dataTypeListEntry.value as List<DateFilterCriterionConfig>,
          dataQuery = dataQuery,
          configComputedRuleValues = configComputedRuleValues
        )
      DataType.DECIMAL, DataType.INTEGER ->
        filterByNumber(
          numberFilterCriterionConfigs =
            dataTypeListEntry.value as List<NumberFilterCriterionConfig>,
          dataQuery = dataQuery,
          configComputedRuleValues = configComputedRuleValues
        )
      DataType.STRING ->
        filterByString(
          stringFilterCriterionConfigs =
            dataTypeListEntry.value as List<StringFilterCriterionConfig>,
          dataQuery = dataQuery,
          configComputedRuleValues = configComputedRuleValues
        )
      DataType.URI, DataType.URL ->
        filterByUri(
          uriFilterCriterionConfigs = dataTypeListEntry.value as List<UriFilterCriterionConfig>,
          dataQuery = dataQuery,
          configComputedRuleValues = configComputedRuleValues
        )
      DataType.REFERENCE ->
        filterByReference(
          referenceFilterCriterionConfigs =
            dataTypeListEntry.value as List<FilterCriterionConfig.ReferenceFilterCriterionConfig>,
          dataQuery = dataQuery,
          configComputedRuleValues = configComputedRuleValues
        )
      DataType.CODING, DataType.CODEABLECONCEPT, DataType.CODE ->
        filterByToken(
          tokenFilterCriterionConfigs = dataTypeListEntry.value as List<TokenFilterCriterionConfig>,
          dataQuery = dataQuery,
          configComputedRuleValues = configComputedRuleValues
        )
      else -> {
        Timber.e("Search operation not supported for the given data type: ${dataTypeListEntry.key}")
      }
    }
  }
}

@Suppress("UNCHECKED_CAST")
private fun <V : Any> retrieveComputedRuleValue(
  key: String?,
  configComputedRuleValues: Map<String, Any>
): V? {
  return if (key.isNullOrEmpty()) {
    Timber.e("Key not provided")
    null
  } else {
    configComputedRuleValues[key] as V?
  }
}

private fun Search.filterByReference(
  referenceFilterCriterionConfigs: List<FilterCriterionConfig.ReferenceFilterCriterionConfig>,
  dataQuery: DataQuery,
  configComputedRuleValues: Map<String, Any>
) {
  val filters =
    referenceFilterCriterionConfigs.map { referenceFilterCriterionConfig ->
      val apply: ReferenceParamFilterCriterion.() -> Unit = {
        this.value =
          referenceFilterCriterionConfig.value
            ?: retrieveComputedRuleValue(
              key = referenceFilterCriterionConfig.computedRule,
              configComputedRuleValues = configComputedRuleValues
            )
      }
      apply
    }
  filter(
    referenceParameter = ReferenceClientParam(dataQuery.paramName),
    init = filters.toTypedArray(),
    operation = dataQuery.operation
  )
}

private fun Search.filterByUri(
  uriFilterCriterionConfigs: List<UriFilterCriterionConfig>,
  dataQuery: DataQuery,
  configComputedRuleValues: Map<String, Any>
) {
  val filters =
    uriFilterCriterionConfigs.map { uriFilterCriterionConfig ->
      val apply: UriParamFilterCriterion.() -> Unit = {
        this.value =
          uriFilterCriterionConfig.value
            ?: retrieveComputedRuleValue(
              key = uriFilterCriterionConfig.computedRule,
              configComputedRuleValues = configComputedRuleValues
            )
      }
      apply
    }
  filter(
    uriParam = UriClientParam(dataQuery.paramName),
    init = filters.toTypedArray(),
    operation = dataQuery.operation
  )
}

private fun Search.filterByString(
  stringFilterCriterionConfigs: List<StringFilterCriterionConfig>,
  dataQuery: DataQuery,
  configComputedRuleValues: Map<String, Any>
) {
  val filters =
    stringFilterCriterionConfigs.map { stringFilterCriterionConfig ->
      val apply: StringParamFilterCriterion.() -> Unit = {
        this.value =
          stringFilterCriterionConfig.value
            ?: retrieveComputedRuleValue(
              key = stringFilterCriterionConfig.computedRule,
              configComputedRuleValues = configComputedRuleValues
            )

        this.modifier = stringFilterCriterionConfig.modifier
      }
      apply
    }
  filter(
    stringParameter = StringClientParam(dataQuery.paramName),
    init = filters.toTypedArray(),
    operation = dataQuery.operation
  )
}

private fun Search.filterByNumber(
  numberFilterCriterionConfigs: List<NumberFilterCriterionConfig>,
  dataQuery: DataQuery,
  configComputedRuleValues: Map<String, Any>
) {
  val filters =
    numberFilterCriterionConfigs.map { numberFilterCriterionConfig ->
      val apply: NumberParamFilterCriterion.() -> Unit = {
        this.prefix = numberFilterCriterionConfig.prefix
        this.value =
          numberFilterCriterionConfig.value
            ?: retrieveComputedRuleValue(
              key = numberFilterCriterionConfig.computedRule,
              configComputedRuleValues = configComputedRuleValues
            )
      }
      apply
    }
  filter(
    numberParameter = NumberClientParam(dataQuery.paramName),
    init = filters.toTypedArray(),
    operation = dataQuery.operation
  )
}

private fun Search.filterByDateTime(
  dateFilterCriterionConfigs: List<DateFilterCriterionConfig>,
  dataQuery: DataQuery,
  configComputedRuleValues: Map<String, Any>
) {
  val filters =
    dateFilterCriterionConfigs.map { dateFilterCriterionConfig ->
      val apply: DateParamFilterCriterion.() -> Unit = {
        this.prefix = dateFilterCriterionConfig.prefix
        val computedRuleValue =
          dateFilterCriterionConfig.value
            ?: retrieveComputedRuleValue(
              key = dateFilterCriterionConfig.computedRule,
              configComputedRuleValues = configComputedRuleValues
            )

        this.value =
          if (dateFilterCriterionConfig.valueAsDateTime) of(DateTimeType(computedRuleValue))
          else of(DateType(computedRuleValue))
      }
      apply
    }
  filter(
    dateParameter = DateClientParam(dataQuery.paramName),
    init = filters.toTypedArray(),
    operation = dataQuery.operation,
  )
}

private fun Search.filterByQuantity(
  quantityFilterCriterionConfigs: List<QuantityFilterCriterionConfig>,
  dataQuery: DataQuery,
  configComputedRuleValues: Map<String, Any>
) {
  val filters =
    quantityFilterCriterionConfigs.map { quantityFilterCriterionConfig ->
      val apply: QuantityParamFilterCriterion.() -> Unit = {
        this.prefix = quantityFilterCriterionConfig.prefix
        this.value =
          quantityFilterCriterionConfig.value
            ?: retrieveComputedRuleValue(
              quantityFilterCriterionConfig.computedRule,
              configComputedRuleValues,
            )

        this.system = quantityFilterCriterionConfig.system
        this.unit = quantityFilterCriterionConfig.unit
      }
      apply
    }
  filter(
    quantityParameter = QuantityClientParam(dataQuery.paramName),
    init = filters.toTypedArray(),
    operation = dataQuery.operation
  )
}

private fun Search.filterByToken(
  tokenFilterCriterionConfigs: List<TokenFilterCriterionConfig>,
  dataQuery: DataQuery,
  configComputedRuleValues: Map<String, Any>
) {
  val filters =
    tokenFilterCriterionConfigs.map { tokenFilterCriterionConfig ->
      val configuredCode = tokenFilterCriterionConfig.value
      val apply: TokenParamFilterCriterion.() -> Unit = {
        val coding =
          if (configuredCode?.code != null)
            Coding(configuredCode.system, configuredCode.code, configuredCode.display)
          else
            retrieveComputedRuleValue(
              tokenFilterCriterionConfig.computedRule,
              configComputedRuleValues
            )
        value = coding?.let { of(it) }
      }
      apply
    }
  filter(
    tokenParameter = TokenClientParam(dataQuery.paramName),
    init = filters.toTypedArray(),
    operation = dataQuery.operation
  )
}

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
