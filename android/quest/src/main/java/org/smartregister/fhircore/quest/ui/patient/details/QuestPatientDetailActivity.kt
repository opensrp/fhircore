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

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.quest.QuestApplication
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.data.patient.PatientRepository
import org.smartregister.fhircore.quest.ui.patient.register.PatientItemMapper
import timber.log.Timber

class QuestPatientDetailActivity : BaseMultiLanguageActivity() {

  private lateinit var patientId: String

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    patientId = intent.extras?.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY) ?: "1"
    val fhirEngine = (QuestApplication.getContext() as ConfigurableApplication).fhirEngine
    val repository = PatientRepository(fhirEngine, PatientItemMapper)
    val viewModel =
      QuestPatientDetailViewModel.get(this, application as QuestApplication, repository, patientId)

    viewModel.setOnBackPressListener(this::onBackPressListener)
    viewModel.setOnMenuItemClickListener(this::onMenuItemClickListener)
    viewModel.setOnFormItemClickListener(this::onFormItemClickListener)
    viewModel.setOnTestResultItemClickListener(this::onTestResultItemClickListener)

    setContent { AppTheme { QuestPatientDetailScreen(viewModel) } }
  }

  private fun onBackPressListener() {
    finish()
  }

  private fun onMenuItemClickListener(menuItem: String) {
    startActivity(
      Intent(this, QuestPatientTestResultActivity::class.java).apply {
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY, patientId)
      }
    )
  }

  private fun onFormItemClickListener(item: QuestionnaireConfig) {
    startActivity(
      Intent(this, QuestionnaireActivity::class.java).apply {
        putExtras(
          QuestionnaireActivity.intentArgs(clientIdentifier = patientId, formName = item.identifier)
        )
      }
    )
  }

  private fun onTestResultItemClickListener(questionnaireResponse: QuestionnaireResponse) {
    if (questionnaireResponse.questionnaire != null) {
      val questionnaireId = questionnaireResponse.questionnaire.split("/")[1]
      val populationResources = ArrayList<Resource>().apply { add(questionnaireResponse) }
      startActivity(
        Intent(this, QuestionnaireActivity::class.java)
          .putExtras(
            QuestionnaireActivity.intentArgs(
              clientIdentifier = "",
              formName = questionnaireId,
              readOnly = true,
              populationResources = populationResources
            )
          )
      )
    } else {
      Toast.makeText(this, getString(R.string.cannot_find_parent_questionnaire), Toast.LENGTH_LONG)
        .show()
      Timber.e(
        Exception(
          "Cannot open QuestionnaireResponse because QuestionnaireResponse.questionnaire is null"
        )
      )
    }
  }
}
