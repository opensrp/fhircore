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
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import com.google.android.fhir.sync.CurrentSyncJobStatus
import com.google.android.fhir.sync.SyncJobStatus
import com.google.android.fhir.sync.SyncOperation
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.smartregister.fhircore.engine.BuildConfig
import org.smartregister.fhircore.engine.configuration.app.SettingsOptions
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
                selectedLanguage = userSettingViewModel.loadSelectedLanguage(),
                languages = userSettingViewModel.languages,
                onEvent = userSettingViewModel::onEvent,
                progressBarState =
                  userSettingViewModel.progressBarState.observeAsState(Pair(false, 0)).value,
                isDebugVariant = BuildConfig.DEBUG,
                mainNavController = findNavController(),
                lastSyncTime = appMainViewModel.getSyncTime(),
                showProgressIndicatorFlow = userSettingViewModel.showProgressIndicatorFlow,
                dataMigrationVersion = userSettingViewModel.retrieveDataMigrationVersion(),
                enableManualSync =
                  userSettingViewModel.enableMenuOption(SettingsOptions.MANUAL_SYNC),
                allowSwitchingLanguages = userSettingViewModel.allowSwitchingLanguages(),
                showDatabaseResetConfirmation =
                  userSettingViewModel.enableMenuOption(SettingsOptions.RESET_DATA) &&
                    userSettingViewModel.showDBResetConfirmationDialog.observeAsState(false).value,
                enableAppInsights = userSettingViewModel.enableMenuOption(SettingsOptions.INSIGHTS),
                showOfflineMaps =
                  userSettingViewModel.enableMenuOption(SettingsOptions.OFFLINE_MAPS),
                allowP2PSync = userSettingViewModel.enabledDeviceToDeviceSync(),
                enableHelpContacts =
                  userSettingViewModel.enableMenuOption(SettingsOptions.CONTACT_HELP),
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
    if (syncJobStatus is CurrentSyncJobStatus.Running) {
      if (syncJobStatus.inProgressSyncJob is SyncJobStatus.InProgress) {
        val inProgressSyncJob = syncJobStatus.inProgressSyncJob as SyncJobStatus.InProgress
        val isSyncUpload = inProgressSyncJob.syncOperation == SyncOperation.UPLOAD
        val progressPercentage = appMainViewModel.calculatePercentageProgress(inProgressSyncJob)
        appMainViewModel.updateAppDrawerUIState(
          isSyncUpload = isSyncUpload,
          currentSyncJobStatus = syncJobStatus,
          percentageProgress = progressPercentage,
        )
      }
    } else {
      appMainViewModel.updateAppDrawerUIState(currentSyncJobStatus = syncJobStatus)
    }
  }

  companion object {
    const val USER_SETTING_SCREEN_BOX_TAG = "fragmentUserSettingScreenTestTag"
  }
}
