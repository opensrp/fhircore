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
import com.google.android.fhir.datacapture.QuestionnaireFragment
import com.google.android.fhir.datacapture.common.datatype.asStringValue

class CustomQuestionnaireFragment : QuestionnaireFragment() {

  private lateinit var photoCaptureFactory: CustomPhotoCaptureFactory

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    photoCaptureFactory = CustomPhotoCaptureFactory(this)
  }

  override fun getCustomQuestionnaireItemViewHolderFactoryMatchers():
    List<QuestionnaireItemViewHolderFactoryMatcher> {
    return listOf(
      QuestionnaireItemViewHolderFactoryMatcher(photoCaptureFactory) { questionnaireItem ->
        questionnaireItem.getExtensionByUrl(CustomPhotoCaptureFactory.URL).let {
          if (it == null) false else it.value.asStringValue() == CustomPhotoCaptureFactory.NAME
        }
      }
    )
  }
}
