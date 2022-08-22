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

package org.smartregister.fhircore.quest.ui.questionnaire

import com.google.android.fhir.datacapture.QuestionnaireFragment
import com.google.android.fhir.datacapture.common.datatype.asStringValue
import com.google.android.fhir.datacapture.contrib.views.barcode.QuestionnaireItemBarCodeReaderViewHolderFactory
import org.smartregister.fhircore.engine.ui.questionnaire.CustomPhotoCaptureFactory

class QuestQuestionnaireFragment : QuestionnaireFragment() {

  override fun getCustomQuestionnaireItemViewHolderFactoryMatchers():
    List<QuestionnaireItemViewHolderFactoryMatcher> {
    return listOf(
      QuestionnaireItemViewHolderFactoryMatcher(QuestionnaireItemBarCodeReaderViewHolderFactory) {
        questionnaireItem ->
        questionnaireItem.getExtensionByUrl(BARCODE_URL).let {
          if (it == null) false else it.value.asStringValue() == BARCODE_NAME
        }
      },
      QuestionnaireItemViewHolderFactoryMatcher(CustomPhotoCaptureFactory(this)) { questionnaireItem
        ->
        questionnaireItem.getExtensionByUrl(PHOTO_CAPTURE_URL).let {
          if (it == null) false else it.value.asStringValue() == PHOTO_CAPTURE_NAME
        }
      }
    )
  }

  companion object {
    const val BARCODE_URL = "https://fhir.labs.smartregister.org/barcode-type-widget-extension"
    const val BARCODE_NAME = "barcode"
    const val PHOTO_CAPTURE_URL = "http://doc-of-photo-capture"
    const val PHOTO_CAPTURE_NAME = "photo-capture"
  }
}
