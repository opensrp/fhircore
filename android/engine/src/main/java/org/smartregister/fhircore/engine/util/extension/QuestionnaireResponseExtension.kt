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
