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
 * either to use either 'AND' or 'OR' in the where clause of the Query.
 */
@Suppress("UNCHECKED_CAST")
fun Search.filterBy(dataQuery: DataQuery, configComputedRuleValues: Map<String, Any>) {
  val filterQueriesMap: Map<DataType, List<FilterCriterionConfig>> =
    dataQuery.filterCriteria.groupBy { it.dataType }
  filterQueriesMap.forEach { dataTypeListEntry ->
    when (dataTypeListEntry.key) {
      DataType.QUANTITY ->
        filterByQuantity(
          dataTypeListEntry.value as List<QuantityFilterCriterionConfig>,
          dataQuery,
          configComputedRuleValues
        )
      DataType.DATETIME, DataType.DATE, DataType.TIME ->
        filterByDateTime(
          dataTypeListEntry.value as List<DateFilterCriterionConfig>,
          dataQuery,
          configComputedRuleValues
        )
      DataType.DECIMAL, DataType.INTEGER ->
        filterByNumber(
          dataTypeListEntry.value as List<NumberFilterCriterionConfig>,
          dataQuery,
          configComputedRuleValues
        )
      DataType.STRING ->
        filterByString(
          dataTypeListEntry.value as List<StringFilterCriterionConfig>,
          dataQuery,
          configComputedRuleValues
        )
      DataType.URI, DataType.URL ->
        filterByUri(
          dataTypeListEntry.value as List<UriFilterCriterionConfig>,
          dataQuery,
          configComputedRuleValues
        )
      DataType.REFERENCE ->
        filterByReference(
          dataTypeListEntry.value as List<FilterCriterionConfig.ReferenceFilterCriterionConfig>,
          dataQuery,
          configComputedRuleValues
        )
      DataType.CODING, DataType.CODEABLECONCEPT, DataType.CODE ->
        filterByToken(
          dataTypeListEntry.value as List<TokenFilterCriterionConfig>,
          dataQuery,
          configComputedRuleValues
        )
      else -> {
        Timber.e("Search operation not supported for the given data type: ${dataTypeListEntry.key}")
      }
    }
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
        val value =
          interpolateDynamicValue(
            referenceFilterCriterionConfig.dynamicValue,
            configComputedRuleValues,
            referenceFilterCriterionConfig.value
          )
        if (value != null) this.value = referenceFilterCriterionConfig.value
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
        val value =
          interpolateDynamicValue(
            uriFilterCriterionConfig.dynamicValue,
            configComputedRuleValues,
            uriFilterCriterionConfig.value
          )
        if (value != null) this.value = value
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
        val value =
          interpolateDynamicValue(
            stringFilterCriterionConfig.dynamicValue,
            configComputedRuleValues,
            stringFilterCriterionConfig.value
          )
        if (value != null) this.value = value
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
      val value =
        interpolateDynamicValue(
          numberFilterCriterionConfig.dynamicValue,
          configComputedRuleValues,
          numberFilterCriterionConfig.value
        )
      val apply: NumberParamFilterCriterion.() -> Unit = {
        if (value != null) {
          this.prefix = numberFilterCriterionConfig.prefix
          this.value = value.toBigDecimal()
        }
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
        val valueDate =
          interpolateDynamicValue(
            dateFilterCriterionConfig.dynamicValue,
            configComputedRuleValues,
            dateFilterCriterionConfig.valueDate
          )
        val valueDateTime =
          interpolateDynamicValue(
            dateFilterCriterionConfig.dynamicValue,
            configComputedRuleValues,
            dateFilterCriterionConfig.valueDateTime
          )
        this.value =
          when {
            valueDate != null -> of(DateType(valueDate))
            valueDateTime != null -> of(DateTimeType(valueDateTime))
            else -> null
          }
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
        val value =
          interpolateDynamicValue(
            quantityFilterCriterionConfig.dynamicValue,
            configComputedRuleValues,
            quantityFilterCriterionConfig.value
          )
        if (value != null) {
          this.prefix = quantityFilterCriterionConfig.prefix
          this.value = value.toBigDecimal()
          this.system = quantityFilterCriterionConfig.system
          this.unit = quantityFilterCriterionConfig.unit
        }
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
        if (configuredCode?.code != null) {
          val tokenValue =
            interpolateDynamicValue(
              tokenFilterCriterionConfig.dynamicValue,
              configComputedRuleValues,
              configuredCode.code
            )
          if (tokenValue != null)
            value = of(Coding(configuredCode.system, tokenValue, configuredCode.display))
        }
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

private fun interpolateDynamicValue(
  dynamicValue: String?,
  configComputedRuleValues: Map<String, Any>,
  defaultValue: Any?
): String? {
  val value =
    dynamicValue?.interpolate(configComputedRuleValues) ?: defaultValue.let { it.toString() }
  return value.takeUnless { it.startsWith("@{") && it.endsWith("}") }
}
