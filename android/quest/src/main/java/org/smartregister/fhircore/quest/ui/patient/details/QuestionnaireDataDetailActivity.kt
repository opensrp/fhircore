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
import ca.uhn.fhir.context.FhirContext
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.view.ConfigurableComposableView
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration
import org.smartregister.fhircore.engine.data.remote.model.response.UserInfo
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_RESPONSE
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.util.USER_INFO_SHARED_PREFERENCE_KEY
import org.smartregister.fhircore.engine.util.extension.decodeJson
import org.smartregister.fhircore.engine.util.extension.getEncounterId
import org.smartregister.fhircore.quest.configuration.view.DataDetailsListViewConfiguration
import org.smartregister.fhircore.quest.configuration.view.QuestionnaireNavigationAction
import org.smartregister.fhircore.quest.configuration.view.ResultDetailsNavigationConfiguration
import org.smartregister.fhircore.quest.configuration.view.TestDetailsNavigationAction
import org.smartregister.fhircore.quest.ui.patient.details.SimpleDetailsActivity.Companion.RECORD_ID_ARG
import org.smartregister.fhircore.quest.util.QuestConfigClassification

@AndroidEntryPoint
class QuestionnaireDataDetailActivity :
  BaseMultiLanguageActivity(), ConfigurableComposableView<DataDetailsListViewConfiguration> {

  private lateinit var detailConfig: DataDetailsListViewConfiguration
  private lateinit var subjectId: String

  val viewModel by viewModels<ListDataDetailViewModel>()

  @Inject lateinit var configurationRegistry: ConfigurationRegistry
  private val authenticatedUserInfo by lazy {
    sharedPreferencesHelper.read(USER_INFO_SHARED_PREFERENCE_KEY, null)?.decodeJson<UserInfo>()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    subjectId = authenticatedUserInfo?.organization!!

    viewModel.apply {
      val detailActivity = this@QuestionnaireDataDetailActivity
      onBackPressClicked.observe(detailActivity) { backPressed ->
        if (backPressed) detailActivity.finish()
      }
      onFormItemClicked.observe(detailActivity, detailActivity::launchQuestionnaireForm)
    }

    detailConfig =
      configurationRegistry.retrieveConfiguration(
        configClassification = QuestConfigClassification.QUESTIONNAIRE_DETAILS_VIEW
      )

    configureViews(detailConfig)
    loadData()

    setContent { AppTheme { QuestPatientDetailScreen(viewModel) } }
  }

  override fun onResume() {
    super.onResume()

    loadData()
  }

  fun loadData() {
    viewModel.run {
      with(detailConfig) {
        getAllForms(questionnaireFilter!!)
        getAllResults(subjectId, questionnaireFilter, detailConfig)
      }
    }
  }

  fun getRegistrationForm(): String {
    return configurationRegistry.retrieveConfiguration<RegisterViewConfiguration>(
        configClassification = QuestConfigClassification.PATIENT_REGISTER
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
              response.getEncounterId().let {
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
          putExtras(QuestionnaireActivity.intentArgs(formName = questionnaireConfig.identifier))
        },
        0
      )
    }
  }

  fun getResultDetailsNavigationOptions() =
    configurationRegistry.retrieveConfiguration<ResultDetailsNavigationConfiguration>(
      configClassification = QuestConfigClassification.RESULT_DETAILS_NAVIGATION
    )

  override fun configureViews(viewConfiguration: DataDetailsListViewConfiguration) {
    viewModel.updateViewConfigurations(viewConfiguration)
  }
}
