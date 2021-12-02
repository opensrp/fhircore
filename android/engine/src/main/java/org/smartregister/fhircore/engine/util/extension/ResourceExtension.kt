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
import com.google.android.fhir.logicalId
import java.util.Date
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Expression
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import org.json.JSONException
import org.json.JSONObject
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import timber.log.Timber

fun Resource.toJson(parser: IParser = FhirContext.forR4().newJsonParser()): String =
  parser.encodeResourceToString(this)

fun <T : Resource> T.updateFrom(updatedResource: Resource): T {
  Timber.e("Resource that going to be updated")
  Timber.e(FhirContext.forR4().newJsonParser().encodeResourceToString(this))
  Timber.e("Resource that being updated")
  Timber.e(FhirContext.forR4().newJsonParser().encodeResourceToString(updatedResource))

  val jsonParser = FhirContext.forR4().newJsonParser()
  val stringJson = toJson(jsonParser)
  val originalResourceJson = JSONObject(stringJson)

  Timber.e("Updated resource")
  originalResourceJson.updateFrom(JSONObject(updatedResource.toJson(jsonParser)))
  Timber.e(originalResourceJson.toString())
  return jsonParser.parseResource(this::class.java, originalResourceJson.toString()).apply {
    if (this.meta == null || this.meta.isEmpty) {
      this.meta = this@updateFrom.meta
      this.meta.tag = this@updateFrom.meta.tag
    } else {
      val setOfTags: Set<Coding> = setOf()
      setOfTags.plus(this.meta.tag)
      setOfTags.plus(this@updateFrom.meta.tag)
      this.meta.tag = setOfTags.distinct()
    }
    if (this is Patient && this@updateFrom is Patient) {
      this.extension = this@updateFrom.extension
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
      item.extension =
        listOf(
          Extension().apply {
            url =
              "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-initialExpression"
            setValue(
              Expression().apply {
                language = "text/fhirpath"
                expression = "$path.where(linkId = '${item.linkId}').answer.value"
              }
            )
          }
        )
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

/** Delete resources in [QuestionnaireResponse.contained] from the database */
suspend fun QuestionnaireResponse.deleteRelatedResources(defaultRepository: DefaultRepository) {
  contained.forEach { defaultRepository.delete(it) }
}

fun QuestionnaireResponse.retainMetadata(questionnaireResponse: QuestionnaireResponse) {
  author = questionnaireResponse!!.author
  authored = questionnaireResponse!!.authored
  id = questionnaireResponse!!.logicalId

  val versionId = Integer.parseInt(questionnaireResponse!!.meta.versionId ?: "1") + 1

  questionnaireResponse.meta.apply {
    lastUpdated = Date()
    setVersionId(versionId.toString())
  }
}
