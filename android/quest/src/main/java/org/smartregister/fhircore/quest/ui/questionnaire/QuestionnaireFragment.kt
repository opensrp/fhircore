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

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.fhir.datacapture.QuestionnaireFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.databinding.QuestionnaireFragmentLayoutBinding
import org.smartregister.fhircore.engine.ui.base.AlertDialogue
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_FRAGMENT_TAG

typealias SdcQuestionnaireFragment = QuestionnaireFragment

@AndroidEntryPoint
class QuestionnaireFragment : Fragment() {

  val viewModel by viewModels<QuestionnaireFragmentViewModel>()
  private val questionnaireFragmentArgs by navArgs<QuestionnaireFragmentArgs>()
  private var viewBinding: QuestionnaireFragmentLayoutBinding? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View? {
    inflater.context.setTheme(R.style.AppTheme_Questionnaire)
    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    viewBinding = QuestionnaireFragmentLayoutBinding.inflate(inflater, container, false)
    return viewBinding?.root
  }

  override fun onDestroyView() {
    super.onDestroyView()
    viewBinding = null
  }

  override fun onAttach(context: Context) {
    super.onAttach(context)
    requireActivity()
      .onBackPressedDispatcher
      .addCallback(
        this,
        object : OnBackPressedCallback(true) {
          override fun handleOnBackPressed() {
            handleBackPress()
          }
        },
      )
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    childFragmentManager.setFragmentResultListener(
      QuestionnaireFragment.SUBMIT_REQUEST_KEY,
      viewLifecycleOwner,
    ) { _, _ ->
      val sdcQuestionnaireFragment = retrieveSdcQuestionnaireFragment()
      val questionnaireConfig = questionnaireFragmentArgs.questionnaireConfig
      if(sdcQuestionnaireFragment != null && questionnaireConfig != null) {
        viewModel.handleQuestionnaireSubmission(
          questionnaireResponse = sdcQuestionnaireFragment.getQuestionnaireResponse(),
          questionnaireConfig = questionnaireConfig
        )
      }
    }
    if (savedInstanceState == null) renderQuestionnaire()
  }

  private fun renderQuestionnaire() {
    viewLifecycleOwner.lifecycleScope.launch {
      val questionnaireConfig = questionnaireFragmentArgs.questionnaireConfig
      if (
        childFragmentManager.findFragmentByTag(QUESTIONNAIRE_FRAGMENT_TAG) == null &&
          questionnaireConfig != null
      ) {
        viewBinding?.questionnaireToolbar?.apply {
          title = questionnaireConfig.title
          setNavigationIcon(R.drawable.ic_arrow_back)
          setNavigationOnClickListener { handleBackPress() }
        }

        val questionnaireJson = viewModel.retrieveQuestionnaireJson(questionnaireConfig)
        if (questionnaireJson.isNotEmpty()) {
          childFragmentManager.commit {
            setReorderingAllowed(true)
            add(
              R.id.container,
              QuestionnaireFragment.builder().setQuestionnaire(questionnaireJson).build(),
              QUESTIONNAIRE_FRAGMENT_TAG,
            )
          }
        }
        retrieveSdcQuestionnaireFragment()?.context?.setTheme(R.style.AppTheme_Questionnaire)
      }
    }
  }

  private fun handleBackPress() {
    val questionnaireConfig = questionnaireFragmentArgs.questionnaireConfig
    if (questionnaireConfig != null) {
      if (questionnaireConfig.type.isReadOnly()) {
        findNavController().popBackStack()
      } else if (questionnaireConfig.saveDraft) {
        AlertDialogue.showCancelAlert(
          requireContext(),
          R.string.questionnaire_in_progress_alert_back_pressed_message,
          R.string.questionnaire_alert_back_pressed_title,
          { /**TODO handleSaveDraftQuestionnaire() */},
          R.string.questionnaire_alert_back_pressed_save_draft_button_title,
          { findNavController().popBackStack() },
          R.string.questionnaire_alert_back_pressed_button_title,
        )
      } else {
        AlertDialogue.showConfirmAlert(
          requireActivity(),
          R.string.questionnaire_alert_back_pressed_message,
          R.string.questionnaire_alert_back_pressed_title,
          {  findNavController().popBackStack() },
          R.string.questionnaire_alert_back_pressed_button_title,
        )
      }
    }
  }

  private fun retrieveSdcQuestionnaireFragment(): SdcQuestionnaireFragment? =
    childFragmentManager.findFragmentByTag(QUESTIONNAIRE_FRAGMENT_TAG) as SdcQuestionnaireFragment?
}
