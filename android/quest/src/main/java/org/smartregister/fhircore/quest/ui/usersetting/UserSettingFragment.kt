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

package org.smartregister.fhircore.quest.ui.usersetting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import dagger.hilt.android.AndroidEntryPoint
import org.smartregister.fhircore.engine.BuildConfig
import org.smartregister.fhircore.engine.ui.theme.AppTheme

@AndroidEntryPoint
class UserSettingFragment : Fragment() {

  val userSettingViewModel by viewModels<UserSettingViewModel>()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    return ComposeView(requireContext()).apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      setContent {
        AppTheme {
          UserSettingScreen(
            username = userSettingViewModel.retrieveUsername(),
            allowSwitchingLanguages = userSettingViewModel.allowSwitchingLanguages(),
            selectedLanguage = userSettingViewModel.loadSelectedLanguage(),
            allowP2PSync = userSettingViewModel.enabledDeviceToDeviceSync(),
            languages = userSettingViewModel.languages,
            onEvent = userSettingViewModel::onEvent,
            showDatabaseResetConfirmation =
              userSettingViewModel.showDBResetConfirmationDialog.observeAsState(false).value,
            progressBarState =
              userSettingViewModel.progressBarState.observeAsState(Pair(false, 0)).value,
            isDebugVariant = BuildConfig.DEBUG,
            mainNavController = findNavController(),
            lastSyncTime = userSettingViewModel.retrieveLastSyncTimestamp()
          )
        }
      }
    }
  }
}
