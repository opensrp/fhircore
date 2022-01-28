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

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.StringRes
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.logicalId
import dagger.hilt.android.AndroidEntryPoint
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.view.ConfigurableComposableView
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration
import org.smartregister.fhircore.engine.cql.LibraryEvaluator
import org.smartregister.fhircore.engine.ui.base.AlertDialogue
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.util.AssetUtil
import org.smartregister.fhircore.mwcore.R
import org.smartregister.fhircore.mwcore.configuration.parser.DetailConfigParser
import org.smartregister.fhircore.mwcore.configuration.view.PatientDetailsViewConfiguration
import org.smartregister.fhircore.mwcore.data.patient.model.QuestResultItem
import org.smartregister.fhircore.mwcore.ui.patient.details.SimpleDetailsActivity.Companion.RECORD_ID_ARG
import org.smartregister.fhircore.mwcore.util.MwCoreConfigClassification
import javax.inject.Inject

@AndroidEntryPoint
class QuestPatientDetailActivity :
  BaseMultiLanguageActivity(), ConfigurableComposableView<PatientDetailsViewConfiguration> {

  private lateinit var profileConfig: QuestPatientDetailViewModel.ProfileConfig
  private lateinit var patientDetailConfig: PatientDetailsViewConfiguration
  private lateinit var patientId: String
  private var parser: DetailConfigParser? = null

  val patientViewModel by viewModels<QuestPatientDetailViewModel>()

  @Inject
  lateinit var configurationRegistry: ConfigurationRegistry

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
      configurationRegistry.retrieveConfiguration(
        configClassification = MwCoreConfigClassification.PATIENT_DETAILS_VIEW
      )

    parser = patientViewModel.loadParser(packageName, patientDetailConfig)

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
      R.string.test_results ->
        startActivity(
          Intent(this, SimpleDetailsActivity::class.java).apply {
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
    }
  }

  fun getRegistrationForm(): String {
    return configurationRegistry.retrieveConfiguration<RegisterViewConfiguration>(
      configClassification = MwCoreConfigClassification.PATIENT_REGISTER_CLIENT
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
            it.substringAfter(LibraryEvaluator.OUTPUT_PARAMETER_KEY, "").takeIf { it.isNotBlank() }?.let {
              AlertDialogue.showInfoAlert(this, it, getString(R.string.run_cql_output))
            }
          }
        }
      )
  }

  // TODO https://github.com/opensrp/fhircore/issues/961
  // allow handling the data back and forth between activities via workflow or config
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    if (resultCode == Activity.RESULT_OK)
      if (configurationRegistry.appId == "g6pd") {
        data?.getStringExtra(QuestionnaireActivity.QUESTIONNAIRE_RESPONSE)?.let {
          val response =
            FhirContext.forR4().newJsonParser().parseResource(it) as QuestionnaireResponse
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
