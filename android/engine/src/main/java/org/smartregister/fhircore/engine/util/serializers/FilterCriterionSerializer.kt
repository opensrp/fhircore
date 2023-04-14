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

package org.smartregister.fhircore.engine.util.serializers

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.apache.commons.lang3.NotImplementedException
import org.hl7.fhir.r4.model.Enumerations.DataType
import org.smartregister.fhircore.engine.domain.model.FilterCriterionConfig

object FilterCriterionSerializer :
  JsonContentPolymorphicSerializer<FilterCriterionConfig>(FilterCriterionConfig::class) {

  private const val DATA_TYPE = "dataType"

  override fun selectDeserializer(
    element: JsonElement
  ): DeserializationStrategy<out FilterCriterionConfig> {
    val jsonObject = element.jsonObject
    val dataType = jsonObject[DATA_TYPE]?.jsonPrimitive?.content
    require(dataType != null && DataType.values().contains(DataType.valueOf(dataType))) {
      """`The dataType $dataType` property missing in the JSON .
         Supported types: ${DataType.values()}
         Parsed JSON: $jsonObject
          """.trimMargin()
    }
    return when (DataType.valueOf(dataType)) {
      DataType.QUANTITY -> FilterCriterionConfig.QuantityFilterCriterionConfig.serializer()
      DataType.DATETIME, DataType.DATE, DataType.TIME ->
        FilterCriterionConfig.DateFilterCriterionConfig.serializer()
      DataType.DECIMAL, DataType.INTEGER ->
        FilterCriterionConfig.NumberFilterCriterionConfig.serializer()
      DataType.STRING -> FilterCriterionConfig.StringFilterCriterionConfig.serializer()
      DataType.URI, DataType.URL -> FilterCriterionConfig.UriFilterCriterionConfig.serializer()
      DataType.REFERENCE -> FilterCriterionConfig.ReferenceFilterCriterionConfig.serializer()
      DataType.CODING, DataType.CODEABLECONCEPT, DataType.CODE ->
        FilterCriterionConfig.TokenFilterCriterionConfig.serializer()
      else -> {
        throw NotImplementedException("Data type `$dataType` is not supported for data filtering ")
      }
    }
  }
}
