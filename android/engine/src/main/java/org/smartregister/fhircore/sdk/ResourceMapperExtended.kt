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

package org.smartregister.fhircore.sdk

import com.google.android.fhir.datacapture.mapping.ResourceMapper
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
import org.apache.commons.lang3.ClassUtils
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.engine.util.extension.FieldType
import org.smartregister.fhircore.engine.util.extension.find
import timber.log.Timber

object ResourceMapperExtended {
  val DEFINITION_PATIENT_EXTENSION =
    "http://hl7.org/fhir/StructureDefinition/Patient#Patient.extension"
  val CHOICE_ELEMENT_CONSTANT_NAME = "value"

  // TODO SDK Does not handle inherited properties mapping and hence extension can not be directly
  // mapped into questionnaire
  // https://github.com/google/android-fhir/issues/943
  suspend fun handleMissingSdkFunctionalityExtension(
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse,
    bundle: Bundle
  ) {
    kotlin
      .runCatching {
        bundle.entry.single { it.resource is Patient }.run {
          questionnaire.find(FieldType.DEFINITION, DEFINITION_PATIENT_EXTENSION).forEach { qit ->
            questionnaireResponse.find(qit.linkId)!!.also {
              this@run.resource.extractField(bundle, qit, it)
            }
          }
        }
      }
      .onFailure { Timber.w(it) }
  }

  private suspend fun Base.extractFields(
    bundle: Bundle,
    questionnaireItemList: List<Questionnaire.QuestionnaireItemComponent>,
    questionnaireResponseItemList: List<QuestionnaireResponse.QuestionnaireResponseItemComponent>
  ) {
    questionnaireItemList.forEach { qit ->
      questionnaireResponseItemList.find(qit.linkId, null)?.let { qrit ->
        extractField(bundle, qit, qrit)
      }
    }
  }

  private suspend fun Base.extractField(
    bundle: Bundle,
    questionnaireItem: Questionnaire.QuestionnaireItemComponent,
    questionnaireResponseItem: QuestionnaireResponse.QuestionnaireResponseItemComponent
  ) {
    if (questionnaireItem.type == Questionnaire.QuestionnaireItemType.GROUP) {
      if (questionnaireItem.definition == null) {
        this.extractFields(bundle, questionnaireItem.item, questionnaireResponseItem.item)
        return
      }
      val groupBase = invokeResourceMapperExtension(questionnaireItem, "createBase") as Base

      val definitionField = questionnaireItem.getDefinitionField ?: return

      // isChoiceElement filter not applicable for extension hence skipping check

      val value: Base =
        (definitionField.nonParameterizedType.newInstance() as Base).apply {
          this.extractFields(bundle, questionnaireItem.item, questionnaireResponseItem.item)
        }

      if (isList(definitionField)) updateListField(this, definitionField, value)
      else invokeResourceMapperExtension(this, "updateField", definitionField, value)
      return
    }
    if (questionnaireResponseItem.answer.isEmpty()) return

    val definitionField = questionnaireItem.getDefinitionField ?: return
    if (definitionField.nonParameterizedType.isEnum) {
      invokeResourceMapperExtension(
        this,
        "updateFieldWithEnum",
        definitionField,
        questionnaireResponseItem.answer.first().value
      )
    } else {
      invokeResourceMapperExtension(
        this,
        "updateField",
        definitionField,
        questionnaireResponseItem.answer
      )
    }
  }

  private fun invokeResourceMapperExtension(onObj: Any, method: String, vararg args: Any): Any? {
    return Class.forName("${ResourceMapper.javaClass.name}Kt")
      .declaredMethods
      .single {
        it.name == method &&
          kotlin.run {
            // First param of an extension method is always the class on which it is invoked
            val required = arrayOf(onObj::class.java, *(args.map { it::class.java }.toTypedArray()))
            val matching =
              it.parameterTypes.filterIndexed { i, c ->
                ClassUtils.isAssignable(c, required[i]) || c.isAssignableFrom(required[i])
              }
            matching.size == required.size
          }
      }
      .also { it.isAccessible = true }
      // First param of an extension method is always the class on which it is invoked
      .invoke(onObj, onObj, *args)
  }

  // copied and simplified from SDK ResourceMapper#L512
  private val Questionnaire.QuestionnaireItemComponent.getDefinitionField: Field?
    get() {
      val path = definition.substringAfter('#', "").split(".")
      if (path.size < 2) return null
      val resourceClass: Class<*> = Class.forName("org.hl7.fhir.r4.model.${path[0]}")

      val definitionField: Field =
        if (isChoiceElement(this)) {
          resourceClass.getFieldOrNull(CHOICE_ELEMENT_CONSTANT_NAME)
        } else {
          resourceClass.getFieldOrNull(path[1])
        }
          ?: return null

      // isChoiceElement filter not applicable for extension hence skipping check

      return path.drop(2).fold(definitionField) { field: Field?, nestedFieldName: String ->
        field?.nonParameterizedType?.getFieldOrNull(nestedFieldName)
      }
    }

  fun isChoiceElement(item: Questionnaire.QuestionnaireItemComponent) =
    invokeResourceMapperExtension(item, "isChoiceElement", 1) as Boolean

  fun isList(field: Field) = invokeResourceMapperExtension(field, "isList") as Boolean

  // copied and modified fro list from SDK ResourceMapper#L397
  private fun updateListField(base: Base, field: Field, answerValue: Base) {
    base
      .javaClass
      .getMethod("add${field.name.capitalize()}", answerValue::class.java)
      .invoke(base, answerValue)
  }

  // copied from SDK ResourceMapper#L610
  private val Field.nonParameterizedType: Class<*>
    get() =
      if (genericType is ParameterizedType)
        (genericType as ParameterizedType).actualTypeArguments[0] as Class<*>
      else type

  // copied and modified from SDK ResourceMapper#L512
  private fun Class<*>.getFieldOrNull(name: String): Field? {
    return try {
      getDeclaredField(name)
    } catch (ex: NoSuchFieldException) {
      superclass?.let {
        return it.getFieldOrNull(name)
      }
    }
  }
}
