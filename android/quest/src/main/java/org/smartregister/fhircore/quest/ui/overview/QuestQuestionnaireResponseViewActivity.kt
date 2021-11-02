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

package org.smartregister.fhircore.quest.ui.overview

import android.app.Application
import android.view.View
import androidx.lifecycle.ViewModelProvider
import ca.uhn.fhir.context.FhirContext
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireViewModel
import org.smartregister.fhircore.engine.util.extension.createFactory
import org.smartregister.fhircore.engine.util.extension.showToast

/** Created by Ephraim Kigamba - nek.eam@gmail.com on 28-10-2021. */
class QuestQuestionnaireResponseViewActivity : QuestionnaireActivity() {

  override fun onClick(view: View) {
    if (view.id == R.id.btn_save_client_info) {
      finish()
    } else {
      showToast(getString(R.string.error_saving_form))
    }
  }

  override fun createViewModel(
    application: Application,
    readOnly: Boolean
  ): QuestionnaireViewModel {
    val questionnaireResponse = intent.getStringExtra("questionnaire-response")
    return ViewModelProvider(
      this@QuestQuestionnaireResponseViewActivity,
      QuestQuestionnaireResponseViewModel(
          application,
          questionnaireResponse =
            FhirContext.forR4()
              .newJsonParser()
              .parseResource(QuestionnaireResponse::class.java, questionnaireResponse)
        )
        .createFactory()
    )[QuestQuestionnaireResponseViewModel::class.java]
  }
}
