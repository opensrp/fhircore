/*
 * Copyright 2021-2024 Ona Systems, Inc
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

package org.smartregister.fhircore.engine.util

import org.hl7.fhir.r4.formats.JsonParser
import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.DecimalType
import org.hl7.fhir.r4.model.Enumerations.DataType
import org.hl7.fhir.r4.model.IntegerType
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.model.TimeType
import org.hl7.fhir.r4.model.Type
import org.hl7.fhir.r4.model.UriType
import org.hl7.fhir.r4.utils.TypesUtilities
import timber.log.Timber

private val fhirTypesJsonParser: JsonParser = JsonParser()

private fun String.castToFhirPrimitiveType(type: DataType): Type? =
  when (type) {
    DataType.BOOLEAN -> BooleanType(this)
    DataType.DECIMAL -> DecimalType(this)
    DataType.INTEGER -> IntegerType(this)
    DataType.DATE -> DateType(this)
    DataType.DATETIME -> DateTimeType(this)
    DataType.TIME -> TimeType(this)
    DataType.STRING -> StringType(this)
    DataType.URI -> UriType(this)
    else -> null
  }

private fun String.castToFhirDataType(type: DataType): Type? =
  try {
    fhirTypesJsonParser.parseType(this, type.toCode())
  } catch (ex: Exception) {
    Timber.e("Error casting \"$this\" to FHIR type \"${type.toCode()}\"")
    throw ex
  }

/** Cast string value (including json string) to the FHIR {@link org.hl7.fhir.r4.model.Type} */
fun String.castToType(type: DataType): Type? {
  return if (TypesUtilities.isPrimitive(type.toCode())) {
    castToFhirPrimitiveType(type)
  } else {
    castToFhirDataType(type)
  }
}
