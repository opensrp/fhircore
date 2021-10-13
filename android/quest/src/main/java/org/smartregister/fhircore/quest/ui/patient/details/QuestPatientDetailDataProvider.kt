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

package org.smartregister.fhircore.quest.ui.patient.details

import androidx.lifecycle.LiveData
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig

interface QuestPatientDetailDataProvider {

  fun getDemographics(): LiveData<Patient>
  fun onBackPressListener(): () -> Unit = {}
  fun onMenuItemClickListener(): (menuItem: String) -> Unit = {}
  fun getAllForms(): LiveData<List<QuestionnaireConfig>>
  fun getAllResults(): LiveData<List<QuestionnaireResponse>>
  fun onFormItemClickListener(): (item: QuestionnaireConfig) -> Unit
  fun onTestResultItemClickListener(): (item: QuestionnaireResponse) -> Unit
}
