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

package org.smartregister.fhircore.engine.ui.questionnaire

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import com.google.android.fhir.datacapture.QuestionnaireFragment
import com.google.android.fhir.datacapture.common.datatype.asStringValue
import com.google.android.fhir.datacapture.contrib.views.barcode.QuestionnaireItemBarCodeReaderViewHolderFactory

class FhirCoreQuestionnaireFragment : QuestionnaireFragment() {

  private val fhirQuestionnaireActivityViewModel: QuestionnaireViewModel by activityViewModels()

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

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    view.findViewById<View>(com.google.android.fhir.datacapture.R.id.pagination_next_button).apply {
      viewTreeObserver.addOnGlobalLayoutListener {
        fhirQuestionnaireActivityViewModel.updateSaveButtonEnableState(
          visibility == View.GONE || !isEnabled
        )
      }
    }
  }

  companion object {
    const val BARCODE_URL = "https://fhir.labs.smartregister.org/barcode-type-widget-extension"
    const val BARCODE_NAME = "barcode"
    const val PHOTO_CAPTURE_URL = "http://doc-of-photo-capture"
    const val PHOTO_CAPTURE_NAME = "photo-capture"
  }
}
