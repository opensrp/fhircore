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

package org.smartregister.fhircore.engine.util.extension

import org.hl7.fhir.r4.model.Attachment
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent

/** Clears the item text in the [QuestionnaireResponse]. */
fun QuestionnaireResponse.clearText() {
  this.item.clearText()
}

/** Clears the text of items in the current list. */
private fun List<QuestionnaireResponseItemComponent>.clearText() {
  this.forEach { itemToClear ->
    itemToClear.text = null
    if (itemToClear.hasItem()) {
      itemToClear.item.clearText()
    }
  }
}

/** Borrows from: https://github.com/google/android-fhir/pull/1936 */
fun QuestionnaireResponse.packRepeatedGroups() {
  item = item.packRepeatedGroups()
}

private fun List<QuestionnaireResponse.QuestionnaireResponseItemComponent>.packRepeatedGroups():
  List<QuestionnaireResponse.QuestionnaireResponseItemComponent> {
  forEach { it ->
    it.item = it.item.packRepeatedGroups()
    it.answer.forEach { it.item = it.item.packRepeatedGroups() }
  }
  val linkIdToPackedResponseItems =
    groupBy { it.linkId }
      .mapValues { (linkId, questionnaireResponseItems) ->
        questionnaireResponseItems.singleOrNull()
          ?: QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
            this.linkId = linkId
            answer =
              questionnaireResponseItems.map {
                QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                  item = it.item
                }
              }
          }
      }
  return map { it.linkId }.distinct().map { linkIdToPackedResponseItems[it]!! }
}

/** Pre-order list of all questionnaire response items in the questionnaire. */
val QuestionnaireResponse.allItems: List<QuestionnaireResponse.QuestionnaireResponseItemComponent>
  get() = item.flatMap { it.descendant }

/**
 * Drops the file bytes of every attachment answer, nested ones included. Each answer keeps its
 * content type, title and creation date. Use it on a copy that is handed to another screen and
 * never saved: Android limits what it passes between screens to about 1 MB.
 */
fun QuestionnaireResponse.removeAttachmentData() =
  allItems.forEach { item ->
    item.answer.forEach { answer -> (answer.value as? Attachment)?.data = null }
  }

/**
 * For a read-only summary, on a copy that is never saved. Drops the file bytes of every attachment
 * answer, as [removeAttachmentData] does, and puts [label] in the file's url: the SDK's read-only
 * screen shows only the url of a file, and « Not Answered » without one. [label] gets the answer's
 * item and the attachment, with its bytes still there. A file that already has a url keeps it.
 */
fun QuestionnaireResponse.replaceAttachmentDataWithLabel(
  label: (QuestionnaireResponseItemComponent, Attachment) -> String,
) =
  allItems.forEach { item ->
    item.answer.forEach { answer ->
      (answer.value as? Attachment)
        ?.takeIf { it.hasData() }
        ?.let { attachment ->
          if (!attachment.hasUrl()) attachment.url = label(item, attachment)
          attachment.data = null
        }
    }
  }

/**
 * Estimates how many bytes this response takes as JSON in the database. Each file's bytes are
 * lifted out while the rest is encoded, then counted at their base64 length, so the files are never
 * copied.
 */
fun QuestionnaireResponse.estimatedJsonSize(): Long {
  val attachments =
    allItems
      .flatMap { item -> item.answer.mapNotNull { it.value as? Attachment } }
      .filter { it.data != null }
  // The elements themselves go back, so HAPI doesn't encode each file to base64 again
  val elements = attachments.map { it.dataElement }
  return try {
    attachments.forEach { it.dataElement = null }
    val textSize = encodeResourceToString().toByteArray(Charsets.UTF_8).size.toLong()
    textSize + elements.sumOf { 4L * ((it.value.size + 2L) / 3) + ATTACHMENT_DATA_FIELD_LENGTH }
  } finally {
    attachments.zip(elements).forEach { (attachment, element) -> attachment.dataElement = element }
  }
}

/** The characters around a file's base64 text in the JSON: `,"data":""`. */
private const val ATTACHMENT_DATA_FIELD_LENGTH = 10L

/**
 * Pre-order list of descendants of the questionnaire response item (inclusive of the current item).
 */
val QuestionnaireResponse.QuestionnaireResponseItemComponent.descendant:
  List<QuestionnaireResponse.QuestionnaireResponseItemComponent>
  get() =
    mutableListOf<QuestionnaireResponse.QuestionnaireResponseItemComponent>().also {
      appendDescendantTo(it)
    }

private fun QuestionnaireResponse.QuestionnaireResponseItemComponent.appendDescendantTo(
  output: MutableList<QuestionnaireResponse.QuestionnaireResponseItemComponent>,
) {
  output.add(this)
  item.forEach { it.appendDescendantTo(output) }
  answer.forEach { answer -> answer.item.forEach { it.appendDescendantTo(output) } }
}
