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
import androidx.compose.material.SnackbarDuration
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.google.android.fhir.sync.SyncJobStatus
import com.google.android.fhir.sync.SyncOperation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.BuildConfig
import org.smartregister.fhircore.engine.domain.model.SnackBarMessageConfig
import org.smartregister.fhircore.engine.sync.OnSyncListener
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.main.AppMainViewModel
import retrofit2.HttpException
import timber.log.Timber

@AndroidEntryPoint
class UserSettingFragment : Fragment(), OnSyncListener {

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
        AppTheme {
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

  override fun onSync(syncJobStatus: SyncJobStatus) {
    when (syncJobStatus) {
      is SyncJobStatus.Started ->
        lifecycleScope.launch {
          userSettingViewModel.emitSnackBarState(
            SnackBarMessageConfig(message = getString(R.string.syncing)),
          )
        }
      is SyncJobStatus.InProgress ->
        emitPercentageProgress(syncJobStatus, syncJobStatus.syncOperation == SyncOperation.UPLOAD)
      is SyncJobStatus.Finished -> {
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
      is SyncJobStatus.Failed -> {
        val hasAuthError =
          try {
            Timber.e(syncJobStatus.exceptions.joinToString { it.exception.message ?: "" })
            syncJobStatus.exceptions.any {
              it.exception is HttpException && (it.exception as HttpException).code() == 401
            }
          } catch (nullPointerException: NullPointerException) {
            false
          }

        lifecycleScope.launch {
          userSettingViewModel.emitSnackBarState(
            SnackBarMessageConfig(
              message =
              getString(
                if (hasAuthError) {
                  R.string.sync_unauthorised
                } else R.string.sync_completed_with_errors,
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

  fun emitPercentageProgress(
    progressSyncJobStatus: SyncJobStatus.InProgress,
    isUploadSync: Boolean,
  ) {
    lifecycleScope.launch {
      val percentageProgress: Int = calculateActualPercentageProgress(progressSyncJobStatus)
      userSettingViewModel.emitPercentageProgressState(percentageProgress, isUploadSync)
    }
  }

  private fun getSyncProgress(completed: Int, total: Int) =
    completed * 100 / if (total > 0) total else 1

  private fun calculateActualPercentageProgress(
    progressSyncJobStatus: SyncJobStatus.InProgress,
  ): Int {
    val totalRecordsOverall =
      userSettingViewModel.sharedPreferencesHelper.read(
        SharedPreferencesHelper.PREFS_SYNC_PROGRESS_TOTAL +
          progressSyncJobStatus.syncOperation.name,
        1L,
      )
    val isProgressTotalLess = progressSyncJobStatus.total <= totalRecordsOverall
    val currentProgress: Int
    val currentTotalRecords =
      if (isProgressTotalLess) {
        currentProgress =
          totalRecordsOverall.toInt() - progressSyncJobStatus.total +
            progressSyncJobStatus.completed
        totalRecordsOverall.toInt()
      } else {
        userSettingViewModel.sharedPreferencesHelper.write(
          SharedPreferencesHelper.PREFS_SYNC_PROGRESS_TOTAL +
            progressSyncJobStatus.syncOperation.name,
          progressSyncJobStatus.total.toLong(),
        )
        currentProgress = progressSyncJobStatus.completed
        progressSyncJobStatus.total
      }

    return getSyncProgress(currentProgress, currentTotalRecords)
  }
}
