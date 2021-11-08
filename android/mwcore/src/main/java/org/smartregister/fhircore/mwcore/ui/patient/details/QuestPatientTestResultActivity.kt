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

package org.smartregister.fhircore.mwcore.ui.patient.details

import android.os.Bundle
import androidx.activity.compose.setContent
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.mwcore.MwCoreApplication
import org.smartregister.fhircore.mwcore.data.patient.PatientRepository
import org.smartregister.fhircore.mwcore.ui.patient.register.PatientItemMapper

class QuestPatientTestResultActivity : BaseMultiLanguageActivity() {

  private lateinit var patientId: String

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    patientId = intent.extras?.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY) ?: "1"
    val fhirEngine = (MwCoreApplication.getContext() as ConfigurableApplication).fhirEngine
    val repository = PatientRepository(fhirEngine, PatientItemMapper)
    val viewModel =
      QuestPatientDetailViewModel.get(this, application as MwCoreApplication, repository, patientId)

    viewModel.setOnBackPressListener(this::onBackPressListener)

    setContent { AppTheme { QuestPatientTestResultScreen(viewModel) } }
  }

  private fun onBackPressListener() {
    finish()
  }
}
