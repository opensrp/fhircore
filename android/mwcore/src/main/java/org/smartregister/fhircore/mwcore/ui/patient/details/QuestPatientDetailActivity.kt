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
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.view.ConfigurableComposableView
import org.smartregister.fhircore.engine.configuration.view.NavigationOption
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration
import org.smartregister.fhircore.engine.ui.base.AlertDialogue
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_RESPONSE
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireType
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.util.extension.decodeResourceFromString
import org.smartregister.fhircore.engine.util.extension.getEncounterId
import org.smartregister.fhircore.mwcore.R
import org.smartregister.fhircore.mwcore.configuration.view.DataDetailsListViewConfiguration
import org.smartregister.fhircore.mwcore.configuration.view.QuestionnaireNavigationAction
import org.smartregister.fhircore.mwcore.configuration.view.ResultDetailsNavigationConfiguration
import org.smartregister.fhircore.mwcore.configuration.view.TestDetailsNavigationAction
import org.smartregister.fhircore.mwcore.data.patient.model.QuestResultItem
import org.smartregister.fhircore.mwcore.ui.patient.details.SimpleDetailsActivity.Companion.RECORD_ID_ARG
import org.smartregister.fhircore.mwcore.util.MwCoreConfigClassification
import org.smartregister.fhircore.mwcore.util.QuestJsonSpecificationProvider

@AndroidEntryPoint
class QuestPatientDetailActivity :
  BaseMultiLanguageActivity(), ConfigurableComposableView<DataDetailsListViewConfiguration> {

  var patientResourcesList: ArrayList<String> = arrayListOf()
  private lateinit var patientDetailConfig: DataDetailsListViewConfiguration
  private lateinit var patientId: String

  val patientViewModel by viewModels<ListDataDetailViewModel>()

  @Inject lateinit var configurationRegistry: ConfigurationRegistry
  @Inject lateinit var questJsonSpecificationProvider: QuestJsonSpecificationProvider

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    patientId = intent.extras?.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY)!!

    patientViewModel.apply {
      val detailActivity = this@QuestPatientDetailActivity
      onBackPressClicked.observe(detailActivity) { backPressed ->
        if (backPressed) detailActivity.finish()
      }
      fetchPatientResources(patientId)
        .observe(detailActivity, detailActivity::handlePatientResources)
      onMenuItemClicked.observe(detailActivity, detailActivity::launchTestResults)
      onFormItemClicked.observe(detailActivity, detailActivity::launchQuestionnaireForm)
      onFormTestResultClicked.observe(detailActivity, detailActivity::onTestResultItemClickListener)
    }

    patientDetailConfig =
      configurationRegistry.retrieveConfiguration(
        configClassification = MwCoreConfigClassification.PATIENT_DETAILS_VIEW,
        questJsonSpecificationProvider.getJson()
      )

    if (configurationRegistry.isAppIdInitialized()) {
      configureViews(patientDetailConfig)
    }

    loadData()

    setContent { AppTheme { QuestPatientDetailScreen(patientViewModel) } }
  }

  private fun handlePatientResources(resourceList: ArrayList<String>) {
    if (resourceList.isNotEmpty()) patientResourcesList.addAll(resourceList)
  }

  override fun onResume() {
    super.onResume()

    loadData()
  }

  fun loadData() {
    patientViewModel.run {
      getDemographicsWithAdditionalData(patientId, patientDetailConfig)
      getAllResults(
        patientId,
        ResourceType.Patient,
        patientDetailConfig.questionnaireFilter!!,
        patientDetailConfig
      )
      getAllForms(patientDetailConfig.questionnaireFilter!!)
    }
  }

  private fun launchTestResults(@StringRes id: Int) {
    when (id) {
      R.string.edit_patient_info -> {
        startActivity(
          Intent(this, QuestionnaireActivity::class.java)
            .putExtras(
              QuestionnaireActivity.intentArgs(
                clientIdentifier = patientId,
                formName = getRegistrationForm(),
                questionnaireType = QuestionnaireType.EDIT
              )
            )
            .apply {
              if (patientResourcesList.isNotEmpty()) {
                this.putStringArrayListExtra(
                  QuestionnaireActivity.QUESTIONNAIRE_POPULATION_RESOURCES,
                  patientResourcesList
                )
              }
            }
        )
      }
    }
  }

  fun getRegistrationForm(): String {
    return configurationRegistry.retrieveConfiguration<RegisterViewConfiguration>(
        configClassification = MwCoreConfigClassification.PATIENT_REGISTER,
        questJsonSpecificationProvider.getJson()
      )
      .registrationForm
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    if (resultCode == Activity.RESULT_OK)
      getResultDetailsNavigationOptions().navigationOptions.forEach {
        when (it.action) {
          is TestDetailsNavigationAction -> {
            data?.getStringExtra(QUESTIONNAIRE_RESPONSE)?.let {
              val response =
                FhirContext.forR4Cached().newJsonParser().parseResource(it) as QuestionnaireResponse
              response.getEncounterId()?.let {
                startActivity(
                  Intent(this, SimpleDetailsActivity::class.java).apply {
                    putExtra(RECORD_ID_ARG, it.replace("#", ""))
                  }
                )
              }
            }
          }
          is QuestionnaireNavigationAction -> {}
        }
      }
  }

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
    getResultDetailsNavigationOptions().navigationOptions.forEach {
      handleNavigationOptions(it, resultItem, patientId)
    }
  }

  private fun handleNavigationOptions(
    navigationOption: NavigationOption,
    resultItem: QuestResultItem?,
    patientId: String
  ) {
    when (navigationOption.action) {
      is QuestionnaireNavigationAction -> {
        resultItem?.let {
          when {
            resultItem.source.second.logicalId.isNullOrBlank() -> {
              AlertDialogue.showErrorAlert(this, R.string.invalid_form_id)
            }
            else -> {
              startActivity(
                Intent(this@QuestPatientDetailActivity, QuestionnaireActivity::class.java)
                  .putExtras(
                    QuestionnaireActivity.intentArgs(
                      clientIdentifier = patientId,
                      formName = resultItem.source.second.logicalId!!,
                      questionnaireType = QuestionnaireType.READ_ONLY,
                      questionnaireResponse =
                        resultItem.source.first.questionnaireResponseString
                          .decodeResourceFromString()
                    )
                  )
              )
            }
          }
        }
      }
      is TestDetailsNavigationAction -> {
        resultItem?.source?.first?.encounterId?.let {
          startActivity(
            Intent(this@QuestPatientDetailActivity, SimpleDetailsActivity::class.java).apply {
              putExtra(RECORD_ID_ARG, it)
            }
          )
        }
      }
    }
  }

  fun getResultDetailsNavigationOptions() =
    configurationRegistry.retrieveConfiguration<ResultDetailsNavigationConfiguration>(
      configClassification = MwCoreConfigClassification.RESULT_DETAILS_NAVIGATION,
      questJsonSpecificationProvider.getJson()
    )

  override fun configureViews(viewConfiguration: DataDetailsListViewConfiguration) {
    patientViewModel.updateViewConfigurations(viewConfiguration)
  }
}
