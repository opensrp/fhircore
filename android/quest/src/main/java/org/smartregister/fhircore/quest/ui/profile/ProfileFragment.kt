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

package org.smartregister.fhircore.quest.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.quest.ui.main.AppMainViewModel
import org.smartregister.fhircore.quest.ui.shared.models.QuestionnaireSubmission

@AndroidEntryPoint
class ProfileFragment : Fragment(), Observer<QuestionnaireSubmission?> {

  val profileFragmentArgs by navArgs<ProfileFragmentArgs>()
  val profileViewModel by viewModels<ProfileViewModel>()
  val appMainViewModel by activityViewModels<AppMainViewModel>()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    with(profileFragmentArgs) {
      lifecycleScope.launchWhenCreated {
        profileViewModel.retrieveProfileUiState(profileId, resourceId, resourceConfig)
      }
    }
    return ComposeView(requireContext()).apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      setContent {
        AppTheme {
          ProfileScreen(
            navController = findNavController(),
            profileUiState = profileViewModel.profileUiState.value,
            onEvent = profileViewModel::onEvent,
            snackStateFlow = profileViewModel.snackBarStateFlow
          )
        }
      }
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    appMainViewModel.questionnaireSubmissionLiveData.observe(viewLifecycleOwner, this)
  }

  /**
   * Overridden method for [Observer] class used to address [QuestionnaireSubmission] events. A new
   * [Observer] is needed for every fragment since the [AppMainViewModel]'s
   * questionnaireSubmissionLiveData outlives the Fragment. Cannot use Kotlin Observer { } as it is
   * optimized to a singleton resulting to an exception using an observer from a detached fragment.
   */
  override fun onChanged(questionnaireSubmission: QuestionnaireSubmission?) {
    lifecycleScope.launch {
      questionnaireSubmission?.let {
        appMainViewModel.onQuestionnaireSubmission(questionnaireSubmission)
        // Always refresh data when questionnaire is submitted
        with(profileFragmentArgs) {
          profileViewModel.retrieveProfileUiState(profileId, resourceId, resourceConfig)
        }

        // Display SnackBar message
        val (questionnaireConfig, _) = questionnaireSubmission
        questionnaireConfig.snackBarMessage?.let { snackBarMessageConfig ->
          profileViewModel.emitSnackBarState(snackBarMessageConfig)
        }

        // Reset activity livedata
        appMainViewModel.questionnaireSubmissionLiveData.postValue(null)
      }
    }
  }
}
