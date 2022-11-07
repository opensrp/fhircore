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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.quest.ui.main.AppMainViewModel
import org.smartregister.fhircore.quest.ui.shared.models.QuestionnaireSubmission
import org.smartregister.fhircore.quest.util.extensions.rememberLifecycleEvent

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
    return ComposeView(requireContext()).apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      setContent {
        AppTheme {
          // Retrieve data when Lifecycle state is resuming
          val lifecycleEvent = rememberLifecycleEvent()
          LaunchedEffect(lifecycleEvent) {
            if (lifecycleEvent == Lifecycle.Event.ON_RESUME) {
              with(profileFragmentArgs) {
                profileViewModel.retrieveProfileUiState(profileId, resourceId, resourceConfig)
              }
            }
          }

          ProfileScreen(
            navController = findNavController(),
            profileUiState = profileViewModel.profileUiState.value,
            onEvent = profileViewModel::onEvent
          )
        }
      }
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    appMainViewModel.questionnaireSubmissionLiveData.observe(viewLifecycleOwner, this)
  }

  /**
   * Overridden method for [Observer] class. A new [Observer] is needed for every fragment since the
   * live
   */
  override fun onChanged(questionnaireSubmission: QuestionnaireSubmission?) {
    questionnaireSubmission?.let {
      with(profileFragmentArgs) {
        profileViewModel.completeTask(
          navController = findNavController(),
          questionnaireSubmission = questionnaireSubmission,
          profileId = profileId,
          resourceId = resourceId,
          resourceConfig = resourceConfig
        )

        // Reset state
        appMainViewModel.questionnaireSubmissionLiveData.value = null
      }
    }
  }
}
