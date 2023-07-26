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
import androidx.core.os.bundleOf
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.google.android.fhir.datacapture.QuestionnaireFragment
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import com.google.android.fhir.logicalId
import dagger.hilt.android.AndroidEntryPoint
import java.util.LinkedList
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.ListResource
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.engine.ui.base.AlertDialogue
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.util.extension.encodeResourceToString
import org.smartregister.fhircore.engine.util.extension.parcelable
import org.smartregister.fhircore.engine.util.extension.parcelableArrayList
import org.smartregister.fhircore.engine.util.extension.showToast
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.databinding.QuestionnaireActivityBinding
import org.smartregister.fhircore.quest.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_FRAGMENT_TAG
import org.smartregister.fhircore.quest.ui.questionnaire.QuestionnaireNewViewModel.Companion.CONTAINED_LIST_TITLE
import timber.log.Timber

@AndroidEntryPoint
class QuestionnaireNewActivity : BaseMultiLanguageActivity() {

  val viewModel by viewModels<QuestionnaireNewViewModel>()
  private lateinit var questionnaireConfig: QuestionnaireConfig
  private lateinit var actionParameters: ArrayList<ActionParameter>
  private lateinit var viewBinding: QuestionnaireActivityBinding
  private var questionnaire: Questionnaire? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setTheme(R.style.AppTheme_Questionnaire)
    viewBinding = QuestionnaireActivityBinding.inflate(layoutInflater)
    setContentView(viewBinding.root)
    with(intent) {
      parcelable<QuestionnaireConfig>(QUESTIONNAIRE_CONFIG)?.also { questionnaireConfig = it }
      actionParameters = parcelableArrayList(QUESTIONNAIRE_ACTION_PARAMETERS) ?: arrayListOf()
    }

    if (!::questionnaireConfig.isInitialized) {
      showToast(getString(R.string.missing_questionnaire_config))
      finish()
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
        viewBinding.questionnaireToolbar.apply {
          title = questionnaireConfig.title
          setNavigationIcon(R.drawable.ic_arrow_back)
          setNavigationOnClickListener { handleBackPress() }
        }

        questionnaire = viewModel.retrieveQuestionnaire(questionnaireConfig, actionParameters)

        if (questionnaire == null) {
          showToast(getString(R.string.questionnaire_not_found))
          finish()
        }

        if (questionnaire?.subjectType.isNullOrEmpty()) {
          showToast(getString(R.string.missing_subject_type))
          Timber.e(
            "Missing subject type on questionnaire. Provide Questionnaire.subjectType to resolve.",
          )
          finish()
        }

        val questionnaireFragmentBuilder = buildQuestionnaireFragment(questionnaire!!)

        supportFragmentManager.commit {
          setReorderingAllowed(true)
          add(R.id.container, questionnaireFragmentBuilder.build(), QUESTIONNAIRE_FRAGMENT_TAG)
        }

        registerFragmentResultListener()
      }
    }
  }

  private suspend fun buildQuestionnaireFragment(
    questionnaire: Questionnaire,
  ): QuestionnaireFragment.Builder {
    val questionnaireFragmentBuilder =
      QuestionnaireFragment.builder().setQuestionnaire(questionnaire.json())

    val questionnaireSubjectType = questionnaire.subjectType.firstOrNull()?.code
    val resourceType =
      questionnaireConfig.resourceType ?: questionnaireSubjectType?.let { ResourceType.valueOf(it) }
    val resourceIdentifier = questionnaireConfig.resourceIdentifier

    val populationResources = LinkedList<Resource>()

    if (resourceType != null && !resourceIdentifier.isNullOrEmpty()) {
      val subjectResource = viewModel.loadResource(resourceType, resourceIdentifier)

      if (subjectResource != null) {
        val launchContexts = listOf(subjectResource.json())
        questionnaireFragmentBuilder.setQuestionnaireLaunchContexts(launchContexts)
        populationResources.add(subjectResource)
      }

      // Get previously extracted resources from the QuestionnaireResponse.contained for population
      val latestQuestionnaireResponse =
        viewModel.searchLatestQuestionnaireResponse(
          resourceId = resourceIdentifier,
          resourceType = resourceType,
          questionnaireId = questionnaire.id,
        )
      latestQuestionnaireResponse?.contained?.forEach { resource ->
        if (
          resource is ListResource && resource.title.equals(CONTAINED_LIST_TITLE, ignoreCase = true)
        ) {
          resource.entry?.forEach { listEntryComponent ->
            if (listEntryComponent.hasItem()) {
              viewModel.loadResource(listEntryComponent.item)?.let { populationResources.add(it) }
            }
          }
        } else {
          viewModel.loadResource(resource.resourceType, resource.logicalId)?.let {
            populationResources.add(it)
          }
        }
      }
    }

    val configuredPopulationResources = viewModel.retrievePopulationResources(actionParameters)

    val questionnaireResponse =
      ResourceMapper.populate(
        questionnaire = questionnaire,
        resources = populationResources.plus(configuredPopulationResources).toTypedArray(),
      )

    val validQuestionnaireResponse =
      viewModel.validateQuestionnaireResponse(questionnaire, questionnaireResponse, this)

    if (validQuestionnaireResponse && !resourceIdentifier.isNullOrEmpty()) {
      questionnaireFragmentBuilder.setQuestionnaireResponse(questionnaireResponse.json())
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
      if (questionnaireConfig.type.isReadOnly() || questionnaire?.experimental == false) {
        finish()
      }
      if (questionnaireResponse != null && questionnaire != null) {
        viewModel.handleQuestionnaireSubmission(
          questionnaire = questionnaire!!,
          currentQuestionnaireResponse = questionnaireResponse,
          questionnaireConfig = questionnaireConfig,
          actionParameters = actionParameters,
          context = this,
        )
      }
    }
  }

  private fun handleBackPress() {
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

  private fun retrieveQuestionnaireResponse(): QuestionnaireResponse? =
    (supportFragmentManager.findFragmentByTag(QUESTIONNAIRE_FRAGMENT_TAG) as QuestionnaireFragment?)
      ?.getQuestionnaireResponse()

  companion object {

    const val QUESTIONNAIRE_CONFIG = "questionnaireConfig"
    const val QUESTIONNAIRE_ACTION_PARAMETERS = "questionnaireActionParameters"
    const val QUESTIONNAIRE_POPULATION_RESOURCES = "questionnairePopulationResources"

    fun intentArgs(
      questionnaireConfig: QuestionnaireConfig,
      actionParams: List<ActionParameter>,
    ): Bundle =
      bundleOf(
        Pair(QUESTIONNAIRE_CONFIG, questionnaireConfig),
        Pair(QUESTIONNAIRE_ACTION_PARAMETERS, actionParams),
      )
  }
}
