/*
 * Copyright 2021-2024 Ona Systems, Inc
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

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.google.android.fhir.datacapture.QuestionnaireFragment
import com.google.android.fhir.logicalId
import dagger.hilt.android.AndroidEntryPoint
import java.io.Serializable
import java.util.LinkedList
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.engine.domain.model.ActionParameterType
import org.smartregister.fhircore.engine.domain.model.isEditable
import org.smartregister.fhircore.engine.domain.model.isReadOnly
import org.smartregister.fhircore.engine.ui.base.AlertDialogue
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.util.extension.clearText
import org.smartregister.fhircore.engine.util.extension.encodeResourceToString
import org.smartregister.fhircore.engine.util.extension.parcelable
import org.smartregister.fhircore.engine.util.extension.parcelableArrayList
import org.smartregister.fhircore.engine.util.extension.showToast
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.databinding.QuestionnaireActivityBinding
import timber.log.Timber

@AndroidEntryPoint
class QuestionnaireActivity : BaseMultiLanguageActivity() {

  val viewModel by viewModels<QuestionnaireViewModel>()
  private lateinit var questionnaireConfig: QuestionnaireConfig
  private lateinit var actionParameters: ArrayList<ActionParameter>
  private lateinit var viewBinding: QuestionnaireActivityBinding
  private var questionnaire: Questionnaire? = null
  private var alertDialog: AlertDialog? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setTheme(org.smartregister.fhircore.engine.R.style.AppTheme_Questionnaire)
    viewBinding = QuestionnaireActivityBinding.inflate(layoutInflater)
    setContentView(viewBinding.root)
    with(intent) {
      parcelable<QuestionnaireConfig>(QUESTIONNAIRE_CONFIG)?.also { questionnaireConfig = it }
      actionParameters = parcelableArrayList(QUESTIONNAIRE_ACTION_PARAMETERS) ?: arrayListOf()
    }

    if (!::questionnaireConfig.isInitialized) {
      showToast(getString(R.string.missing_questionnaire_config))
      finish()
      return
    }

    viewModel.questionnaireProgressStateLiveData.observe(this) { progressState ->
      alertDialog =
        if (progressState?.active == false) {
          alertDialog?.dismiss()
          null
        } else {
          when (progressState) {
            is QuestionnaireProgressState.ExtractionInProgress ->
              AlertDialogue.showProgressAlert(this, R.string.extraction_in_progress)
            is QuestionnaireProgressState.QuestionnaireLaunch ->
              AlertDialogue.showProgressAlert(this, R.string.loading_questionnaire)
            else -> null
          }
        }
    }

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

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.clear()
  }

  private fun renderQuestionnaire() {
    lifecycleScope.launch {
      if (supportFragmentManager.findFragmentByTag(QUESTIONNAIRE_FRAGMENT_TAG) == null) {
        viewModel.setProgressState(QuestionnaireProgressState.QuestionnaireLaunch(true))
        with(viewBinding) {
          questionnaireToolbar.apply {
            setNavigationIcon(R.drawable.ic_arrow_back)
            setNavigationOnClickListener { handleBackPress() }
          }
          questionnaireTitle.apply { text = questionnaireConfig.title }
          clearAll.apply {
            visibility = if (questionnaireConfig.showClearAll) View.VISIBLE else View.GONE
            setOnClickListener {
              // TODO Clear current QuestionnaireResponse items -> SDK
            }
          }
        }

        questionnaire = viewModel.retrieveQuestionnaire(questionnaireConfig, actionParameters)

        try {
          val questionnaireFragmentBuilder = buildQuestionnaireFragment(questionnaire!!)
          supportFragmentManager.commit {
            setReorderingAllowed(true)
            add(R.id.container, questionnaireFragmentBuilder.build(), QUESTIONNAIRE_FRAGMENT_TAG)
          }

          registerFragmentResultListener()
        } catch (nullPointerException: NullPointerException) {
          showToast(getString(R.string.questionnaire_not_found))
          finish()
        } finally {
          viewModel.setProgressState(QuestionnaireProgressState.QuestionnaireLaunch(false))
        }
      }
    }
  }

  private suspend fun buildQuestionnaireFragment(
    questionnaire: Questionnaire,
  ): QuestionnaireFragment.Builder {
    if (questionnaire.subjectType.isNullOrEmpty()) {
      showToast(getString(R.string.missing_subject_type))
      Timber.e(
        "Missing subject type on questionnaire. Provide Questionnaire.subjectType to resolve.",
      )
      finish()
    }
    val questionnaireFragmentBuilder =
      QuestionnaireFragment.builder()
        .setQuestionnaire(questionnaire.json())
        .showAsterisk(questionnaireConfig.showRequiredTextAsterisk)
        .showRequiredText(questionnaireConfig.showRequiredText)

    val questionnaireSubjectType = questionnaire.subjectType.firstOrNull()?.code
    val resourceType =
      questionnaireConfig.resourceType ?: questionnaireSubjectType?.let { ResourceType.valueOf(it) }
    val resourceIdentifier = questionnaireConfig.resourceIdentifier

    if (resourceType != null && !resourceIdentifier.isNullOrEmpty()) {
      // Add subject and other configured resource to launchContext
      val launchContextResources =
        LinkedList<Resource>().apply {
          viewModel.loadResource(resourceType, resourceIdentifier)?.let { add(it) }
          addAll(
            // Exclude the subject resource its already added
            viewModel.retrievePopulationResources(
              actionParameters.filterNot {
                it.paramType == ActionParameterType.QUESTIONNAIRE_RESPONSE_POPULATION_RESOURCE &&
                  resourceType == it.resourceType &&
                  resourceIdentifier.equals(it.value, ignoreCase = true)
              },
            ),
          )
        }

      if (launchContextResources.isNotEmpty()) {
        questionnaireFragmentBuilder.setQuestionnaireLaunchContextMap(
          launchContextResources.associate {
            Pair(it.resourceType.name.lowercase(), it.encodeResourceToString())
          },
        )
      }

      // Populate questionnaire with latest QuestionnaireResponse
      if (questionnaireConfig.isEditable()) {
        val latestQuestionnaireResponse =
          viewModel.searchLatestQuestionnaireResponse(
            resourceId = resourceIdentifier,
            resourceType = resourceType,
            questionnaireId = questionnaire.logicalId,
          )

        val questionnaireResponse =
          QuestionnaireResponse().apply {
            item = latestQuestionnaireResponse?.item
            // Clearing the text prompts the SDK to re-process the content, which includes HTML
            clearText()
          }

        if (viewModel.validateQuestionnaireResponse(questionnaire, questionnaireResponse, this)) {
          questionnaireFragmentBuilder.setQuestionnaireResponse(questionnaireResponse.json())
        } else {
          showToast(getString(R.string.error_populating_questionnaire))
        }
      }
    }
    return questionnaireFragmentBuilder
  }

  private fun Resource.json(): String = this.encodeResourceToString()

  private fun registerFragmentResultListener() {
    supportFragmentManager.setFragmentResultListener(
      QuestionnaireFragment.SUBMIT_REQUEST_KEY,
      this,
    ) { _, _ ->
      val questionnaireResponse = retrieveQuestionnaireResponse()

      // Close questionnaire if opened in read only mode or if experimental
      if (questionnaireConfig.isReadOnly() || questionnaire?.experimental == true) {
        finish()
      }
      if (questionnaireResponse != null && questionnaire != null) {
        viewModel.run {
          setProgressState(QuestionnaireProgressState.ExtractionInProgress(true))
          handleQuestionnaireSubmission(
            questionnaire = questionnaire!!,
            currentQuestionnaireResponse = questionnaireResponse,
            questionnaireConfig = questionnaireConfig,
            actionParameters = actionParameters,
            context = this@QuestionnaireActivity,
          ) { idTypes, questionnaireResponse ->
            // Dismiss progress indicator dialog, submit result then finish activity
            // TODO Ensure this dialog is dismissed even when an exception is encountered
            setProgressState(QuestionnaireProgressState.ExtractionInProgress(false))
            setResult(
              Activity.RESULT_OK,
              Intent().apply {
                putExtra(QUESTIONNAIRE_RESPONSE, questionnaireResponse as Serializable)
                putExtra(QUESTIONNAIRE_SUBMISSION_EXTRACTED_RESOURCE_IDS, idTypes as Serializable)
                putExtra(QUESTIONNAIRE_CONFIG, questionnaireConfig as Parcelable)
              },
            )
            finish()
          }
        }
      }
    }
  }

  private fun handleBackPress() {
    if (questionnaireConfig.isReadOnly()) {
      finish()
    } else if (questionnaireConfig.saveDraft) {
      AlertDialogue.showCancelAlert(
        context = this,
        message =
          org.smartregister.fhircore.engine.R.string
            .questionnaire_in_progress_alert_back_pressed_message,
        title = org.smartregister.fhircore.engine.R.string.questionnaire_alert_back_pressed_title,
        confirmButtonListener = {
          retrieveQuestionnaireResponse()?.let { questionnaireResponse ->
            viewModel.saveDraftQuestionnaire(questionnaireResponse)
          }
        },
        confirmButtonText =
          org.smartregister.fhircore.engine.R.string
            .questionnaire_alert_back_pressed_save_draft_button_title,
        neutralButtonListener = { finish() },
        neutralButtonText =
          org.smartregister.fhircore.engine.R.string.questionnaire_alert_back_pressed_button_title,
      )
    } else {
      AlertDialogue.showConfirmAlert(
        context = this,
        message =
          org.smartregister.fhircore.engine.R.string.questionnaire_alert_back_pressed_message,
        title = org.smartregister.fhircore.engine.R.string.questionnaire_alert_back_pressed_title,
        confirmButtonListener = { finish() },
        confirmButtonText =
          org.smartregister.fhircore.engine.R.string.questionnaire_alert_back_pressed_button_title,
      )
    }
  }

  private fun retrieveQuestionnaireResponse(): QuestionnaireResponse? =
    (supportFragmentManager.findFragmentByTag(QUESTIONNAIRE_FRAGMENT_TAG) as QuestionnaireFragment?)
      ?.getQuestionnaireResponse()

  companion object {

    const val QUESTIONNAIRE_FRAGMENT_TAG = "questionnaireFragment"
    const val QUESTIONNAIRE_CONFIG = "questionnaireConfig"
    const val QUESTIONNAIRE_SUBMISSION_EXTRACTED_RESOURCE_IDS = "questionnaireExtractedResourceIds"
    const val QUESTIONNAIRE_RESPONSE = "questionnaireResponse"
    const val QUESTIONNAIRE_ACTION_PARAMETERS = "questionnaireActionParameters"
    const val QUESTIONNAIRE_POPULATION_RESOURCES = "questionnairePopulationResources"

    fun intentBundle(
      questionnaireConfig: QuestionnaireConfig,
      actionParams: List<ActionParameter>,
    ): Bundle =
      bundleOf(
        Pair(QUESTIONNAIRE_CONFIG, questionnaireConfig),
        Pair(QUESTIONNAIRE_ACTION_PARAMETERS, actionParams),
      )
  }
}
