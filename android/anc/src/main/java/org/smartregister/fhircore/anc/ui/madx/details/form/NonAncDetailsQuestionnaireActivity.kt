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

package org.smartregister.fhircore.anc.ui.madx.details.form

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.data.NonAncPatientRepository
import org.smartregister.fhircore.anc.ui.anccare.register.AncItemMapper
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity


class NonAncDetailsQuestionnaireActivity : QuestionnaireActivity() {
    internal lateinit var ancPatientRepository: NonAncPatientRepository
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ancPatientRepository =
            NonAncPatientRepository(AncApplication.getContext().fhirEngine, AncItemMapper)
    }

    override fun handleQuestionnaireResponse(questionnaireResponse: QuestionnaireResponse) {
        when (questionnaireConfig.form) {
            NonAncDetailsFormConfig.ANC_VITAL_SIGNS_UNIT_OPTIONS ->
                lifecycleScope.launch {
                    ancPatientRepository.selectVitalSignStandard(
                        questionnaire!!,
                        questionnaireResponse,
                        clientIdentifier
                    )
                    this@NonAncDetailsQuestionnaireActivity.finish()
                }
            NonAncDetailsFormConfig.ANC_VITAL_SIGNS_METRIC -> lifecycleScope.launch {
                ancPatientRepository.postVitalSigns(
                    questionnaire!!,
                    questionnaireResponse,
                    clientIdentifier
                )
                this@NonAncDetailsQuestionnaireActivity.finish()
            }

            NonAncDetailsFormConfig.ANC_VITAL_SIGNS_STANDARD -> lifecycleScope.launch {
                ancPatientRepository.postVitalSigns(
                    questionnaire!!,
                    questionnaireResponse,
                    clientIdentifier
                )
                this@NonAncDetailsQuestionnaireActivity.finish()
            }
        }
    }
}
