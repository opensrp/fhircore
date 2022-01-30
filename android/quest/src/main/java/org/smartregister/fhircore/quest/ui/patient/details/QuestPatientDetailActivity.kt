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
import androidx.activity.viewModels
import androidx.annotation.StringRes
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration
import org.smartregister.fhircore.engine.cql.LibraryEvaluator.Companion.OUTPUT_PARAMETER_KEY
import org.smartregister.fhircore.engine.nfc.MainViewModel
import org.smartregister.fhircore.engine.nfc.main.PatientNfcItem
import org.smartregister.fhircore.engine.ui.base.AlertDialogue
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.data.patient.model.PatientItem
import org.smartregister.fhircore.quest.util.QuestConfigClassification
import timber.log.Timber

@AndroidEntryPoint
class QuestPatientDetailActivity : BaseMultiLanguageActivity() {

  private lateinit var patientId: String

  val patientViewModel by viewModels<QuestPatientDetailViewModel>()

  private val mainViewModel: MainViewModel by viewModels()

  @Inject lateinit var configurationRegistry: ConfigurationRegistry

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    patientId = intent.extras?.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY)!!

    patientViewModel.apply {
      val detailActivity = this@QuestPatientDetailActivity
      onBackPressClicked.observe(
        detailActivity,
        { backPressed -> if (backPressed) detailActivity.finish() }
      )
      onMenuItemClicked.observe(detailActivity, detailActivity::launchTestResults)
      onFormItemClicked.observe(detailActivity, detailActivity::launchQuestionnaireForm)
      onFormTestResultClicked.observe(detailActivity, detailActivity::onTestResultItemClickListener)
    }
    patientViewModel.run {
      getDemographics(patientId)
      getAllResults(patientId)
      getAllForms(this@QuestPatientDetailActivity)
    }
    setContent { AppTheme { QuestPatientDetailScreen(patientViewModel) } }
  }

  private fun launchTestResults(@StringRes id: Int) {
    when (id) {
      R.string.test_results ->
        startActivity(
          Intent(this, QuestPatientTestResultActivity::class.java).apply {
            putExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY, patientId)
          }
        )
      R.string.run_cql -> runCql()
      R.string.edit_patient_info ->
        startActivity(
          Intent(this, QuestionnaireActivity::class.java)
            .putExtras(
              QuestionnaireActivity.intentArgs(
                clientIdentifier = patientId,
                formName = getRegistrationForm(),
                editMode = true
              )
            )
        )
      R.string.write_to_card -> launchWriteToCard(patientViewModel.patientItem.value)
    }
  }

  fun getRegistrationForm(): String {
    return configurationRegistry.retrieveConfiguration<RegisterViewConfiguration>(
        configClassification = QuestConfigClassification.PATIENT_REGISTER
      )
      .registrationForm
  }

  fun runCql() {
    val progress = AlertDialogue.showProgressAlert(this, R.string.loading)

    patientViewModel
      .runCqlFor(patientId, this)
      .observe(
        this,
        {
          if (it?.isNotBlank() == true) {
            progress.dismiss()

            AlertDialogue.showInfoAlert(this, it, getString(R.string.run_cql_log))
            // show separate alert for output resources generated
            it.substringAfter(OUTPUT_PARAMETER_KEY, "").takeIf { it.isNotBlank() }?.let {
              AlertDialogue.showInfoAlert(this, it, getString(R.string.run_cql_output))
            }
          }
        }
      )
  }

  private fun launchQuestionnaireForm(questionnaireConfig: QuestionnaireConfig?) {
    if (questionnaireConfig != null) {
      startActivity(
        Intent(this, QuestionnaireActivity::class.java).apply {
          putExtras(
            QuestionnaireActivity.intentArgs(
              clientIdentifier = patientId,
              formName = questionnaireConfig.identifier
            )
          )
        }
      )
    }
  }

  private fun onTestResultItemClickListener(questionnaireResponse: QuestionnaireResponse?) {
    if (questionnaireResponse != null) {
      if (questionnaireResponse.questionnaire != null) {
        val questionnaireId = questionnaireResponse.questionnaire.split("/")[1]
        val populationResources = ArrayList<Resource>().apply { add(questionnaireResponse) }
        startActivity(
          Intent(this, QuestionnaireActivity::class.java)
            .putExtras(
              QuestionnaireActivity.intentArgs(
                clientIdentifier = patientId,
                formName = questionnaireId,
                readOnly = true,
                populationResources = populationResources
              )
            )
        )
      } else {
        Toast.makeText(
            this,
            getString(R.string.cannot_find_parent_questionnaire),
            Toast.LENGTH_LONG
          )
          .show()
        Timber.e(
          Exception(
            "Cannot open QuestionnaireResponse because QuestionnaireResponse.questionnaire is null"
          )
        )
      }
    }
  }

  private fun launchWriteToCard(patientItem: PatientItem?) {
    mainViewModel.generateProtoFile()
    // InitializeSAM
    mainViewModel.initSAM()
    // Perform Write action with the UI given by the Service

    val patientNfcItem =
      patientItem?.let {
        PatientNfcItem(
          patientId = it.id,
          // identifier = dto.identifierFirstRep.value ?: "",
          firstName = it.name,
          gender = it.gender.toString(),
          age = it.age,
          lastName = "",
          middleName = "",
          birthDate = "",
          caretakerName = "",
          caretakerRelationship = "",
          village = "",
          healthCenter = "",
          beneficiaryGroup = "",
          registrationDate = "",
          creationDate = ""
        )
      }
    val json = Gson().toJson(patientNfcItem)
    mainViewModel.writeSerialized(json)
  }
}
