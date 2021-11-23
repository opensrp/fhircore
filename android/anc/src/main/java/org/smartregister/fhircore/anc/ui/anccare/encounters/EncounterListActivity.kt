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

package org.smartregister.fhircore.anc.ui.anccare.encounters

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.google.android.fhir.FhirEngine
import dagger.hilt.android.AndroidEntryPoint
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.data.EncounterRepository
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import javax.inject.Inject

@AndroidEntryPoint
class EncounterListActivity : BaseMultiLanguageActivity() {

  val encounterListViewModel by viewModels<EncounterListViewModel>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val patientId =
      intent.extras?.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY) ?: ""
    encounterListViewModel.repository.patientId = patientId
    encounterListViewModel.setAppBackClickListener(this::handleBackClicked)

    setContent { AppTheme { EncounterListScreen(encounterListViewModel) } }
  }

  private fun handleBackClicked() {
    finish()
  }
}
