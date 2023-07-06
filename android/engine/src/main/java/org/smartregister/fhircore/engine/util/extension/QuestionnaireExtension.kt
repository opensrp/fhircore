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

import com.google.android.fhir.datacapture.extensions.asStringValue
import com.google.android.fhir.datacapture.extensions.targetStructureMap
import com.google.android.fhir.logicalId
import java.util.Locale
import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.DecimalType
import org.hl7.fhir.r4.model.Enumerations.DataType
import org.hl7.fhir.r4.model.Expression
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.IntegerType
import org.hl7.fhir.r4.model.Quantity
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.model.TimeType
import org.hl7.fhir.r4.model.Type
import org.hl7.fhir.r4.model.UriType
import org.smartregister.fhircore.engine.domain.model.ActionParameter

fun QuestionnaireResponse.QuestionnaireResponseItemComponent.asLabel() =
  if (this.linkId != null) {
    this.linkId
      .replace("_", " ")
      .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString() }
      .plus(": ")
  } else ""

fun Questionnaire.isExtractionCandidate() =
  this.targetStructureMap != null ||
    this.extension.any { it.url.contains("sdc-questionnaire-itemExtractionContext") }

fun Questionnaire.cqfLibraryIds() =
  this.extension.filter { it.url.contains("cqf-library") }.mapNotNull {
    it.value?.asStringValue()?.replace("Library/", "")
  }

fun QuestionnaireResponse.findSubject(bundle: Bundle?) =
  IdType(this.subject.reference).let { subject ->
    bundle?.entry?.find { it.resource.logicalId == subject.idPart }?.resource
  }

fun Questionnaire.find(linkId: String): Questionnaire.QuestionnaireItemComponent? {
  val result = mutableListOf<Questionnaire.QuestionnaireItemComponent>()
  item.find(FieldType.LINK_ID, linkId, result)
  return result.firstOrNull()
}

fun QuestionnaireResponse.find(
  linkId: String
): QuestionnaireResponse.QuestionnaireResponseItemComponent? {
  return item.find(linkId, null)
}

fun List<QuestionnaireResponse.QuestionnaireResponseItemComponent>.find(
  linkId: String,
  default: QuestionnaireResponse.QuestionnaireResponseItemComponent?
): QuestionnaireResponse.QuestionnaireResponseItemComponent? {
  var result = default
  run loop@{
    forEach {
      if (it.linkId == linkId) {
        result = it
        return@loop
      } else if (it.item.isNotEmpty()) {
        result = it.item.find(linkId, result)
      } else if (it.hasAnswer()) {
        it.answer.forEach { result = it.item.find(linkId, result) }
      }
    }
  }

  return result
}

enum class FieldType {
  DEFINITION,
  LINK_ID,
  TYPE
}

fun Questionnaire.find(
  fieldType: FieldType,
  value: String
): List<Questionnaire.QuestionnaireItemComponent> {
  val result = mutableListOf<Questionnaire.QuestionnaireItemComponent>()
  item.find(fieldType, value, result)
  return result
}

fun List<Questionnaire.QuestionnaireItemComponent>.find(
  fieldType: FieldType,
  value: String,
  target: MutableList<Questionnaire.QuestionnaireItemComponent>
) {
  forEach {
    when (fieldType) {
      FieldType.DEFINITION -> {
        if (it.definition?.contentEquals(value, true) == true) {
          target.add(it)
        }
      }
      FieldType.LINK_ID -> {
        if (it.linkId == value) {
          target.add(it)
        }
      }
      FieldType.TYPE -> {
        if (it.type == Questionnaire.QuestionnaireItemType.valueOf(value)) target.add(it)
      }
    }

    if (it.item.isNotEmpty()) {
      it.item.find(fieldType, value, target)
    }
  }
}

val Questionnaire.QuestionnaireItemComponent.initialExpression: Expression?
  get() =
    this.getExtensionByUrl(EXTENSION_INITIAL_EXPRESSION_URL)?.let { it.castToExpression(it.value) }

const val EXTENSION_INITIAL_EXPRESSION_URL: String =
  "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-initialExpression"

/** Pre-Populate Questionnaire items with initial values */
// TODO: handle interpolation for null values on rules engine and not where the values are used
fun List<Questionnaire.QuestionnaireItemComponent>.prePopulateInitialValues(
  interpolationPrefix: String,
  prePopulationParams: List<ActionParameter>
) {
  forEach { item ->
    prePopulationParams
      .firstOrNull {
        it.linkId == item.linkId &&
          !it.value.isNullOrEmpty() &&
          !it.value.contains(interpolationPrefix)
      }
      ?.let { actionParam ->
        /**
         * Removes the initialExpression when the same linkId will be populated by config
         * pre-populate.
         */
        if (item.hasExtension(EXTENSION_INITIAL_EXPRESSION_URL)) {
          item.removeExtension(EXTENSION_INITIAL_EXPRESSION_URL)
        }
        item.initial =
          arrayListOf(
            Questionnaire.QuestionnaireItemInitialComponent().apply {
              value = actionParam.dataType?.let { actionParam.value.castToType(it) }
            }
          )
      }
    if (item.item.isNotEmpty()) {
      item.item.prePopulateInitialValues(interpolationPrefix, prePopulationParams)
    }
  }
}

/** Cast string value (including json string) to the FHIR {@link org.hl7.fhir.r4.model.Type} */
fun String.castToType(type: DataType): Type? {
  return when (type) {
    DataType.BOOLEAN -> BooleanType(this)
    DataType.DECIMAL -> DecimalType(this)
    DataType.INTEGER -> IntegerType(this)
    DataType.DATE -> DateType(this)
    DataType.DATETIME -> DateTimeType(this)
    DataType.TIME -> TimeType(this)
    DataType.STRING -> StringType(this)
    DataType.URI -> UriType(this)
    DataType.CODING -> this.tryDecodeJson<Coding>()
    DataType.QUANTITY -> this.tryDecodeJson<Quantity>()
    else -> null /*TODO cast the (several) remaining Enumeration.DataTypes*/
  }
}
