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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Library
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.engine.cql.LibraryEvaluator
import org.smartregister.fhircore.engine.ui.base.AlertDialogue
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.util.extension.filterBy
import org.smartregister.fhircore.quest.QuestApplication
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.data.patient.PatientRepository
import org.smartregister.fhircore.quest.ui.patient.register.PatientItemMapper
import timber.log.Timber

class QuestPatientDetailActivity : BaseMultiLanguageActivity() {

  private lateinit var patientId: String
  private lateinit var repository: PatientRepository

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    patientId = intent.extras?.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY) ?: "1"

    val fhirEngine = (QuestApplication.getContext() as ConfigurableApplication).fhirEngine
    repository = PatientRepository(fhirEngine, PatientItemMapper)
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
    when (menuItem) {
      PatientDetailMenuItem.TEST_RESULTS.name -> {
        startActivity(
          Intent(this, QuestPatientTestResultActivity::class.java).apply {
            putExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY, patientId)
          }
        )
      }
      PatientDetailMenuItem.CQL_RESULT.name -> {
        runCql()
      }
    }
  }

  fun runCql() {
    val progress = AlertDialogue.showProgressAlert(this, R.string.loading)
    val libraryEvaluator = LibraryEvaluator()
    val fhirEngine = repository.fhirEngine

    lifecycleScope.launch {
      val patient = fhirEngine.load(Patient::class.java, patientId)
      val observations = fhirEngine.filterBy<Observation>(Observation.SUBJECT, patient)
      val conditions = fhirEngine.filterBy<Condition>(Condition.SUBJECT, patient)

      val everything =
        mutableListOf<Resource>().apply {
          add(patient)
          addAll(observations)
          addAll(conditions)
        }

      val dataBundle = libraryEvaluator.createBundle(everything)

      var error = ""
      val result =
        kotlin
          .runCatching {
            libraryEvaluator
              .runCql(
                library = fhirEngine.load(Library::class.java, "4575"),
                helper = fhirEngine.load(Library::class.java, "1753"),
                valueSet = org.hl7.fhir.r4.model.Bundle(),
                data = dataBundle
              )
              .joinToString("\n")
          }
          .onFailure { error = it.stackTraceToString() }
          .getOrNull()

      progress.dismiss()

      AlertDialogue.showInfoAlert(this@QuestPatientDetailActivity, message = result ?: error)
    }
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

  private fun onTestResultItemClickListener(item: QuestionnaireResponse) {
    if (item.questionnaire != null) {
      val questionnaireId = item.questionnaire.split("/")[1]
      startActivity(
        Intent(this, QuestionnaireActivity::class.java)
          .putExtras(
            QuestionnaireActivity.intentArgs(
              clientIdentifier = null,
              formName = questionnaireId,
              readOnly = true,
              questionnaireResponse = item
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
