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

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.datacapture.common.datatype.asStringValue
import com.google.android.fhir.logicalId
import java.util.Date
import java.util.UUID
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.BaseDateTimeType
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Expression
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.PrimitiveType
import org.hl7.fhir.r4.model.Quantity
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.model.Type
import org.json.JSONException
import org.json.JSONObject
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.ui.questionnaire.FhirCoreQuestionnaireFragment
import timber.log.Timber

fun Type?.valueToString(): String {
  return if (this == null) ""
  else if (this.isDateTime) (this as BaseDateTimeType).value.makeItReadable()
  else if (this.isPrimitive) (this as PrimitiveType<*>).asStringValue()
  else if (this is Coding) this.display ?: code
  else if (this is CodeableConcept) this.stringValue()
  else if (this is Quantity) this.value.toPlainString() else this.asStringValue()
}

fun CodeableConcept.stringValue(): String =
  this.text ?: this.codingFirstRep.display ?: this.codingFirstRep.code

fun Resource.toJson(parser: IParser = FhirContext.forR4().newJsonParser()): String =
  parser.encodeResourceToString(this)

fun <T : Resource> T.updateFrom(updatedResource: Resource): T {
  var extensionUpdateForm = listOf<Extension>()
  if (updatedResource is Patient) {
    extensionUpdateForm = updatedResource.extension
  }
  var extension = listOf<Extension>()
  if (this is Patient) {
    extension = this.extension
  }
  val jsonParser = FhirContext.forR4().newJsonParser()
  val stringJson = toJson(jsonParser)
  val originalResourceJson = JSONObject(stringJson)

  originalResourceJson.updateFrom(JSONObject(updatedResource.toJson(jsonParser)))
  return jsonParser.parseResource(this::class.java, originalResourceJson.toString()).apply {
    val meta = this.meta
    val metaUpdateForm = this@updateFrom.meta
    if ((meta == null || meta.isEmpty)) {
      if (metaUpdateForm != null) {
        this.meta = metaUpdateForm
        this.meta.tag = metaUpdateForm.tag
      }
    } else {
      val setOfTags = mutableSetOf<Coding>()
      setOfTags.addAll(meta.tag)
      setOfTags.addAll(metaUpdateForm.tag)
      this.meta.tag = setOfTags.distinctBy { it.code + it.system }
    }
    if (this is Patient && this@updateFrom is Patient && updatedResource is Patient) {
      if (extension.isEmpty()) {
        if (extensionUpdateForm.isNotEmpty()) {
          this.extension = extensionUpdateForm
        }
      } else {
        val setOfExtension = mutableSetOf<Extension>()
        setOfExtension.addAll(extension)
        setOfExtension.addAll(extensionUpdateForm)
        this.extension = setOfExtension.distinct()
      }
    }
  }
}

@Throws(JSONException::class)
fun JSONObject.updateFrom(updated: JSONObject) {
  val keys =
    mutableListOf<String>().apply {
      keys().forEach { add(it) }
      updated.keys().forEach { add(it) }
    }

  keys.forEach { key -> updated.opt(key)?.run { put(key, this) } }
}

/**
 * Set all questions that are not of type [Questionnaire.QuestionnaireItemType.GROUP] to readOnly if
 * [readOnly] is true. This also generates the correct FHIRPath population expression for each
 * question when mapped to the corresponding [QuestionnaireResponse]
 */
fun List<Questionnaire.QuestionnaireItemComponent>.prepareQuestionsForReadingOrEditing(
  path: String,
  readOnly: Boolean = false,
) {
  forEach { item ->
    if (item.type != Questionnaire.QuestionnaireItemType.GROUP) {
      item.readOnly = readOnly
      item.createCustomExtensionsIfExist(path)
      item.item.prepareQuestionsForReadingOrEditing(
        "$path.where(linkId = '${item.linkId}').answer.item",
        readOnly
      )
    } else {
      item.item.prepareQuestionsForReadingOrEditing(
        "$path.where(linkId = '${item.linkId}').item",
        readOnly
      )
    }
  }
}

private fun Questionnaire.QuestionnaireItemComponent.createCustomExtensionsIfExist(path: String) {
  val list = mutableListOf<Extension>()
  list.add(
    Extension().apply {
      url = "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-initialExpression"
      setValue(
        Expression().apply {
          language = "text/fhirpath"
          expression =
            "$path.where(linkId = '${this@createCustomExtensionsIfExist.linkId}').answer.value"
        }
      )
    }
  )
  this.getExtensionByUrl(FhirCoreQuestionnaireFragment.BARCODE_URL)?.let {
    list.add(
      createCustomExtension(
        FhirCoreQuestionnaireFragment.BARCODE_URL,
        FhirCoreQuestionnaireFragment.BARCODE_NAME
      )
    )
  }
  this.getExtensionByUrl(FhirCoreQuestionnaireFragment.PHOTO_CAPTURE_URL)?.let {
    list.add(
      createCustomExtension(
        FhirCoreQuestionnaireFragment.PHOTO_CAPTURE_URL,
        FhirCoreQuestionnaireFragment.PHOTO_CAPTURE_NAME
      )
    )
  }
  this.extension = list
}

private fun createCustomExtension(url: String, name: String): Extension {
  return Extension().apply {
    this.url = url
    setValue(StringType().apply { value = name })
  }
}

/** Delete resources in [QuestionnaireResponse.contained] from the database */
suspend fun QuestionnaireResponse.deleteRelatedResources(defaultRepository: DefaultRepository) {
  contained.forEach { defaultRepository.delete(it) }
}

fun QuestionnaireResponse.retainMetadata(questionnaireResponse: QuestionnaireResponse) {
  author = questionnaireResponse.author
  authored = questionnaireResponse.authored
  id = questionnaireResponse.logicalId

  val versionId = Integer.parseInt(questionnaireResponse.meta.versionId ?: "1") + 1

  questionnaireResponse.meta.apply {
    lastUpdated = Date()
    setVersionId(versionId.toString())
  }
}

fun QuestionnaireResponse.assertSubject() {
  if (!this.hasSubject() || !this.subject.hasReference())
    throw IllegalStateException("QuestionnaireResponse must have a subject reference assigned")
}

fun Resource.generateMissingId() {
  if (logicalId.isBlank()) id = UUID.randomUUID().toString()
}

fun Resource.isPatient(patientId: String) =
  this.resourceType == ResourceType.Patient && this.logicalId == patientId

fun Resource.asReference(): Reference {
  val referenceValue = "${fhirType()}/$logicalId"

  return Reference().apply { this.reference = referenceValue }
}

fun Resource.referenceValue(): String = "${fhirType()}/$logicalId"

fun Resource.setPropertySafely(name: String, value: Base) =
  kotlin.runCatching { this.setProperty(name, value) }.onFailure { Timber.w(it) }.getOrNull()

fun ResourceType.generateUniqueId() = UUID.randomUUID().toString()
