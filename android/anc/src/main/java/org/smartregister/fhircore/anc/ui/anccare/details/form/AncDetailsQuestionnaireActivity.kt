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

package org.smartregister.fhircore.anc.ui.anccare.details.form

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.data.AncPatientRepository
import org.smartregister.fhircore.anc.data.FamilyRepository
import org.smartregister.fhircore.anc.ui.anccare.register.AncItemMapper
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity

class AncDetailsQuestionnaireActivity : QuestionnaireActivity() {
  internal lateinit var ancPatientRepository: AncPatientRepository
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    ancPatientRepository = AncPatientRepository(AncApplication.getContext().fhirEngine, AncItemMapper)
  }

  override fun handleQuestionnaireResponse(questionnaireResponse: QuestionnaireResponse) {
    lifecycleScope.launch {
      ancPatientRepository.postVitalSigns(questionnaire!!, questionnaireResponse)
      finish()
    }
  }
}
