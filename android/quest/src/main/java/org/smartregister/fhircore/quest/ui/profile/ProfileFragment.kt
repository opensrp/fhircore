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

package org.smartregister.fhircore.quest.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.quest.event.AppEvent
import org.smartregister.fhircore.quest.event.EventBus
import org.smartregister.fhircore.quest.navigation.MainNavigationScreen
import org.smartregister.fhircore.quest.ui.main.AppMainViewModel
import org.smartregister.fhircore.quest.ui.shared.models.QuestionnaireSubmission

@AndroidEntryPoint
class ProfileFragment : Fragment() {

  @Inject lateinit var eventBus: EventBus
  @Inject lateinit var configurationRegistry: ConfigurationRegistry
  val profileFragmentArgs by navArgs<ProfileFragmentArgs>()
  val profileViewModel by viewModels<ProfileViewModel>()
  val appMainViewModel by activityViewModels<AppMainViewModel>()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {

    with(profileFragmentArgs) {
      lifecycleScope.launch {
        profileViewModel.run {
          decodeBinaryResourceIconsToBitmap(profileId)
          retrieveProfileUiState(profileId, resourceId, resourceConfig, params)
        }
      }
    }

    profileViewModel.refreshProfileDataLiveData.observe(viewLifecycleOwner) {
      viewLifecycleOwner.lifecycleScope.launch {
        if (it == true) {
          with(profileFragmentArgs) {
            profileViewModel.retrieveProfileUiState(profileId, resourceId, resourceConfig, params)
          }
          profileViewModel.refreshProfileDataLiveData.value = null
        }
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
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
        eventBus
          .events
          .getFor(MainNavigationScreen.Profile.route.toString())
          .onEach { appEvent ->
            when (appEvent) {
              is AppEvent.OnSubmitQuestionnaire ->
                handleQuestionnaireSubmission(appEvent.questionnaireSubmission)
              else -> {}
            }
          }
          .launchIn(lifecycleScope)
      }
    }
  }

  @VisibleForTesting
  suspend fun handleQuestionnaireSubmission(questionnaireSubmission: QuestionnaireSubmission) {
    appMainViewModel.onQuestionnaireSubmission(questionnaireSubmission)

    // Always refresh data when questionnaire is submitted
    with(profileFragmentArgs) {
      profileViewModel.retrieveProfileUiState(profileId, resourceId, resourceConfig, params)
    }

    // Display SnackBar message
    val (questionnaireConfig, _) = questionnaireSubmission
    questionnaireConfig.snackBarMessage?.let { snackBarMessageConfig ->
      profileViewModel.emitSnackBarState(snackBarMessageConfig)
    }
  }
}
