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
