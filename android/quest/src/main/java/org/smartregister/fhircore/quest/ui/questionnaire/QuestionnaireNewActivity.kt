/*
 * Copyright 2021-2023 Ona Systems, Inc
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

package org.smartregister.fhircore.quest.ui.questionnaire

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.navigation.navArgs
import com.google.android.fhir.datacapture.QuestionnaireFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.engine.ui.base.AlertDialogue
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.util.extension.encodeResourceToString
import org.smartregister.fhircore.engine.util.extension.showToast
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.databinding.QuestionnaireActivityBinding
import org.smartregister.fhircore.quest.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_FRAGMENT_TAG

@AndroidEntryPoint
class QuestionnaireNewActivity : BaseMultiLanguageActivity() {

  val viewModel by viewModels<QuestionnaireNewViewModel>()
  private val questionnaireFragmentArgs by navArgs<QuestionnaireNewActivityArgs>()
  private lateinit var viewBinding: QuestionnaireActivityBinding
  private var questionnaire: Questionnaire? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    setTheme(R.style.AppTheme_Questionnaire)
    viewBinding = QuestionnaireActivityBinding.inflate(layoutInflater)
    setContentView(viewBinding.root)

    if (savedInstanceState == null) renderQuestionnaire()

    this.onBackPressedDispatcher.addCallback(
      this,
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          handleBackPress()
        }
      },
    )
  }

  override fun onStart() {
    super.onStart()
    supportFragmentManager.setFragmentResultListener(
      QuestionnaireFragment.SUBMIT_REQUEST_KEY,
      this,
    ) { _, _ ->
      val questionnaireResponse = retrieveQuestionnaireResponse()
      questionnaireFragmentArgs.questionnaireConfig?.let { questionnaireConfig ->
        // Close questionnaire if opened in read only mode or if experimental
        if (questionnaireConfig.type.isReadOnly() || questionnaire?.experimental == false) {
          finish()
        }
        if (questionnaireResponse != null && questionnaire != null) {
          viewModel.handleQuestionnaireSubmission(
            questionnaire = questionnaire!!,
            currentQuestionnaireResponse = questionnaireResponse,
            questionnaireConfig = questionnaireConfig,
            actionParameters = questionnaireFragmentArgs.params,
            context = this,
          )
        }
      }
    }
  }

  private fun renderQuestionnaire() {
    val questionnaireConfig = questionnaireFragmentArgs.questionnaireConfig
    val actionParameters = questionnaireFragmentArgs.params

    lifecycleScope.launch {
      if (
        supportFragmentManager.findFragmentByTag(QUESTIONNAIRE_FRAGMENT_TAG) == null &&
          questionnaireConfig != null
      ) {
        viewBinding.questionnaireToolbar.apply {
          title = questionnaireConfig.title
          setNavigationIcon(R.drawable.ic_arrow_back)
          setNavigationOnClickListener { handleBackPress() }
        }

        questionnaire = viewModel.retrieveQuestionnaire(questionnaireConfig, actionParameters)
        val questionnaireJson = questionnaire?.asJson()
        if (questionnaireJson.isNullOrEmpty()) {
          showToast(getString(R.string.questionnaire_not_found))
          finish()
        }
        supportFragmentManager.commit {
          setReorderingAllowed(true)
          add(
            R.id.container,
            QuestionnaireFragment.builder().setQuestionnaire(questionnaireJson!!).build(),
            QUESTIONNAIRE_FRAGMENT_TAG,
          )
        }
      }
    }
  }

  private fun Questionnaire?.asJson(): String? = this?.encodeResourceToString()

  private fun handleBackPress() {
    val questionnaireConfig = questionnaireFragmentArgs.questionnaireConfig
    if (questionnaireConfig != null) {
      if (questionnaireConfig.type.isReadOnly()) {
        finish()
      } else if (questionnaireConfig.saveDraft) {
        AlertDialogue.showCancelAlert(
          context = this,
          message = R.string.questionnaire_in_progress_alert_back_pressed_message,
          title = R.string.questionnaire_alert_back_pressed_title,
          confirmButtonListener = {
            retrieveQuestionnaireResponse()?.let { questionnaireResponse ->
              viewModel.saveDraftQuestionnaire(questionnaireResponse)
            }
          },
          confirmButtonText = R.string.questionnaire_alert_back_pressed_save_draft_button_title,
          neutralButtonListener = { finish() },
          neutralButtonText = R.string.questionnaire_alert_back_pressed_button_title,
        )
      } else {
        AlertDialogue.showConfirmAlert(
          context = this,
          message = R.string.questionnaire_alert_back_pressed_message,
          title = R.string.questionnaire_alert_back_pressed_title,
          confirmButtonListener = { finish() },
          confirmButtonText = R.string.questionnaire_alert_back_pressed_button_title,
        )
      }
    }
  }

  private fun retrieveQuestionnaireResponse(): QuestionnaireResponse? =
    (supportFragmentManager.findFragmentByTag(QUESTIONNAIRE_FRAGMENT_TAG) as QuestionnaireFragment?)
      ?.getQuestionnaireResponse()
}
