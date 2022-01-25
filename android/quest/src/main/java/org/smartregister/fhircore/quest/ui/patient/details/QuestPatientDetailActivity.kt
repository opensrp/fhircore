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

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.StringRes
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.logicalId
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.view.ConfigurableComposableView
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_RESPONSE
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.util.AssetUtil
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.configuration.parser.QuestDetailConfigParser
import org.smartregister.fhircore.quest.configuration.view.PatientDetailsViewConfiguration
import org.smartregister.fhircore.quest.data.patient.model.QuestResultItem
import org.smartregister.fhircore.quest.ui.patient.details.SimpleDetailsActivity.Companion.RECORD_ID_ARG
import org.smartregister.fhircore.quest.util.QuestConfigClassification

@AndroidEntryPoint
class QuestPatientDetailActivity :
  BaseMultiLanguageActivity(), ConfigurableComposableView<PatientDetailsViewConfiguration> {

  private lateinit var profileConfig: QuestPatientDetailViewModel.ProfileConfig
  private lateinit var patientDetailConfig: PatientDetailsViewConfiguration
  private lateinit var patientId: String
  @Inject lateinit var parser: QuestDetailConfigParser

  val patientViewModel by viewModels<QuestPatientDetailViewModel>()

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

    patientDetailConfig =
      configurationRegistry.retrieveConfiguration<PatientDetailsViewConfiguration>(
        configClassification = QuestConfigClassification.PATIENT_DETAILS_VIEW
      )

    // TODO Load binary resources
    profileConfig =
      AssetUtil.decodeAsset(fileName = QuestPatientDetailViewModel.PROFILE_CONFIG, this)

    if (configurationRegistry.isAppIdInitialized()) {
      configureViews(patientDetailConfig)
    }
    patientViewModel.run {
      getDemographicsWithAdditionalData(patientId, patientDetailConfig)
      getAllResults(patientId, profileConfig, patientDetailConfig, parser)
      getAllForms(profileConfig)
    }
    setContent { AppTheme { QuestPatientDetailScreen(patientViewModel) } }
  }

  override fun onResume() {
    super.onResume()

    patientViewModel.run {
      getDemographicsWithAdditionalData(patientId, patientDetailConfig)
      getAllResults(patientId, profileConfig, patientDetailConfig, parser)
      getAllForms(profileConfig)
    }
  }

  private fun launchTestResults(@StringRes id: Int) {
    when (id) {
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
    }
  }

  fun getRegistrationForm(): String {
    return configurationRegistry.retrieveConfiguration<RegisterViewConfiguration>(
        configClassification = QuestConfigClassification.PATIENT_REGISTER
      )
      .registrationForm
  }

  // TODO https://github.com/opensrp/fhircore/issues/961
  // allow handling the data back and forth between activities via workflow or config
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    if (resultCode == Activity.RESULT_OK)
      if (configurationRegistry.appId == "g6pd") {
        data?.getStringExtra(QUESTIONNAIRE_RESPONSE)?.let {
          val response =
            FhirContext.forR4Cached().newJsonParser().parseResource(it) as QuestionnaireResponse
          response.contained.find { it.resourceType == ResourceType.Encounter }?.logicalId?.let {
            startActivity(
              Intent(this, SimpleDetailsActivity::class.java).apply {
                putExtra(RECORD_ID_ARG, it.replace("#", ""))
              }
            )
          }
        }
      }
  }

  // TODO https://github.com/opensrp/fhircore/issues/961
  // allow handling the data back and forth between activities via workflow or config
  private fun launchQuestionnaireForm(questionnaireConfig: QuestionnaireConfig?) {
    if (questionnaireConfig != null) {
      startActivityForResult(
        Intent(this, QuestionnaireActivity::class.java).apply {
          putExtras(
            QuestionnaireActivity.intentArgs(
              clientIdentifier = patientId,
              formName = questionnaireConfig.identifier
            )
          )
        },
        0
      )
    }
  }

  private fun onTestResultItemClickListener(resultItem: QuestResultItem?) {
    resultItem?.let { parser?.onResultItemClicked(resultItem, this, patientId) }
  }

  override fun configureViews(viewConfiguration: PatientDetailsViewConfiguration) {
    patientViewModel.updateViewConfigurations(viewConfiguration)
  }
}
