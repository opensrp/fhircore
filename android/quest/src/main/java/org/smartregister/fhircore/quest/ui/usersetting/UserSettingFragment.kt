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

package org.smartregister.fhircore.quest.ui.usersetting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.google.android.fhir.sync.CurrentSyncJobStatus
import com.google.android.fhir.sync.SyncJobStatus
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.BuildConfig
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.domain.model.SnackBarMessageConfig
import org.smartregister.fhircore.engine.sync.OnSyncListener
import org.smartregister.fhircore.engine.sync.SyncListenerManager
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.quest.ui.main.AppMainViewModel
import org.smartregister.fhircore.quest.ui.shared.components.SnackBarMessage
import org.smartregister.fhircore.quest.util.extensions.hookSnackBar

@AndroidEntryPoint
class UserSettingFragment : Fragment(), OnSyncListener {
  @Inject lateinit var syncListenerManager: SyncListenerManager

  val userSettingViewModel by viewModels<UserSettingViewModel>()
  private val appMainViewModel by activityViewModels<AppMainViewModel>()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    return ComposeView(requireContext()).apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      setContent {
        val appConfig = appMainViewModel.applicationConfiguration
        val scaffoldState = rememberScaffoldState()

        LaunchedEffect(Unit) {
          userSettingViewModel.snackBarStateFlow.hookSnackBar(
            scaffoldState = scaffoldState,
            resourceData = null,
            navController = findNavController(),
          )
        }

        AppTheme {
          Scaffold(
            scaffoldState = scaffoldState,
            snackbarHost = { snackBarHostState ->
              SnackBarMessage(
                snackBarHostState = snackBarHostState,
                backgroundColorHex = appConfig.snackBarTheme.backgroundColor,
                actionColorHex = appConfig.snackBarTheme.actionTextColor,
                contentColorHex = appConfig.snackBarTheme.messageTextColor,
              )
            },
          ) {
            Box(
              modifier =
                androidx.compose.ui.Modifier.padding(it).testTag(USER_SETTING_SCREEN_BOX_TAG),
            ) {
              UserSettingScreen(
                appTitle = appMainViewModel.appMainUiState.value.appTitle,
                username = userSettingViewModel.retrieveUsername(),
                practitionerLocation = userSettingViewModel.practitionerLocation(),
                fullname = userSettingViewModel.retrieveUserInfo()?.name,
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
                lastSyncTime = userSettingViewModel.retrieveLastSyncTimestamp(),
                showProgressIndicatorFlow = userSettingViewModel.showProgressIndicatorFlow,
              )
            }
          }
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    syncListenerManager.registerSyncListener(this, lifecycle)
  }

  override fun onSync(syncJobStatus: CurrentSyncJobStatus) {
    when (syncJobStatus) {
      is CurrentSyncJobStatus.Running ->
        if (syncJobStatus.inProgressSyncJob is SyncJobStatus.Started) {
          lifecycleScope.launch {
            userSettingViewModel.emitSnackBarState(
              SnackBarMessageConfig(message = getString(R.string.syncing)),
            )
          }
        }
      is CurrentSyncJobStatus.Succeeded -> {
        lifecycleScope.launch {
          userSettingViewModel.emitSnackBarState(
            SnackBarMessageConfig(
              message = getString(R.string.sync_completed),
              actionLabel = getString(R.string.ok).uppercase(),
              duration = SnackbarDuration.Long,
            ),
          )
        }
      }
      is CurrentSyncJobStatus.Failed -> {
        lifecycleScope.launch {
          userSettingViewModel.emitSnackBarState(
            SnackBarMessageConfig(
              message =
                getString(
                  R.string.sync_completed_with_errors,
                ),
              duration = SnackbarDuration.Long,
              actionLabel = getString(R.string.ok).uppercase(),
            ),
          )
        }
      }
      else -> {
        // Do nothing
      }
    }
  }

  companion object {
    const val USER_SETTING_SCREEN_BOX_TAG = "fragmentUserSettingScreenTestTag"
  }
}
